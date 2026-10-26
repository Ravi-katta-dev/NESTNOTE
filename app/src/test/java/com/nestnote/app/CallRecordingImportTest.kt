package com.nestnote.app

import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CallRecordingImportTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    @Before
    fun resetPreferences() {
        context.getSharedPreferences(CallRecordingImport.PREFS, 0).edit().clear().commit()
    }

    @Test
    fun automaticImportIsDisabledByDefault() {
        assertFalse(CallRecordingImport.enabled(context))
        assertEquals(CallRecordingImport.STATUS_DISABLED, CallRecordingImport.status(context))
    }

    @Test
    fun importedRecordingMetadataSurvivesReload() {
        val expected = listOf(
            CallRecordingImport.Item(
                attachmentId = "0123456789abcdef0123456789abcdef",
                displayName = "Call recording.m4a",
                importedAtMillis = 1_800_000_000_000L,
                durationMillis = 72_000L,
                sourceUri = "content://provider/tree/recording-1"
            )
        )

        CallRecordingImport.saveItems(context, expected)

        assertEquals(expected, CallRecordingImport.items(context))
    }

    @Test
    fun malformedSavedSourceUriDoesNotCrashSettings() {
        context.getSharedPreferences(CallRecordingImport.PREFS, 0).edit()
            .putString(CallRecordingImport.KEY_TREE_URI, "not a uri")
            .commit()

        val parsed = CallRecordingImport.sourceTree(context)
        assertEquals(Uri.parse("not a uri"), parsed)
    }
}
