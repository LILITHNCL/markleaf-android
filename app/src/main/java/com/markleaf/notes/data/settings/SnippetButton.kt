package com.markleaf.notes.data.settings

/**
 * A button the user adds to the row above the keyboard, inserting [text] at the
 * caret (#436). None exist until someone adds one in Settings → Markdown.
 *
 * [text] is kept exactly as typed, [CARET_MARKER] included, so reopening it in
 * Settings shows what was written; the marker is resolved only when inserting.
 * [label] may be blank, in which case [displayLabel] takes the start of the
 * snippet instead.
 */
data class SnippetButton(
    val label: String,
    val text: String
) {
    /** What lands in the note: [text] with its caret marker taken out. */
    val insertedText: String
        get() = caretMarkerIndex().let { at ->
            if (at < 0) text else text.removeRange(at, at + CARET_MARKER.length)
        }

    /** Where the caret ends up within [insertedText]: the marker, or the end. */
    val caretOffset: Int
        get() = caretMarkerIndex().let { at -> if (at < 0) insertedText.length else at }

    /** The text on the button. */
    val displayLabel: String
        get() = label.trim().ifEmpty { labelFromText(insertedText) }

    /** One line of what the button inserts, for its tooltip and TalkBack. */
    val preview: String
        get() = insertedText.replace(Regex("\\s+"), " ").trim().let { flat ->
            if (flat.length <= PREVIEW_LENGTH) flat else flat.take(PREVIEW_LENGTH).trimEnd() + "…"
        }

    /** Why this can't be saved yet, or null when it can. */
    val problem: SnippetProblem?
        get() = when {
            insertedText.isEmpty() -> SnippetProblem.EMPTY
            caretMarkerIndex() != text.lastIndexOf(CARET_MARKER) -> SnippetProblem.SEVERAL_MARKERS
            displayLabel.isEmpty() -> SnippetProblem.LABEL_NEEDED
            else -> null
        }

    private fun caretMarkerIndex(): Int = text.indexOf(CARET_MARKER)

    companion object {
        /**
         * Typed into a snippet where the caret should land: `[[{|}]]` inserts
         * `[[]]` with the caret between the brackets. Picked because it reads as
         * a caret, needs no translation, and plain Markdown has no use for it —
         * a bare `|` would collide with tables.
         */
        const val CARET_MARKER = "{|}"

        /** Enough for a working set; past this the row is mostly scrolling. */
        const val MAX_COUNT = 10

        /** Counted in code points, so an emoji label isn't cut in half. */
        const val MAX_LABEL_LENGTH = 6

        /** A snippet, not a template library; also bounds the settings file. */
        const val MAX_TEXT_LENGTH = 2000

        private const val PREVIEW_LENGTH = 60

        /** [value] cut to [MAX_LABEL_LENGTH] code points. */
        fun limitLabel(value: String): String = value.takeCodePoints(MAX_LABEL_LENGTH)

        private fun labelFromText(inserted: String): String =
            inserted.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
                ?.takeCodePoints(MAX_LABEL_LENGTH)
                .orEmpty()

        private fun String.takeCodePoints(count: Int): String =
            if (codePointCount(0, length) <= count) this else substring(0, offsetByCodePoints(0, count))
    }
}

/** What stops a snippet from being saved, shown under its field in Settings. */
enum class SnippetProblem {
    /** Nothing would be inserted. */
    EMPTY,

    /** More than one [SnippetButton.CARET_MARKER]; the caret can only go one place. */
    SEVERAL_MARKERS,

    /** No label given and the snippet is only spaces or line breaks, so there's nothing to show. */
    LABEL_NEEDED
}
