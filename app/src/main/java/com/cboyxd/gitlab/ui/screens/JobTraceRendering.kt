package com.cboyxd.gitlab.ui.screens

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

internal val JobConsoleBackground = Color(0xFF18171D)
internal val JobConsoleForeground = Color(0xFFCCCCCC)
internal val JobConsoleMuted = Color(0xFF89888D)

internal data class JobTraceLine(
  val number: Int,
  val text: AnnotatedString,
  val timestamp: String?,
  val sectionKey: String? = null,
  val parents: List<String> = emptyList(),
  val initiallyCollapsed: Boolean = false,
)

private val tracePrefix = Regex("""^(\d{4}-\d{2}-\d{2}T\S+)\s+\d{2}[OE]\+? ?""")
private val sectionStart = Regex("""section_start:(\d+):([A-Za-z0-9_.\-]+)(\[[^\]]*])?\r?(\u001B\[0K)?""")
private val sectionEnd = Regex("""section_end:\d+:([A-Za-z0-9_.\-]+)\r?(\u001B\[0K)?""")
private val ansiEscape = Regex("""\u001B\[([0-9;:]*)([ -/]*)([@-~])""")
private val terminalLink = Regex("""\u001B\][^\u0007\u001B]*(?:\u0007|\u001B\\)""")

/** GitLab terminal palette: https://gitlab.com/gitlab-org/gitlab/-/raw/master/app/assets/stylesheets/page_bundles/xterm.scss */
private val terminalPalette = listOf(
  Color(0xFF000000), Color(0xFFDF6D6D), Color(0xFF4BB349), Color(0xFFB2B215),
  Color(0xFF4193E4), Color(0xFFF330F3), Color(0xFF1AA2A2), Color(0xFFCCCCCC),
  Color(0xFF918E95), Color(0xFFFF6161), Color(0xFF5CF759), Color(0xFFCDCD1A),
  Color(0xFF5797FF), Color(0xFFEA4FEA), Color(0xFF00BDBD), Color(0xFFFFFFFF),
)

private fun terminalColor(index: Int): Color? = when (index) {
  in 0..15 -> terminalPalette[index]
  in 16..231 -> {
    val n = index - 16
    val levels = intArrayOf(0, 95, 135, 175, 215, 255)
    Color(levels[n / 36], levels[(n / 6) % 6], levels[n % 6])
  }
  in 232..255 -> (8 + (index - 232) * 10).let { Color(it, it, it) }
  else -> null
}

internal fun parseJobTrace(raw: String): List<JobTraceLine> {
  val renderer = TerminalStyle()
  val result = ArrayList<JobTraceLine>()
  val sections = mutableListOf<Pair<String, String>>()
  raw.lineSequence().forEachIndexed { index, rawLine ->
    val prefix = tracePrefix.find(rawLine)
    var line = if (prefix != null) rawLine.removeRange(prefix.range) else rawLine
    val timestamp = prefix?.groupValues?.get(1)?.substringAfter('T')?.removeSuffix("Z")?.take(12)
    val start = sectionStart.find(line)
    val end = sectionEnd.find(line)
    if (end != null) {
      val position = sections.indexOfLast { it.second == end.groupValues[1] }
      if (position >= 0) while (sections.size > position) sections.removeAt(sections.lastIndex)
      line = line.removeRange(end.range)
    }
    if (start != null) line = sectionStart.replace(line, "")
    line = line.trimEnd('\r')
    // Progress output redraws the current row, rather than introducing visible escape codes.
    if ('\r' in line) {
      val parts = line.split('\r')
      parts.dropLast(1).forEach { renderer.render(it) }
      line = parts.last()
    }
    var text = renderer.render(line)
    if (start != null && text.isBlank()) {
      text = AnnotatedString(start.groupValues[2].removePrefix("step_").replace('_', ' ').replaceFirstChar { it.uppercase() })
    }
    if (end != null && text.isBlank() && start == null) return@forEachIndexed
    val key = start?.let { "${it.groupValues[1]}:${it.groupValues[2]}" }
    result += JobTraceLine(index + 1, text, timestamp, key, sections.map { it.first }, start?.groupValues?.get(3)?.contains("collapsed=true") == true)
    if (start != null && key != null) sections += key to start.groupValues[2]
  }
  while (result.lastOrNull()?.text?.isEmpty() == true) result.removeAt(result.lastIndex)
  return result
}

/** Formatting survives newlines, as it does in a terminal. Extended colors consume their operands. */
private class TerminalStyle {
  private var foreground: Color? = null
  private var basicForeground: Int? = null
  private var background: Color? = null
  private var bold = false
  private var dim = false
  private var italic = false
  private var underline = false
  private var strike = false
  private var inverse = false
  private var concealed = false

  fun render(raw: String): AnnotatedString = buildAnnotatedString {
    val line = terminalLink.replace(raw, "")
    var offset = 0
    fun chunk(value: String) {
      if (value.isEmpty()) return
      var fg = basicForeground?.let { terminalPalette[it + if (bold && it < 8) 8 else 0] } ?: foreground ?: JobConsoleForeground
      var bg = background ?: Color.Unspecified
      if (inverse) {
        val previous = fg
        fg = background ?: JobConsoleBackground
        bg = previous
      }
      if (dim) fg = fg.copy(alpha = 0.6f)
      if (concealed) fg = if (bg == Color.Unspecified) JobConsoleBackground else bg
      val decorations = buildList {
        if (underline) add(TextDecoration.Underline)
        if (strike) add(TextDecoration.LineThrough)
      }
      withStyle(SpanStyle(
        color = fg, background = bg,
        fontWeight = if (bold) FontWeight.Bold else null,
        fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
        textDecoration = TextDecoration.combine(decorations),
      )) { append(value.replace("\u001B", "")) }
    }
    ansiEscape.findAll(line).forEach { match ->
      chunk(line.substring(offset, match.range.first))
      offset = match.range.last + 1
      if (match.groupValues[3] != "m") return@forEach
      val codes = match.groupValues[1].replace("38:2::", "38;2;").replace("48:2::", "48;2;").split(';', ':').map { it.toIntOrNull() ?: 0 }
      var i = 0
      while (i < codes.size) {
        when (val code = codes[i]) {
          0 -> { foreground = null; basicForeground = null; background = null; bold = false; dim = false; italic = false; underline = false; strike = false; inverse = false; concealed = false }
          1 -> bold = true
          2 -> dim = true
          3 -> italic = true
          4 -> underline = true
          7 -> inverse = true
          8 -> concealed = true
          9 -> strike = true
          22 -> { bold = false; dim = false }
          23 -> italic = false
          24 -> underline = false
          27 -> inverse = false
          28 -> concealed = false
          29 -> strike = false
          in 30..37 -> { basicForeground = code - 30; foreground = null }
          in 90..97 -> { basicForeground = code - 90 + 8; foreground = null }
          in 40..47 -> background = terminalPalette[code - 40]
          in 100..107 -> background = terminalPalette[code - 100 + 8]
          39 -> { foreground = null; basicForeground = null }
          49 -> background = null
          38, 48 -> {
            var color: Color? = null
            if (codes.getOrNull(i + 1) == 5 && i + 2 < codes.size) {
              color = terminalColor(codes[i + 2]); i += 2
            } else if (codes.getOrNull(i + 1) == 2 && i + 4 < codes.size) {
              color = Color(codes[i + 2].coerceIn(0, 255), codes[i + 3].coerceIn(0, 255), codes[i + 4].coerceIn(0, 255)); i += 4
            }
            if (color != null) {
              if (code == 38) { foreground = color; basicForeground = null } else background = color
            }
          }
        }
        i++
      }
    }
    chunk(line.substring(offset))
  }
}
