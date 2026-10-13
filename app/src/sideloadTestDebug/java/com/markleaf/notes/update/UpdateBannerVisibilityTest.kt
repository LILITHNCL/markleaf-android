package com.markleaf.notes.update

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.markleaf.notes.R
import com.markleaf.notes.domain.model.Note
import com.markleaf.notes.domain.repository.NoteRepository
import com.markleaf.notes.feature.notes.NotesListScreen
import com.markleaf.notes.ui.theme.MarkleafTheme
import com.markleaf.notes.ui.viewmodel.NotesViewModel
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The sideload update banner on the note list (#262, v2.67.0 candidate).
 *
 * It used to be the list's first item, drawing nothing until the check — run
 * on IO after the first frame — offered a version. A zero-height first item
 * counts as off screen, so the list anchored on the first note and the banner,
 * once it grew, sat above the viewport: the same hiding #434's conflict banner
 * had. Here the offer comes from the cached manifest, so it arrives the way a
 * real launch has it: after the list is drawn.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [33], qualifiers = "w411dp-h891dp-mdpi")
class UpdateBannerVisibilityTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val notes = (1..20).map { note("n$it", "Note $it") }

    @Before
    fun offerANewerVersion() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        UpdatePreferences(context).apply {
            setEnabled(true)
            // Checked just now, so the banner reads the cache and never fetches.
            setLastCheckedAt(System.currentTimeMillis())
            setCachedManifest(MANIFEST)
        }
    }

    @Test
    fun `an offer that arrives after the list is drawn is on screen`() {
        val viewModel = NotesViewModel(FakeNoteRepository(notes))
        composeRule.setContent {
            MarkleafTheme(dynamicColor = false) {
                NotesListScreen(viewModel = viewModel, onNoteClick = {}, onFabClick = {})
            }
        }

        val text = composeRule.activity.getString(R.string.update_banner_text, "999.0.0")
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText(text).assertIsDisplayed()
        // And the list did not lose its top either.
        composeRule.onNodeWithText("Note 1").assertIsDisplayed()
    }

    private fun note(id: String, title: String) = Note(
        id = id,
        title = title,
        contentMarkdown = "# $title",
        excerpt = "",
        createdAt = Instant.parse("2026-10-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-10-01T00:00:00Z")
    )

    private class FakeNoteRepository(private val all: List<Note>) : NoteRepository {
        override fun observeNotes(): Flow<List<Note>> = flowOf(all)
        override suspend fun getNote(noteId: String): Note? = all.firstOrNull { it.id == noteId }
        override fun observeNote(noteId: String): Flow<Note?> = flowOf(all.firstOrNull { it.id == noteId })
        override suspend fun getAllNotes(): List<Note> = all
        override suspend fun createNote(note: Note) = Unit
        override suspend fun updateNote(note: Note) = Unit
        override suspend fun updateDerivedTitle(noteId: String, title: String, excerpt: String) = Unit
        override suspend fun moveToTrash(noteId: String) = Unit
        override suspend fun setPinned(noteId: String, pinned: Boolean) = Unit
        override suspend fun setArchived(noteId: String, archived: Boolean) = Unit
        override suspend fun restoreFromTrash(noteId: String) = Unit
        override suspend fun deleteForever(noteId: String) = Unit
        override suspend fun reorderNotes(notes: List<Note>) = Unit
        override fun observeTrashedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeArchivedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override fun observeLockedNotes(): Flow<List<Note>> = flowOf(emptyList())
        override suspend fun setLocked(noteId: String, locked: Boolean) = Unit
        override suspend fun unlockAllLocked() = Unit
        override fun searchNotes(query: String): Flow<List<Note>> = flowOf(emptyList())
        override fun observeConflictNotes(): Flow<List<Note>> = flowOf(emptyList())
    }

    private companion object {
        val MANIFEST = """
            {
              "versionCode": 999999,
              "versionName": "999.0.0",
              "apkUrl": "https://example.invalid/markleaf.apk",
              "apkSizeBytes": 2831155,
              "sha256": "0be97352a650c3d1a3d2332fd18afc44e0c95a4abca347e9250a2b8a7eecf91a",
              "minSdk": 26,
              "releaseNotesUrl": "https://example.invalid/notes"
            }
        """.trimIndent()
    }
}
