package com.markleaf.notes.data.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The snippet list as stored (#436). Robolectric because the codec uses the
 * platform's `org.json`, which plain JVM tests only have as stubs.
 */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [33])
class SnippetButtonsStorageTest {
    private val repo = AppSettingsRepository(InMemoryPreferencesDataStore())

    @Test
    fun anUntouchedStoreHasNoSnippets() = runTest {
        assertEquals(emptyList<SnippetButton>(), repo.settings.first().snippetButtons)
    }

    @Test
    fun theListRoundTripsInOrderWithAnythingInTheText() = runTest {
        val buttons = listOf(
            SnippetButton(label = "", text = "[[{|}]]"),
            SnippetButton(label = "fn", text = "```kotlin\n{|}\n```\n"),
            SnippetButton(label = "q", text = "\"quoted\" \\ {braces} 🌿")
        )

        repo.setSnippetButtons(buttons)

        assertEquals(buttons, repo.settings.first().snippetButtons)
    }

    @Test
    fun anEmptyListGoesBackToTheDefault() = runTest {
        repo.setSnippetButtons(listOf(SnippetButton(label = "", text = "#tag")))
        repo.setSnippetButtons(emptyList())

        assertTrue(repo.settings.first().snippetButtons.isEmpty())
    }

    @Test
    fun moreThanTheLimitIsCut() = runTest {
        repo.setSnippetButtons(List(SnippetButton.MAX_COUNT + 3) { SnippetButton(label = "", text = "s$it") })

        assertEquals(SnippetButton.MAX_COUNT, repo.settings.first().snippetButtons.size)
    }

    @Test
    fun aDamagedValueReadsAsNoSnippets() {
        assertEquals(emptyList<SnippetButton>(), SnippetButtonsCodec.decode("[{\"label\":"))
        assertEquals(emptyList<SnippetButton>(), SnippetButtonsCodec.decode("{}"))
    }

    @Test
    fun entriesWithoutTextAreDropped() {
        val decoded = SnippetButtonsCodec.decode(
            """[{"label":"a","text":""},{"label":"b"},7,{"text":"#ok"}]"""
        )

        assertEquals(listOf(SnippetButton(label = "", text = "#ok")), decoded)
    }
}
