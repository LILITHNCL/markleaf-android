package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.text.TextRange
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.data.settings.SnippetButton
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The user's snippet buttons in the real editor (#436): on the row with the
 * formatting button turned off, inserting at the caret and saving, and gone
 * while text is selected.
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorSnippetButtonsTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }
    private val content = "Practice notes\n\nsee "
    private val wikilink = SnippetButton(label = "", text = "[[{|}]]")

    private fun createNote(): String {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Practice notes",
                    contentMarkdown = content,
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        return noteId
    }

    private fun show(noteId: String) {
        // "Show formatting button" off: the snippets must not depend on it.
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore()).also {
            runBlocking {
                it.setShowFormattingButton(false)
                it.setSnippetButtons(listOf(wikilink))
            }
        }
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Practice notes", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.waitForIdle()
    }

    private fun field() = composeRule.onNodeWithContentDescription(context.getString(R.string.note_content))

    private fun snippetButton() =
        composeRule.onNodeWithContentDescription(context.getString(R.string.snippet_button_insert, wikilink.preview))

    @Test
    fun tappingASnippetInsertsItAtTheCaretAndSavesIt() {
        val noteId = createNote()
        show(noteId)
        field().performTextInputSelection(TextRange(content.length))
        composeRule.waitForIdle()

        snippetButton().performClick()
        composeRule.waitForIdle()

        val node = field().fetchSemanticsNode()
        assertEquals("$content[[]]", node.config[SemanticsProperties.EditableText].text)
        assertEquals(TextRange(content.length + 2), node.config[SemanticsProperties.TextSelectionRange])
        composeRule.waitUntil(timeoutMillis = 5_000) {
            runBlocking { repo.getNote(noteId)?.contentMarkdown } == "$content[[]]"
        }
    }

    @Test
    fun aSelectionTakesTheSnippetsOffTheRow() {
        show(createNote())
        field().performTextInputSelection(TextRange(content.length))
        composeRule.waitForIdle()
        snippetButton().assertExists()

        field().performTextInputSelection(TextRange(0, 8))
        composeRule.waitForIdle()

        snippetButton().assertDoesNotExist()
    }
}
