package com.nestnote.app

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the native SQLCipher load/open/write/read path on an Android runtime. */
@RunWith(AndroidJUnit4::class)
class SqlCipherNativeSmokeTest {
    @Test
    fun nativeLibraryLoadsAndEncryptedStoreRoundTripsNote() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = SqlCipherNoteStore(context)
        val expected = listOf(
            NestNote(
                id = 8675309L,
                title = "SQLCipher native smoke test",
                body = "Verify native init and encrypted database access",
                updated = 1_798_000_000_000L
            )
        )

        assertTrue("SQLCipher encrypted save should succeed", store.save(expected))
        val loaded = store.loadSafely()

        assertFalse("SQLCipher store must not report recovery for a valid round trip", loaded.recoveryRequired)
        assertEquals(expected, loaded.notes)
    }
}
