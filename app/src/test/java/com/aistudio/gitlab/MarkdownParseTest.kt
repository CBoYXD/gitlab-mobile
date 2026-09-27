package com.aistudio.gitlab

import androidx.compose.ui.text.LinkAnnotation
import com.aistudio.gitlab.ui.components.MarkdownContext
import com.aistudio.gitlab.ui.components.MdBlock
import com.aistudio.gitlab.ui.components.parseMarkdown
import com.aistudio.gitlab.ui.theme.LightGlColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParseTest {

  private val ctx = MarkdownContext(webBase = "https://gitlab.com/group/app", rawBase = "https://gitlab.com/group/app/-/raw/main/")

  private fun parse(md: String) = parseMarkdown(md, LightGlColors, ctx)

  @Test
  fun `headings paragraphs and emphasis`() {
    val blocks = parse("# Title\n\nSome **bold** and _italic_ text.")
    assertEquals(2, blocks.size)
    assertEquals(1, (blocks[0] as MdBlock.Heading).level)
    assertEquals("Title", (blocks[0] as MdBlock.Heading).text.text)
    assertEquals("Some bold and italic text.", (blocks[1] as MdBlock.Paragraph).text.text)
  }

  @Test
  fun `task lists keep their check state`() {
    val list = parse("- [x] done\n- [ ] todo\n- plain").single() as MdBlock.ListBlock
    assertEquals(listOf(true, false, null), list.items.map { it.checked })
  }

  @Test
  fun `fenced code keeps its text and language`() {
    val code = parse("```kotlin\nval x = 1\n```").single() as MdBlock.Code
    assertEquals("val x = 1", code.code.text)
    assertEquals("kotlin", code.language)
  }

  @Test
  fun `tables have header and rows`() {
    val table = parse("| a | b |\n|---|---|\n| 1 | 2 |").single() as MdBlock.Table
    assertEquals(listOf("a", "b"), table.header.map { it.text })
    assertEquals(listOf("1", "2"), table.rows.single().map { it.text })
  }

  @Test
  fun `relative images resolve against the repository`() {
    val images = parse("![logo](docs/logo.png)").single() as MdBlock.Images
    assertEquals("https://gitlab.com/group/app/-/raw/main/docs/logo.png", images.images.single().url)
  }

  @Test
  fun `issue and merge request references become links`() {
    val paragraph = parse("Fixes #12 and !3").single() as MdBlock.Paragraph
    val urls = paragraph.text.getLinkAnnotations(0, paragraph.text.length).map { (it.item as LinkAnnotation.Clickable).tag }
    assertEquals(listOf("https://gitlab.com/group/app/-/issues/12", "https://gitlab.com/group/app/-/merge_requests/3"), urls)
  }

  @Test
  fun `badge rows become image blocks`() {
    val images = parse("[![ci](https://x/ci.svg)](https://x/ci) [![cov](https://x/cov.svg)](https://x/cov)").single() as MdBlock.Images
    assertEquals(2, images.images.size)
    assertTrue(images.images.all { it.link != null })
  }

  @Test
  fun `emoji shortcodes render outside code`() {
    val blocks = parse("Ship it :rocket: `:rocket:`")
    val text = (blocks.single() as MdBlock.Paragraph).text.text
    assertTrue(text.startsWith("Ship it 🚀"))
    assertTrue(text.contains(":rocket:"))
  }
}
