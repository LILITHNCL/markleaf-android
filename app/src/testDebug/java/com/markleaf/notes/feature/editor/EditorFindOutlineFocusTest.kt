package com.markleaf.notes.feature.editor

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.data.local.AppDatabase
import com.markleaf.notes.data.repository.LocalNoteRepository
import com.markleaf.notes.data.settings.AppSettingsRepository
import com.markleaf.notes.data.settings.InMemoryPreferencesDataStore
import com.markleaf.notes.domain.model.Note
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The outline replaces the whole editor body, so the field that held focus is
 * gone when it closes. With Find still open, closing the outline gives focus
 * back to the search field rather than leaving nothing focused (#262, a Codex
 * review finding on #527). Focus mode is the opposite case — the editor stays
 * composed there and keeps its caret — which `FindBarFocusTest` covers.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w360dp-h640dp-mdpi")
class EditorFindOutlineFocusTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val repo by lazy { LocalNoteRepository(AppDatabase.getInstance(context)) }

    @Test
    fun closingTheOutlineWithFindOpenFocusesTheSearchFieldAgain() {
        val noteId = UUID.randomUUID().toString()
        runBlocking {
            repo.createNote(
                Note(
                    id = noteId,
                    title = "Pears",
                    contentMarkdown = "# Pears\n\nOne pear.\n\n## Tarts\n\nTwo pears.",
                    excerpt = "",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                )
            )
        }
        val settings = AppSettingsRepository(InMemoryPreferencesDataStore())
        composeRule.setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                EditorScreen(noteId = noteId, onBack = {}, settingsRepository = settings)
            }
        }
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription(context.getString(R.string.more_options)).performClick()
        composeRule.onNodeWithText(context.getString(R.string.find_in_note)).performClick()
        composeRule.waitForIdle()
        searchField().assertIsFocused()

        composeRule.onNodeWithContentDescription(context.getString(R.string.table_of_contents)).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription(context.getString(R.string.close)).performClick()
        composeRule.waitForIdle()

        searchField().assertIsFocused()
    }

    // The find field is the first of the bar's two text fields (find, replace).
    private fun searchField() = composeRule.onAllNodes(
        hasSetTextAction() and !hasContentDescription(context.getString(R.string.note_content))
    )[0]
}
