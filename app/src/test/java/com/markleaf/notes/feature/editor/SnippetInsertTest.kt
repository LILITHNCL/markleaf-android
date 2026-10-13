package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.data.settings.SnippetButton
import org.junit.Assert.assertEquals
import org.junit.Test

class SnippetInsertTest {
    private val wikilink = SnippetButton(label = "", text = "[[{|}]]")

    @Test
    fun insertsAtTheCaretAndLeavesTheCaretAtTheMarker() {
        val result = insertSnippet(TextFieldValue("see  here", TextRange(4)), wikilink)

        assertEquals("see [[]] here", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun insertsAtTheEndOfTheNote() {
        val result = insertSnippet(TextFieldValue("abc", TextRange(3)), SnippetButton("", "#tag"))

        assertEquals("abc#tag", result.text)
        assertEquals(TextRange(7), result.selection)
    }

    @Test
    fun aSelectionIsReplacedLikeTyping() {
        val result = insertSnippet(TextFieldValue("one two", TextRange(4, 7)), wikilink)

        assertEquals("one [[]]", result.text)
        assertEquals(TextRange(6), result.selection)
    }

    @Test
    fun aStaleSelectionPastTheEndIsClamped() {
        val result = insertSnippet(TextFieldValue("ab", TextRange(9)), wikilink)

        assertEquals("ab[[]]", result.text)
        assertEquals(TextRange(4), result.selection)
    }
}
