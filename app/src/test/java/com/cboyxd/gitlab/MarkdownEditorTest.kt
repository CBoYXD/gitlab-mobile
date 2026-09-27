package com.cboyxd.gitlab

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.cboyxd.gitlab.ui.components.link
import com.cboyxd.gitlab.ui.components.prefixLines
import com.cboyxd.gitlab.ui.components.wrap
import org.junit.Assert.assertEquals
import org.junit.Test

class MarkdownEditorTest {

  @Test
  fun `bold wraps the selection and keeps it selected`() {
    val result = TextFieldValue("make this loud", TextRange(5, 9)).wrap("**", "**", "bold text")
    assertEquals("make **this** loud", result.text)
    assertEquals(TextRange(7, 11), result.selection)
  }

  @Test
  fun `bold without a selection inserts a selected placeholder`() {
    val result = TextFieldValue("x ", TextRange(2)).wrap("**", "**", "bold text")
    assertEquals("x **bold text**", result.text)
    assertEquals(TextRange(4, 13), result.selection)
  }

  @Test
  fun `list prefix applies to every selected line and toggles off`() {
    val on = TextFieldValue("one\ntwo\nthree", TextRange(0, 7)).prefixLines("- ")
    assertEquals("- one\n- two\nthree", on.text)
    val off = TextFieldValue(on.text, TextRange(0, 11)).prefixLines("- ")
    assertEquals("one\ntwo\nthree", off.text)
  }

  @Test
  fun `numbered list counts lines`() {
    val result = TextFieldValue("a\nb", TextRange(0, 3)).prefixLines("1. ")
    assertEquals("1. a\n2. b", result.text)
  }

  @Test
  fun `link puts the cursor after the url stub`() {
    val result = TextFieldValue("docs", TextRange(0, 4)).link()
    assertEquals("[docs](https://)", result.text)
    assertEquals(TextRange(15), result.selection)
  }
}
