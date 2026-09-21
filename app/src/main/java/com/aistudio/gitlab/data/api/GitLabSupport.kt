package com.aistudio.gitlab.data.api

import com.aistudio.gitlab.data.model.GitLabJob
import com.aistudio.gitlab.data.model.GitLabStage
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

class GitLabApiException(val code: Int, message: String) : Exception(message)

fun Throwable.isAuthError(): Boolean = this is GitLabApiException && (code == 401 || code == 403)

fun normalizeBaseUrl(raw: String): String {
  val trimmed = raw.trim()
  if (trimmed.isEmpty() || trimmed.equals("http://", true) || trimmed.equals("https://", true)) {
    throw IllegalArgumentException("Enter a GitLab server URL")
  }
  var base = trimmed.trimEnd('/')
  if (base.equals("http:", true) || base.equals("https:", true) || base.isEmpty()) {
    throw IllegalArgumentException("Enter a GitLab server URL")
  }
  if (!base.startsWith("http://") && !base.startsWith("https://")) {
    base = "https://$base"
  }
  val host = base.substringAfter("://").substringBefore('/').substringBefore(':')
  if (host.isBlank()) throw IllegalArgumentException("Enter a GitLab server URL")
  return "$base/"
}

fun encodeGitLabPath(path: String): String =
  path.trim('/').split('/')
    .filter { it.isNotEmpty() }
    .joinToString("%2F") { segment ->
      URLEncoder.encode(segment, Charsets.UTF_8.name()).replace("+", "%20")
    }

fun formatGitLabTime(iso: String, nowMillis: Long = System.currentTimeMillis()): String {
  if (iso.isBlank()) return ""
  val parsed = parseGitLabMillis(iso) ?: return iso.take(10)
  val deltaSec = ((nowMillis - parsed) / 1000).coerceAtLeast(0)
  return when {
    deltaSec < 60 -> "just now"
    deltaSec < 3600 -> "${deltaSec / 60}m ago"
    deltaSec < 86_400 -> "${deltaSec / 3600}h ago"
    deltaSec < 86_400 * 7 -> "${deltaSec / 86_400}d ago"
    else -> iso.take(10)
  }
}

fun parseGitLabMillis(iso: String): Long? {
  val patterns = arrayOf(
    "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
    "yyyy-MM-dd'T'HH:mm:ssXXX",
    "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    "yyyy-MM-dd'T'HH:mm:ss'Z'",
    "yyyy-MM-dd'T'HH:mm:ss.SSSXX",
    "yyyy-MM-dd'T'HH:mm:ssXX"
  )
  for (pattern in patterns) {
    try {
      val formatter = SimpleDateFormat(pattern, Locale.US)
      formatter.timeZone = TimeZone.getTimeZone("UTC")
      formatter.isLenient = false
      val date = formatter.parse(iso) ?: continue
      return date.time
    } catch (_: Exception) {
      // Try the next GitLab timestamp shape.
    }
  }
  return null
}

fun parseGitLabError(raw: String?, code: Int): String {
  val fallback = when (code) {
    401 -> "Authentication failed. Check the personal access token."
    403 -> "You don't have permission for this request."
    404 -> "Not found"
    0 -> "Network error"
    else -> "HTTP $code"
  }
  if (raw.isNullOrBlank()) return fallback
  val stringMessage = extractJsonString(raw, "message") ?: extractJsonString(raw, "error")
  if (!stringMessage.isNullOrBlank()) return stringMessage
  val objectMessage = extractJsonValue(raw, "message")
  if (!objectMessage.isNullOrBlank() && !objectMessage.trimStart().startsWith("\"")) {
    return objectMessage.trim().trim('{', '}').trim().ifBlank { fallback }
  }
  val compact = raw.trim()
  return if (compact.startsWith("{") || compact.startsWith("[")) fallback else compact.take(180)
}

fun stagesFromJobs(jobs: List<GitLabJob>): List<GitLabStage> {
  val grouped = LinkedHashMap<String, MutableList<GitLabJob>>()
  jobs.sortedBy { it.id }.forEach { job ->
    val stage = job.stage.ifBlank { "unknown" }
    grouped.getOrPut(stage) { mutableListOf() }.add(job)
  }
  return grouped.map { (name, stageJobs) ->
    GitLabStage(name = name, status = stageStatus(stageJobs), jobs = stageJobs)
  }
}

private fun stageStatus(jobs: List<GitLabJob>): String {
  val statuses = jobs.map { it.status }
  return when {
    statuses.any { it == "failed" } -> "failed"
    statuses.any { it == "running" } -> "running"
    statuses.any { it == "pending" || it == "created" || it == "preparing" || it == "scheduled" || it == "waiting_for_resource" } -> "pending"
    statuses.any { it == "manual" } && statuses.none { it == "success" } -> "manual"
    statuses.any { it == "success" } -> "success"
    statuses.all { it == "skipped" } -> "skipped"
    statuses.any { it == "canceled" || it == "canceling" } -> "canceled"
    else -> statuses.firstOrNull().orEmpty().ifBlank { "unknown" }
  }
}

private fun extractJsonString(json: String, field: String): String? {
  val value = extractJsonValue(json, field) ?: return null
  val trimmed = value.trim()
  if (trimmed.length < 2 || trimmed.first() != '"' || trimmed.last() != '"') return null
  return trimmed.substring(1, trimmed.length - 1)
    .replace("\\\"", "\"")
    .replace("\\n", "\n")
}

private fun extractJsonValue(json: String, field: String): String? {
  val key = "\"$field\""
  val keyAt = json.indexOf(key)
  if (keyAt < 0) return null
  var index = keyAt + key.length
  while (index < json.length && json[index].isWhitespace()) index++
  if (index >= json.length || json[index] != ':') return null
  index++
  while (index < json.length && json[index].isWhitespace()) index++
  if (index >= json.length) return null
  return when (json[index]) {
    '"' -> readJsonString(json, index)
    '{' -> readBalanced(json, index, '{', '}')
    '[' -> readBalanced(json, index, '[', ']')
    else -> {
      val end = json.indexOfAny(charArrayOf(',', '}', ']'), index).let { if (it < 0) json.length else it }
      json.substring(index, end).trim()
    }
  }
}

private fun readJsonString(json: String, start: Int): String {
  var index = start + 1
  val out = StringBuilder().append('"')
  while (index < json.length) {
    val char = json[index]
    out.append(char)
    if (char == '\\' && index + 1 < json.length) {
      out.append(json[index + 1])
      index += 2
      continue
    }
    if (char == '"') break
    index++
  }
  return out.toString()
}

private fun readBalanced(json: String, start: Int, open: Char, close: Char): String {
  var depth = 0
  var inString = false
  var escaped = false
  for (index in start until json.length) {
    val char = json[index]
    if (inString) {
      if (escaped) escaped = false
      else if (char == '\\') escaped = true
      else if (char == '"') inString = false
      continue
    }
    when (char) {
      '"' -> inString = true
      open -> depth++
      close -> {
        depth--
        if (depth == 0) return json.substring(start, index + 1)
      }
    }
  }
  return json.substring(start)
}
