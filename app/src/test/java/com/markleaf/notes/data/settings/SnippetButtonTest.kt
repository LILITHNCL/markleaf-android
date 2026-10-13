package com.markleaf.notes.data.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SnippetButtonTest {

    @Test
    fun `the caret marker is taken out and the caret lands where it was`() {
        val snippet = SnippetButton(label = "", text = "[[{|}]]")

        assertEquals("[[]]", snippet.insertedText)
        assertEquals(2, snippet.caretOffset)
    }

    @Test
    fun `without a marker the caret goes to the end`() {
        val snippet = SnippetButton(label = "", text = "#idea ")

        assertEquals("#idea ", snippet.insertedText)
        assertEquals(6, snippet.caretOffset)
    }

    @Test
    fun `a multi-line snippet keeps its line breaks and caret`() {
        val snippet = SnippetButton(label = "fn", text = "```kotlin\n{|}\n```\n")

        assertEquals("```kotlin\n\n```\n", snippet.insertedText)
        assertEquals("```kotlin\n".length, snippet.caretOffset)
    }

    @Test
    fun `a given label is used as is, trimmed`() {
        assertEquals("NG", SnippetButton(label = "  NG ", text = "[[Nei Gong]]").displayLabel)
    }

    @Test
    fun `a blank label falls back to the start of the first non-empty line`() {
        assertEquals("[[]]", SnippetButton(label = "", text = "[[{|}]]").displayLabel)
        assertEquals("- [ ] ", SnippetButton(label = " ", text = "\n- [ ] task").displayLabel)
        assertEquals("callou", SnippetButton(label = "", text = "callout block").displayLabel)
    }

    @Test
    fun `labels are cut by code point, so an emoji is never split`() {
        val sixEmoji = "🌿🌿🌿🌿🌿🌿"

        assertEquals(sixEmoji, SnippetButton.limitLabel(sixEmoji + "🌿"))
        assertEquals(sixEmoji, SnippetButton(label = "", text = sixEmoji + "🌿🌿").displayLabel)
    }

    @Test
    fun `the preview is one line and ellipsized`() {
        assertEquals("a b c", SnippetButton(label = "", text = "a\n b\t{|}c").preview)
        val long = SnippetButton(label = "", text = "x".repeat(100)).preview
        assertEquals(61, long.length)
        assertEquals('…', long.last())
    }

    @Test
    fun `a snippet that inserts something and has a label is valid`() {
        assertNull(SnippetButton(label = "", text = "[[{|}]]").problem)
        assertNull(SnippetButton(label = "br", text = "  \n").problem)
    }

    @Test
    fun `nothing to insert is a problem`() {
        assertEquals(SnippetProblem.EMPTY, SnippetButton(label = "x", text = "").problem)
        assertEquals(SnippetProblem.EMPTY, SnippetButton(label = "x", text = "{|}").problem)
    }

    @Test
    fun `two markers is a problem`() {
        assertEquals(SnippetProblem.SEVERAL_MARKERS, SnippetButton(label = "", text = "[{|}]({|})").problem)
    }

    @Test
    fun `only whitespace with no label is a problem`() {
        assertEquals(SnippetProblem.LABEL_NEEDED, SnippetButton(label = " ", text = "  \n").problem)
    }
}
