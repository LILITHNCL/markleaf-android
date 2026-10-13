package com.markleaf.notes.data.settings

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * The snippet buttons as one DataStore string (#436): a JSON array of
 * `{"label": …, "text": …}`, in row order.
 *
 * JSON rather than a home-made separator because a snippet is free text — it
 * can hold any character a separator could be, newlines included.
 */
internal object SnippetButtonsCodec {
    fun encode(buttons: List<SnippetButton>): String {
        val array = JSONArray()
        buttons.forEach { button ->
            array.put(JSONObject().put(LABEL, button.label).put(TEXT, button.text))
        }
        return array.toString()
    }

    /**
     * Only [encode] writes this value, so anything unreadable means a damaged
     * file rather than a format to support. The row then shows no buttons
     * instead of failing to open the editor, and the next change in Settings
     * writes a clean list over it. Entries without text are dropped for the
     * same reason: a button that inserts nothing has no way to have been saved.
     */
    fun decode(raw: String?): List<SnippetButton> {
        if (raw.isNullOrBlank()) return emptyList()
        val array = try {
            JSONArray(raw)
        } catch (_: JSONException) {
            return emptyList()
        }
        return (0 until array.length())
            .mapNotNull { index ->
                val entry = array.optJSONObject(index) ?: return@mapNotNull null
                val text = entry.optString(TEXT)
                if (text.isEmpty()) null else SnippetButton(label = entry.optString(LABEL), text = text)
            }
            .take(SnippetButton.MAX_COUNT)
    }

    private const val LABEL = "label"
    private const val TEXT = "text"
}
