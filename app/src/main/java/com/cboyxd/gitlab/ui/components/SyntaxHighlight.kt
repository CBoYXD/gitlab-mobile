package com.cboyxd.gitlab.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.cboyxd.gitlab.ui.theme.Gl
import com.cboyxd.gitlab.ui.theme.GlColors
import dev.snipme.highlights.Highlights
import dev.snipme.highlights.model.BoldHighlight
import dev.snipme.highlights.model.ColorHighlight
import dev.snipme.highlights.model.SyntaxLanguage
import dev.snipme.highlights.model.SyntaxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Files larger than this are shown plain: highlighting cost grows with size and they rarely matter on a phone. */
private const val MAX_HIGHLIGHT_CHARS = 200_000

private val EXTENSIONS = mapOf(
  "kt" to SyntaxLanguage.KOTLIN, "kts" to SyntaxLanguage.KOTLIN,
  "java" to SyntaxLanguage.JAVA, "gradle" to SyntaxLanguage.KOTLIN,
  "js" to SyntaxLanguage.JAVASCRIPT, "mjs" to SyntaxLanguage.JAVASCRIPT, "cjs" to SyntaxLanguage.JAVASCRIPT,
  "jsx" to SyntaxLanguage.JAVASCRIPT, "vue" to SyntaxLanguage.JAVASCRIPT,
  "ts" to SyntaxLanguage.TYPESCRIPT, "tsx" to SyntaxLanguage.TYPESCRIPT,
  "py" to SyntaxLanguage.PYTHON, "pyi" to SyntaxLanguage.PYTHON,
  "rb" to SyntaxLanguage.RUBY, "rake" to SyntaxLanguage.RUBY, "gemspec" to SyntaxLanguage.RUBY, "erb" to SyntaxLanguage.RUBY, "haml" to SyntaxLanguage.RUBY,
  "go" to SyntaxLanguage.GO, "rs" to SyntaxLanguage.RUST, "swift" to SyntaxLanguage.SWIFT,
  "c" to SyntaxLanguage.C, "h" to SyntaxLanguage.C,
  "cc" to SyntaxLanguage.CPP, "cpp" to SyntaxLanguage.CPP, "cxx" to SyntaxLanguage.CPP, "hpp" to SyntaxLanguage.CPP, "hh" to SyntaxLanguage.CPP,
  "cs" to SyntaxLanguage.CSHARP, "dart" to SyntaxLanguage.DART, "php" to SyntaxLanguage.PHP,
  "pl" to SyntaxLanguage.PERL, "pm" to SyntaxLanguage.PERL, "coffee" to SyntaxLanguage.COFFEESCRIPT,
  "sh" to SyntaxLanguage.SHELL, "bash" to SyntaxLanguage.SHELL, "zsh" to SyntaxLanguage.SHELL, "fish" to SyntaxLanguage.SHELL,
)

private val FILE_NAMES = mapOf(
  "Gemfile" to SyntaxLanguage.RUBY, "Rakefile" to SyntaxLanguage.RUBY, "Podfile" to SyntaxLanguage.RUBY,
  "Dockerfile" to SyntaxLanguage.SHELL, "Makefile" to SyntaxLanguage.SHELL,
)

private val FENCE_NAMES = mapOf(
  "kotlin" to SyntaxLanguage.KOTLIN, "java" to SyntaxLanguage.JAVA, "javascript" to SyntaxLanguage.JAVASCRIPT,
  "typescript" to SyntaxLanguage.TYPESCRIPT, "python" to SyntaxLanguage.PYTHON, "ruby" to SyntaxLanguage.RUBY,
  "golang" to SyntaxLanguage.GO, "rust" to SyntaxLanguage.RUST, "shell" to SyntaxLanguage.SHELL,
  "console" to SyntaxLanguage.SHELL, "csharp" to SyntaxLanguage.CSHARP, "c++" to SyntaxLanguage.CPP,
)

/** Language for a repository path, or DEFAULT (strings, numbers and comments only). */
fun languageForPath(path: String): SyntaxLanguage {
  val name = path.substringAfterLast('/')
  FILE_NAMES[name]?.let { return it }
  return EXTENSIONS[name.substringAfterLast('.', "").lowercase()] ?: SyntaxLanguage.DEFAULT
}

/** Language for a fenced code block's info string (```kotlin). */
fun languageForFence(info: String?): SyntaxLanguage {
  val key = info?.trim()?.substringBefore(' ')?.lowercase().orEmpty()
  if (key.isEmpty()) return SyntaxLanguage.DEFAULT
  return FENCE_NAMES[key] ?: EXTENSIONS[key] ?: SyntaxLanguage.DEFAULT
}

private fun GlColors.syntaxTheme() = SyntaxTheme(
  key = if (isDark) "gl-dark" else "gl-light",
  code = textPrimary.toArgb() and 0xFFFFFF,
  keyword = syntaxKeyword.toArgb() and 0xFFFFFF,
  string = syntaxString.toArgb() and 0xFFFFFF,
  literal = syntaxLiteral.toArgb() and 0xFFFFFF,
  comment = syntaxComment.toArgb() and 0xFFFFFF,
  metadata = syntaxMeta.toArgb() and 0xFFFFFF,
  multilineComment = syntaxComment.toArgb() and 0xFFFFFF,
  punctuation = textPrimary.toArgb() and 0xFFFFFF,
  mark = syntaxKeyword.toArgb() and 0xFFFFFF,
)

/**
 * Languages whose `#` comments collide with `#` inside strings (Ruby "#{x}", shell "${#a}").
 * The highlighter can misread one of those and colour the rest of the file as a string, so
 * these are highlighted line by line: a misread then stays inside a single line.
 */
private val LINE_BY_LINE = setOf(
  SyntaxLanguage.RUBY, SyntaxLanguage.PYTHON, SyntaxLanguage.SHELL, SyntaxLanguage.PERL, SyntaxLanguage.COFFEESCRIPT,
)

/** Highlights [code] as one AnnotatedString. Blocking; call off the main thread. */
fun highlight(code: String, language: SyntaxLanguage, colors: GlColors): AnnotatedString {
  if (code.length > MAX_HIGHLIGHT_CHARS) return AnnotatedString(code)
  if (language in LINE_BY_LINE && '\n' in code) {
    return buildAnnotatedString {
      code.split('\n').forEachIndexed { index, line ->
        if (index > 0) append('\n')
        append(highlightSpans(line, language, colors))
      }
    }
  }
  return highlightSpans(code, language, colors)
}

private fun highlightSpans(code: String, language: SyntaxLanguage, colors: GlColors): AnnotatedString {
  if (code.isBlank()) return AnnotatedString(code)
  val spans = runCatching {
    Highlights.Builder().code(code).language(language).theme(colors.syntaxTheme()).build().getHighlights()
  }.getOrElse { return AnnotatedString(code) }
  return buildAnnotatedString {
    append(code)
    spans.forEach { span ->
      val start = span.location.start.coerceIn(0, code.length)
      val end = span.location.end.coerceIn(start, code.length)
      if (start == end) return@forEach
      when (span) {
        is ColorHighlight -> addStyle(SpanStyle(color = Color(0xFF000000.toInt() or span.rgb)), start, end)
        is BoldHighlight -> addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), start, end)
      }
    }
  }
}

/** Splits a highlighted document into lines, keeping each line's spans. */
fun AnnotatedString.splitLines(): List<AnnotatedString> {
  val result = ArrayList<AnnotatedString>()
  var start = 0
  while (true) {
    val end = text.indexOf('\n', start)
    if (end < 0) {
      result += subSequence(start, text.length)
      return result
    }
    result += subSequence(start, end)
    start = end + 1
  }
}

/**
 * Plain lines first, highlighted lines once the background pass finishes. Large files therefore
 * appear immediately and never block the UI thread.
 */
@Composable
fun rememberHighlightedLines(code: String, language: SyntaxLanguage): List<AnnotatedString> {
  val colors = Gl.colors
  val plain = remember(code) { code.lines().map { AnnotatedString(it) } }
  var highlighted by remember(code, language, colors) { mutableStateOf<List<AnnotatedString>?>(null) }
  LaunchedEffect(code, language, colors) {
    highlighted = withContext(Dispatchers.Default) { highlight(code, language, colors).splitLines() }
  }
  return highlighted ?: plain
}

@Composable
fun rememberHighlighted(code: String, language: SyntaxLanguage): AnnotatedString {
  val colors = Gl.colors
  var highlighted by remember(code, language, colors) { mutableStateOf(AnnotatedString(code)) }
  LaunchedEffect(code, language, colors) {
    highlighted = withContext(Dispatchers.Default) { highlight(code, language, colors) }
  }
  return highlighted
}
