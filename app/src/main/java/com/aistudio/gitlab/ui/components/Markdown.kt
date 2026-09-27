package com.aistudio.gitlab.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.CheckBoxOutlineBlank
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aistudio.gitlab.ui.theme.Gl
import com.aistudio.gitlab.ui.theme.GlColors
import com.aistudio.gitlab.ui.theme.GlRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.intellij.markdown.MarkdownElementTypes
import org.intellij.markdown.MarkdownTokenTypes
import org.intellij.markdown.ast.ASTNode
import org.intellij.markdown.ast.getTextInNode
import org.intellij.markdown.flavours.gfm.GFMElementTypes
import org.intellij.markdown.flavours.gfm.GFMFlavourDescriptor
import org.intellij.markdown.flavours.gfm.GFMTokenTypes
import org.intellij.markdown.parser.MarkdownParser

/**
 * Where relative links and images in a document point. [webBase] is the project's web URL
 * (https://host/group/project); [rawBase] serves repository files, e.g. `webBase/-/raw/main/`.
 * Links are opened through [onLink]; it returns true when the app handled the URL itself.
 */
@Immutable
data class MarkdownContext(
  val webBase: String? = null,
  val rawBase: String? = null,
  val onLink: ((String) -> Boolean)? = null,
)

/** Longest document rendered; the rest is replaced by a note so a huge README can't stall the UI. */
private const val MAX_MARKDOWN_CHARS = 120_000

@Immutable
sealed interface MdBlock {
  data class Heading(val level: Int, val text: AnnotatedString) : MdBlock
  data class Paragraph(val text: AnnotatedString) : MdBlock
  data class Images(val images: List<MdImage>) : MdBlock
  data class Code(val code: AnnotatedString, val language: String?) : MdBlock
  data class Quote(val blocks: List<MdBlock>) : MdBlock
  data class ListBlock(val items: List<MdListItem>) : MdBlock
  data class Table(val header: List<AnnotatedString>, val rows: List<List<AnnotatedString>>) : MdBlock
  data object Rule : MdBlock
}

@Immutable
data class MdImage(val url: String, val alt: String, val link: String?)

@Immutable
data class MdListItem(val marker: String, val checked: Boolean?, val blocks: List<MdBlock>)

/**
 * GitLab Flavored Markdown rendered with the app's type scale and colors. Parsing and
 * highlighting run on a background thread; plain text shows until they finish.
 */
@Composable
fun MarkdownText(
  markdown: String,
  modifier: Modifier = Modifier,
  context: MarkdownContext = MarkdownContext(),
  textStyle: TextStyle = Gl.type.body,
) {
  val colors = Gl.colors
  val uriHandler = LocalUriHandler.current
  val open: (String) -> Unit = remember(context, uriHandler) {
    { url -> if (context.onLink?.invoke(url) != true) runCatching { uriHandler.openUri(url) } }
  }
  val blocks by produceState<List<MdBlock>?>(null, markdown, colors, context.webBase, context.rawBase) {
    value = withContext(Dispatchers.Default) {
      runCatching { MarkdownBuilder(colors, context, open).build(markdown) }.getOrNull()
    }
  }
  val parsed = blocks
  if (parsed == null) {
    Text(markdown.take(4_000), style = textStyle, color = colors.textPrimary, modifier = modifier)
    return
  }
  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    parsed.forEach { MdBlockView(it, textStyle, context) }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MdBlockView(block: MdBlock, textStyle: TextStyle, context: MarkdownContext) {
  val c = Gl.colors
  when (block) {
    is MdBlock.Heading -> {
      val style = when (block.level) {
        1 -> Gl.type.title
        2 -> Gl.type.sectionTitle
        else -> Gl.type.rowTitle.copy(fontWeight = FontWeight.SemiBold)
      }
      Column(modifier = Modifier.padding(top = if (block.level <= 2) 8.dp else 4.dp)) {
        Text(block.text, style = style, color = c.textPrimary, modifier = Modifier.semantics { heading() })
        if (block.level <= 2) {
          Spacer(Modifier.height(6.dp))
          GlDivider()
        }
      }
    }
    is MdBlock.Paragraph -> Text(block.text, style = textStyle, color = c.textPrimary)
    is MdBlock.Images -> {
      if (block.images.size == 1 && block.images.first().link == null) {
        MdImageView(block.images.first(), fullWidth = true, context)
      } else {
        // Badge rows (shields.io and friends) wrap like text.
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
          block.images.forEach { MdImageView(it, fullWidth = false, context) }
        }
      }
    }
    is MdBlock.Code -> {
      val scroll = rememberScrollState()
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(GlRadius.control))
          .background(c.codeBg)
          .border(1.dp, c.border, RoundedCornerShape(GlRadius.control))
          .horizontalScroll(scroll)
          .padding(12.dp),
      ) {
        Text(block.code, style = Gl.type.mono, color = c.textPrimary, softWrap = false)
      }
    }
    is MdBlock.Quote -> Row(modifier = Modifier.height(IntrinsicSize.Min)) {
      Box(Modifier.width(3.dp).fillMaxHeight().background(c.borderStrong, RoundedCornerShape(2.dp)))
      Spacer(Modifier.width(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        block.blocks.forEach { MdBlockView(it, textStyle.copy(color = c.textSecondary), context) }
      }
    }
    is MdBlock.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      block.items.forEach { item ->
        Row {
          Box(modifier = Modifier.widthIn(min = 24.dp).padding(end = 6.dp), contentAlignment = Alignment.TopEnd) {
            when (item.checked) {
              null -> Text(item.marker, style = textStyle, color = c.textSecondary)
              else -> Icon(
                if (item.checked) Icons.Outlined.CheckBox else Icons.Outlined.CheckBoxOutlineBlank,
                contentDescription = if (item.checked) "Done" else "Not done",
                tint = if (item.checked) c.textPrimary else c.textSecondary,
                modifier = Modifier.padding(top = 2.dp).size(18.dp),
              )
            }
          }
          Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.weight(1f)) {
            item.blocks.forEach { MdBlockView(it, textStyle, context) }
          }
        }
      }
    }
    is MdBlock.Table -> {
      val scroll = rememberScrollState()
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(GlRadius.control))
          .border(1.dp, c.border, RoundedCornerShape(GlRadius.control))
          .horizontalScroll(scroll),
      ) {
        Column(modifier = Modifier.width(IntrinsicSize.Max)) {
          val columns = maxOf(block.header.size, block.rows.maxOfOrNull { it.size } ?: 0)
          val all = listOf(block.header) + block.rows
          all.forEachIndexed { rowIndex, row ->
            Row(modifier = Modifier.background(if (rowIndex == 0) c.surface2 else c.surface)) {
              for (col in 0 until columns) {
                Text(
                  text = row.getOrNull(col) ?: AnnotatedString(""),
                  style = if (rowIndex == 0) Gl.type.label else Gl.type.bodySmall,
                  color = c.textPrimary,
                  modifier = Modifier.widthIn(min = 80.dp, max = 260.dp).padding(horizontal = 10.dp, vertical = 8.dp),
                )
              }
            }
            if (rowIndex < all.lastIndex) GlDivider()
          }
        }
      }
    }
    MdBlock.Rule -> GlDivider()
  }
}

@Composable
private fun MdImageView(image: MdImage, fullWidth: Boolean, context: MarkdownContext) {
  val c = Gl.colors
  val open = LocalUriHandler.current
  val base = Modifier
    .clip(RoundedCornerShape(if (fullWidth) GlRadius.chip else 4.dp))
    .semantics { contentDescription = image.alt.ifBlank { "Image" } }
  val clickable = image.link?.let { link ->
    Modifier.pressable(onClick = { if (context.onLink?.invoke(link) != true) runCatching { open.openUri(link) } }, scaleOnPress = false)
  } ?: Modifier
  AsyncImage(
    model = rememberImageModel(image.url),
    contentDescription = null,
    contentScale = if (fullWidth) ContentScale.FillWidth else ContentScale.Fit,
    modifier = if (fullWidth) {
      base.then(clickable).fillMaxWidth().heightIn(max = 480.dp).border(1.dp, c.imageOutline, RoundedCornerShape(GlRadius.chip))
    } else {
      base.then(clickable).height(20.dp).widthIn(max = 220.dp)
    },
  )
}

// ---------------------------------------------------------------------------------------------
// Parsing
// ---------------------------------------------------------------------------------------------

private val HTML_IMG = Regex("""<img\b[^>]*\bsrc\s*=\s*["']([^"']+)["'][^>]*>""", RegexOption.IGNORE_CASE)
private val HTML_ALT = Regex("""\balt\s*=\s*["']([^"']*)["']""", RegexOption.IGNORE_CASE)
private val HTML_TAG = Regex("""<[^>]+>""")
private val REFERENCE = Regex("""(?<![\w/&])(#|!|%|&)(\d+)\b|(?<![\w/@.])@([A-Za-z0-9_][A-Za-z0-9_.\-]*[A-Za-z0-9_])""")

/** Parses GitLab Flavored Markdown into render blocks. Pure; safe to call off the main thread. */
internal fun parseMarkdown(markdown: String, colors: GlColors, context: MarkdownContext, open: (String) -> Unit = {}): List<MdBlock> =
  MarkdownBuilder(colors, context, open).build(markdown)

private class MarkdownBuilder(
  private val colors: GlColors,
  private val context: MarkdownContext,
  private val open: (String) -> Unit,
) {
  private lateinit var src: String

  fun build(markdown: String): List<MdBlock> {
    val capped = if (markdown.length > MAX_MARKDOWN_CHARS) markdown.take(MAX_MARKDOWN_CHARS) + "\n\n*The rest is too long to show here.*" else markdown
    src = replaceEmojiShortcodes(capped)
    val root = MarkdownParser(GFMFlavourDescriptor()).buildMarkdownTreeFromString(src)
    return blocks(root.children)
  }

  private fun text(node: ASTNode) = node.getTextInNode(src).toString()

  private fun blocks(nodes: List<ASTNode>): List<MdBlock> = nodes.mapNotNull { block(it) }.flatMap { it }

  private fun block(node: ASTNode): List<MdBlock>? = when (node.type) {
    MarkdownElementTypes.ATX_1, MarkdownElementTypes.ATX_2, MarkdownElementTypes.ATX_3,
    MarkdownElementTypes.ATX_4, MarkdownElementTypes.ATX_5, MarkdownElementTypes.ATX_6 -> {
      val level = node.type.name.last().digitToInt()
      val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.ATX_CONTENT }
      listOf(MdBlock.Heading(level, inline(content?.children ?: emptyList()).trimAnnotated()))
    }
    MarkdownElementTypes.SETEXT_1, MarkdownElementTypes.SETEXT_2 -> {
      val content = node.children.firstOrNull { it.type == MarkdownTokenTypes.SETEXT_CONTENT }
      listOf(MdBlock.Heading(if (node.type == MarkdownElementTypes.SETEXT_1) 1 else 2, inline(content?.children ?: emptyList()).trimAnnotated()))
    }
    MarkdownElementTypes.PARAGRAPH -> paragraph(node.children)
    MarkdownElementTypes.CODE_FENCE -> {
      val lang = node.children.firstOrNull { it.type == MarkdownTokenTypes.FENCE_LANG }?.let { text(it) }
      val body = node.children
        .dropWhile { it.type != MarkdownTokenTypes.EOL }
        .drop(1)
        .filterNot { it.type == MarkdownTokenTypes.CODE_FENCE_END }
        .joinToString("") { text(it) }
        .trimEnd('\n')
      listOf(MdBlock.Code(highlight(body, languageForFence(lang), colors), lang))
    }
    MarkdownElementTypes.CODE_BLOCK -> {
      val body = node.children.joinToString("") { if (it.type == MarkdownTokenTypes.CODE_LINE) text(it).removePrefix("    ") else text(it) }
      listOf(MdBlock.Code(AnnotatedString(body.trimEnd('\n')), null))
    }
    MarkdownElementTypes.BLOCK_QUOTE -> listOf(MdBlock.Quote(blocks(node.children.filterNot { it.type == MarkdownTokenTypes.BLOCK_QUOTE })))
    MarkdownElementTypes.UNORDERED_LIST, MarkdownElementTypes.ORDERED_LIST -> listOf(list(node))
    GFMElementTypes.TABLE -> listOf(table(node))
    MarkdownTokenTypes.HORIZONTAL_RULE -> listOf(MdBlock.Rule)
    MarkdownElementTypes.HTML_BLOCK -> html(text(node))
    MarkdownElementTypes.LINK_DEFINITION, MarkdownTokenTypes.EOL, MarkdownTokenTypes.WHITE_SPACE -> null
    else -> {
      val raw = text(node).trim()
      if (raw.isEmpty()) null else listOf(MdBlock.Paragraph(AnnotatedString(raw)))
    }
  }

  private fun list(node: ASTNode): MdBlock.ListBlock {
    val ordered = node.type == MarkdownElementTypes.ORDERED_LIST
    var number = 1
    val items = node.children.filter { it.type == MarkdownElementTypes.LIST_ITEM }.map { item ->
      val markerNode = item.children.firstOrNull { it.type == MarkdownTokenTypes.LIST_NUMBER || it.type == MarkdownTokenTypes.LIST_BULLET }
      val marker = if (ordered) {
        val parsed = markerNode?.let { text(it).trim().trimEnd('.', ')').toIntOrNull() }
        if (parsed != null && number == 1) number = parsed
        "${number++}."
      } else "•"
      val box = item.children.firstOrNull { it.type == GFMTokenTypes.CHECK_BOX }?.let { text(it).contains('x', ignoreCase = true) }
      val content = item.children.filterNot {
        it.type == MarkdownTokenTypes.LIST_BULLET || it.type == MarkdownTokenTypes.LIST_NUMBER || it.type == GFMTokenTypes.CHECK_BOX
      }
      MdListItem(marker, box, blocks(content))
    }
    return MdBlock.ListBlock(items)
  }

  private fun table(node: ASTNode): MdBlock.Table {
    fun cells(row: ASTNode) = row.children.filter { it.type == GFMTokenTypes.CELL }.map { inline(it.children).trimAnnotated() }
    val header = node.children.firstOrNull { it.type == GFMElementTypes.HEADER }?.let(::cells).orEmpty()
    val rows = node.children.filter { it.type == GFMElementTypes.ROW }.map(::cells)
    return MdBlock.Table(header, rows)
  }

  /** Paragraphs made only of images (optionally linked) become image blocks; the rest is text. */
  private fun paragraph(children: List<ASTNode>): List<MdBlock> {
    val meaningful = children.filterNot { it.type == MarkdownTokenTypes.EOL || it.type == MarkdownTokenTypes.WHITE_SPACE || (it.type == MarkdownTokenTypes.TEXT && text(it).isBlank()) }
    val images = meaningful.mapNotNull { imageOf(it) }
    if (meaningful.isNotEmpty() && images.size == meaningful.size) return listOf(MdBlock.Images(images))
    val htmlImages = meaningful.filter { it.type == MarkdownTokenTypes.HTML_TAG }.mapNotNull { htmlImage(text(it)) }
    val result = mutableListOf<MdBlock>()
    val body = inline(children).trimAnnotated()
    if (body.text.isNotBlank()) result += MdBlock.Paragraph(body)
    if (htmlImages.isNotEmpty()) result += MdBlock.Images(htmlImages)
    return result
  }

  private fun imageOf(node: ASTNode): MdImage? = when (node.type) {
    MarkdownElementTypes.IMAGE -> {
      val link = node.children.firstOrNull { it.type == MarkdownElementTypes.INLINE_LINK }
      val dest = link?.children?.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.let { text(it) }
      val alt = link?.children?.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }?.let { text(it).trim('[', ']') }.orEmpty()
      dest?.let { MdImage(resolve(it.trim('<', '>'), image = true), alt, null) }
    }
    MarkdownElementTypes.INLINE_LINK -> {
      // [![alt](img)](href): a linked badge.
      val textNode = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
      val inner = textNode?.children?.firstOrNull { it.type == MarkdownElementTypes.IMAGE }
      val href = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.let { text(it) }
      val onlyImage = textNode?.children?.count { it.type != MarkdownTokenTypes.LBRACKET && it.type != MarkdownTokenTypes.RBRACKET } == 1
      if (inner != null && onlyImage) imageOf(inner)?.copy(link = href?.let { resolve(it, image = false) }) else null
    }
    MarkdownTokenTypes.HTML_TAG -> htmlImage(text(node))
    else -> null
  }

  private fun htmlImage(tag: String): MdImage? {
    val src = HTML_IMG.find(tag)?.groupValues?.get(1) ?: return null
    val alt = HTML_ALT.find(tag)?.groupValues?.get(1).orEmpty()
    return MdImage(resolve(src, image = true), alt, null)
  }

  /** README HTML: keep images and readable text, drop the markup. */
  private fun html(raw: String): List<MdBlock> {
    val images = HTML_IMG.findAll(raw).map { m ->
      MdImage(resolve(m.groupValues[1], image = true), HTML_ALT.find(m.value)?.groupValues?.get(1).orEmpty(), null)
    }.toList()
    val textOnly = raw
      .replace(Regex("(?i)<br\\s*/?>"), "\n")
      .replace(HTML_TAG, "")
      .decodeEntities()
      .lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
    val out = mutableListOf<MdBlock>()
    if (images.isNotEmpty()) out += MdBlock.Images(images)
    if (textOnly.isNotBlank()) out += MdBlock.Paragraph(AnnotatedString(textOnly))
    return out
  }

  private fun inline(nodes: List<ASTNode>): AnnotatedString =
    linkReferences(buildAnnotatedString { nodes.forEach { appendInline(it) } })

  private fun AnnotatedString.Builder.appendInline(node: ASTNode) {
    when (node.type) {
      MarkdownTokenTypes.TEXT -> append(text(node))
      MarkdownTokenTypes.WHITE_SPACE -> append(' ')
      MarkdownTokenTypes.EOL -> append(' ')
      MarkdownTokenTypes.HARD_LINE_BREAK -> append('\n')
      MarkdownElementTypes.EMPH -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { node.children.filterNot { it.type == MarkdownTokenTypes.EMPH }.forEach { appendInline(it) } }
      MarkdownElementTypes.STRONG -> withStyle(SpanStyle(fontWeight = FontWeight.SemiBold)) { node.children.filterNot { it.type == MarkdownTokenTypes.EMPH }.forEach { appendInline(it) } }
      GFMElementTypes.STRIKETHROUGH -> withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)) { node.children.filterNot { it.type == GFMTokenTypes.TILDE }.forEach { appendInline(it) } }
      MarkdownElementTypes.CODE_SPAN -> {
        val code = node.children.filterNot { it.type == MarkdownTokenTypes.BACKTICK }.joinToString("") { text(it) }.trim()
        withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = colors.codeBg)) { append(" $code ") }
      }
      MarkdownElementTypes.INLINE_LINK -> {
        val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }
        val dest = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_DESTINATION }?.let { text(it) }
        val inner = label?.children?.filterNot { it.type == MarkdownTokenTypes.LBRACKET || it.type == MarkdownTokenTypes.RBRACKET }.orEmpty()
        if (dest == null) inner.forEach { appendInline(it) } else link(resolve(dest.trim('<', '>'), image = false)) { inner.forEach { appendInline(it) } }
      }
      MarkdownElementTypes.FULL_REFERENCE_LINK, MarkdownElementTypes.SHORT_REFERENCE_LINK -> {
        val label = node.children.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT || it.type == MarkdownElementTypes.LINK_LABEL }
        label?.children?.filterNot { it.type == MarkdownTokenTypes.LBRACKET || it.type == MarkdownTokenTypes.RBRACKET }?.forEach { appendInline(it) }
      }
      MarkdownElementTypes.AUTOLINK -> {
        val url = node.children.firstOrNull { it.type == MarkdownTokenTypes.AUTOLINK }?.let { text(it) } ?: text(node).trim('<', '>')
        link(url) { append(url) }
      }
      GFMTokenTypes.GFM_AUTOLINK, MarkdownTokenTypes.URL -> {
        val url = text(node)
        link(url) { append(url) }
      }
      MarkdownElementTypes.IMAGE -> {
        val alt = node.children.firstOrNull { it.type == MarkdownElementTypes.INLINE_LINK }
          ?.children?.firstOrNull { it.type == MarkdownElementTypes.LINK_TEXT }?.let { text(it).trim('[', ']') }
        if (!alt.isNullOrBlank()) append(alt)
      }
      MarkdownTokenTypes.HTML_TAG -> {
        val tag = text(node)
        if (tag.startsWith("<br", ignoreCase = true)) append('\n')
      }
      MarkdownTokenTypes.ESCAPED_BACKTICKS -> append('`')
      else -> if (node.children.isEmpty()) append(text(node).decodeEntities()) else node.children.forEach { appendInline(it) }
    }
  }

  private inline fun AnnotatedString.Builder.link(url: String, content: AnnotatedString.Builder.() -> Unit) {
    val style = TextLinkStyles(SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline))
    withLink(LinkAnnotation.Clickable(tag = url, styles = style) { open(url) }) { content() }
  }

  /**
   * GitLab references (#12 issues, !3 merge requests, @user) become links when a project is
   * known. Runs on the assembled text because the parser splits "!" and "#" into separate tokens;
   * existing links and inline code are left alone.
   */
  private fun linkReferences(built: AnnotatedString): AnnotatedString {
    val base = context.webBase?.trimEnd('/') ?: return built
    val host = base.substringBefore("://") + "://" + base.substringAfter("://").substringBefore('/')
    val taken = built.getLinkAnnotations(0, built.length).map { it.start until it.end } +
      built.spanStyles.filter { it.item.fontFamily == FontFamily.Monospace }.map { it.start until it.end }
    val matches = REFERENCE.findAll(built.text).filter { m -> taken.none { r -> m.range.first <= r.last && r.first <= m.range.last } }.toList()
    if (matches.isEmpty()) return built
    val style = TextLinkStyles(SpanStyle(color = colors.link, textDecoration = TextDecoration.Underline))
    return buildAnnotatedString {
      append(built)
      matches.forEach { match ->
        val url = when {
          match.groupValues[3].isNotEmpty() -> "$host/${match.groupValues[3]}"
          match.groupValues[1] == "#" -> "$base/-/issues/${match.groupValues[2]}"
          match.groupValues[1] == "!" -> "$base/-/merge_requests/${match.groupValues[2]}"
          match.groupValues[1] == "%" -> "$base/-/milestones/${match.groupValues[2]}"
          else -> "$base/-/epics/${match.groupValues[2]}"
        }
        addLink(LinkAnnotation.Clickable(tag = url, styles = style) { open(url) }, match.range.first, match.range.last + 1)
      }
    }
  }

  private fun resolve(raw: String, image: Boolean): String {
    val url = raw.trim()
    if (url.startsWith("http://") || url.startsWith("https://") || url.startsWith("mailto:") || url.startsWith("#")) return url
    val web = context.webBase?.trimEnd('/') ?: return url
    val host = web.substringBefore("://") + "://" + web.substringAfter("://").substringBefore('/')
    return when {
      url.startsWith("/uploads/") -> "$web$url"
      url.startsWith("/") -> "$host$url"
      image && context.rawBase != null -> context.rawBase.trimEnd('/') + "/" + url.removePrefix("./")
      else -> "$web/-/blob/HEAD/" + url.removePrefix("./")
    }
  }
}

private fun AnnotatedString.trimAnnotated(): AnnotatedString {
  val start = text.indexOfFirst { !it.isWhitespace() }
  if (start < 0) return AnnotatedString("")
  val end = text.indexOfLast { !it.isWhitespace() } + 1
  return subSequence(start, end)
}

private fun String.decodeEntities(): String = this
  .replace("&nbsp;", " ")
  .replace("&amp;", "&")
  .replace("&lt;", "<")
  .replace("&gt;", ">")
  .replace("&quot;", "\"")
  .replace("&#39;", "'")

private val SHORTCODE = Regex(""":([a-z0-9_+\-]+):""")

/** Common GitLab emoji shortcodes. Unknown codes stay as typed. */
private val EMOJI = mapOf(
  "rocket" to "🚀", "tada" to "🎉", "sparkles" to "✨", "bug" to "🐛", "fire" to "🔥", "zap" to "⚡",
  "boom" to "💥", "warning" to "⚠️", "white_check_mark" to "✅", "heavy_check_mark" to "✔️", "x" to "❌",
  "+1" to "👍", "thumbsup" to "👍", "-1" to "👎", "thumbsdown" to "👎", "heart" to "❤️", "eyes" to "👀",
  "memo" to "📝", "pencil" to "✏️", "pencil2" to "✏️", "lock" to "🔒", "unlock" to "🔓", "key" to "🔑",
  "star" to "⭐", "star2" to "🌟", "construction" to "🚧", "art" to "🎨", "recycle" to "♻️", "bulb" to "💡",
  "wrench" to "🔧", "hammer" to "🔨", "hammer_and_wrench" to "🛠️", "package" to "📦", "pushpin" to "📌",
  "link" to "🔗", "books" to "📚", "book" to "📖", "speech_balloon" to "💬", "thinking" to "🤔",
  "smile" to "😄", "smiley" to "😃", "slight_smile" to "🙂", "wink" to "😉", "laughing" to "😆", "joy" to "😂",
  "cry" to "😢", "confused" to "😕", "scream" to "😱", "sweat_smile" to "😅", "pray" to "🙏", "clap" to "👏",
  "wave" to "👋", "raised_hands" to "🙌", "ok_hand" to "👌", "muscle" to "💪", "point_right" to "👉",
  "arrow_up" to "⬆️", "arrow_down" to "⬇️", "arrow_right" to "➡️", "arrow_left" to "⬅️",
  "chart_with_upwards_trend" to "📈", "calendar" to "📅", "clock" to "🕐", "hourglass" to "⌛",
  "mag" to "🔍", "gear" to "⚙️", "globe_with_meridians" to "🌐", "computer" to "💻", "iphone" to "📱",
  "tools" to "🛠️", "test_tube" to "🧪", "lipstick" to "💄", "ambulance" to "🚑", "lock_with_ink_pen" to "🔏",
  "heavy_plus_sign" to "➕", "heavy_minus_sign" to "➖", "question" to "❓", "exclamation" to "❗",
  "information_source" to "ℹ️", "no_entry" to "⛔", "stop_sign" to "🛑", "rotating_light" to "🚨",
  "100" to "💯", "trophy" to "🏆", "medal" to "🏅", "gift" to "🎁", "coffee" to "☕", "beer" to "🍺",
  "fox" to "🦊", "tanuki" to "🦊", "robot" to "🤖", "handshake" to "🤝", "shipit" to "🚢", "ship" to "🚢",
)

/** Replaces :shortcode: with emoji outside fenced blocks and inline code. */
private fun replaceEmojiShortcodes(text: String): String {
  if (':' !in text) return text
  var inFence = false
  return text.lines().joinToString("\n") { line ->
    if (line.trimStart().startsWith("```") || line.trimStart().startsWith("~~~")) {
      inFence = !inFence
      line
    } else if (inFence) {
      line
    } else {
      line.split('`').mapIndexed { index, part ->
        if (index % 2 == 1) part else SHORTCODE.replace(part) { m -> EMOJI[m.groupValues[1]] ?: m.value }
      }.joinToString("`")
    }
  }
}
