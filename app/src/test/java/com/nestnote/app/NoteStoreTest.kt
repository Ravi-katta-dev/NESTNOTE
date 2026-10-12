package com.nestnote.app

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NoteStoreTest {
    private lateinit var context: Context
    private lateinit var store: NoteStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().clear().commit()
        store = NoteStore(context)
    }

    @Test
    fun firstLoadReturnsStarterNotes() {
        val notes = store.load()
        assertTrue(notes.isNotEmpty())
        assertTrue(notes.any { it.title == "Project Plan" })
    }

    @Test
    fun saveAndLoadRoundTripsAllNoteFields() {
        val expected = listOf(NestNote(
            id = 42L, title = "Test title", body = "Body text",
            category = "Work", color = 3, pinned = true,
            reminder = "2026-10-12 09:30", updated = 123456L
        ))
        store.save(expected)
        assertEquals(expected, store.load())
    }

    @Test
    fun legacyRecordsWithoutCreatedTimestampUseTheirIdAsFallback() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit()
            .putString(
                "notes",
                """[{"id":77,"title":"Legacy","body":"Older note","category":"Ideas","updated":88}]"""
            )
            .commit()

        val loaded = store.load().single()
        assertEquals(77L, loaded.created)
        assertEquals("Legacy", loaded.title)
    }

    @Test
    fun createdTimestampSurvivesSaveAndLoad() {
        val original = NestNote(
            id = 9L, title = "Created date", body = "Keep original time",
            updated = 500L, created = 100L
        )
        store.save(listOf(original.copy(updated = 900L)))
        val loaded = store.load().single()
        assertEquals(100L, loaded.created)
        assertEquals(900L, loaded.updated)
    }

    @Test
    fun archiveAndRestoreStateSurviveSaveAndLoad() {
        val archived = NestNote(id = 19L, title = "Archived", body = "Keep it", archived = true)
        store.save(listOf(archived))
        assertEquals(archived, store.load().single())

        val restored = store.load().single().copy(archived = false)
        store.save(listOf(restored))
        assertEquals(restored, store.load().single())
        assertFalse(store.load().single().archived)
    }

    @Test
    fun legacyRecordsDefaultToNotArchived() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", """[{"id":78,"title":"Legacy","body":"Visible"}]""").commit()
        assertFalse(store.load().single().archived)
    }

    @Test
    fun noteWallpaperSelectionSurvivesSaveAndLoad() {
        val decorated = NestNote(
            id = 91L, title = "Wallpaper", body = "Botanical",
            color = 2, background = 1
        )
        store.save(listOf(decorated))
        assertEquals(decorated, store.load().single())
    }

    @Test
    fun legacyNotesDefaultToNoCustomWallpaper() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", """[{"id":92,"title":"Legacy","body":"No wallpaper"}]""").commit()
        assertEquals(0, store.load().single().background)
    }

    @Test
    fun savingEmptyListPersistsAnEmptyList() {
        store.save(emptyList())
        assertTrue(store.load().isEmpty())
    }

    @Test
    fun decodeRejectsDuplicateIdsInImportedBackup() {
        val raw = """[{"id":7,"title":"One","body":"A"},{"id":7,"title":"Two","body":"B"}]"""
        assertEquals(null, NoteStore.decode(raw, requireFields = true))
    }

    @Test
    fun decodeRejectsInvalidIdsAndOversizedFields() {
        val zeroId = """[{"id":0,"title":"Invalid","body":"Body"}]"""
        val longTitle = "x".repeat(NoteStore.MAX_TITLE_LENGTH + 1)
        val oversizedTitle = NoteStore.encode(listOf(NestNote(id = 8, title = longTitle, body = "Body")))
        assertEquals(null, NoteStore.decode(zeroId, requireFields = true))
        assertEquals(null, NoteStore.decode(oversizedTitle, requireFields = true))
    }

    @Test
    fun decodeRejectsOversizedBackupBeforeParsing() {
        val raw = " ".repeat(NoteStore.MAX_BACKUP_BYTES + 1)
        assertEquals(null, NoteStore.decode(raw, requireFields = true))
    }

    @Test
    fun saveRejectsDuplicateIdsWithoutReplacingExistingNotes() {
        val original = NestNote(id = 81, title = "Keep me", body = "Existing data")
        assertTrue(store.save(listOf(original)))
        val result = store.save(listOf(original, original.copy(title = "Duplicate")))
        assertFalse(result)
        assertEquals(listOf(original), store.load())
    }

    @Test
    fun malformedStoredJsonFallsBackToStarterNotes() {
        context.getSharedPreferences("nestnote_local", Context.MODE_PRIVATE)
            .edit().putString("notes", "{not valid json").commit()
        val notes = store.load()
        assertFalse(notes.isEmpty())
        assertTrue(notes.any { it.title == "Project Plan" })
    }
}
