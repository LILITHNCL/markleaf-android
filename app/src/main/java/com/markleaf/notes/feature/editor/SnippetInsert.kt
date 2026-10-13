package com.markleaf.notes.feature.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.markleaf.notes.data.settings.SnippetButton

/**
 * [snippet] at the caret, with the caret left where its marker was (#436).
 *
 * The row only offers snippets while nothing is selected, so in practice this
 * inserts; a selection that slips through is replaced, the way typing would.
 */
internal fun insertSnippet(value: TextFieldValue, snippet: SnippetButton): TextFieldValue {
    val start = value.selection.min.coerceIn(0, value.text.length)
    val end = value.selection.max.coerceIn(start, value.text.length)
    return value.copy(
        text = value.text.replaceRange(start, end, snippet.insertedText),
        selection = TextRange(start + snippet.caretOffset)
    )
}
