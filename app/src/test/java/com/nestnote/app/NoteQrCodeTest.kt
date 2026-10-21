package com.nestnote.app

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class NoteQrCodeTest {
    @Test
    fun createsSquareQrBitmapWithBlackAndWhiteModules() {
        val bitmap = NoteQrCode.createBitmap("NESTNOTE QR test", size = 128)

        assertEquals(128, bitmap.width)
        assertEquals(128, bitmap.height)

        var blackPixels = 0
        var whitePixels = 0
        for (y in 0 until bitmap.height) {
            for (x in 0 until bitmap.width) {
                when (bitmap.getPixel(x, y)) {
                    Color.BLACK -> blackPixels++
                    Color.WHITE -> whitePixels++
                }
            }
        }
        assertTrue(blackPixels > 0)
        assertTrue(whitePixels > 0)
    }

    @Test
    fun sharePayloadReplacesVoiceAttachmentIdsWithReadableLabel() {
        val id = "12345678-1234-4234-8234-123456789abc"

        assertEquals("Title\n\nBefore [Voice note] after", NoteQrCode.sharePayload("Title", "Before [[voice:$id]] after"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsBlankPayload() {
        NoteQrCode.createBitmap("  ")
    }
}
