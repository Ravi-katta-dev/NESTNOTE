package com.nestnote.app

import java.io.IOException

/**
 * Fixed PCM contract for voice attachments. Recording and playback must stay aligned:
 * signed 16-bit samples, one channel, 16 kHz, little-endian PCM as produced by AudioRecord.
 */
internal object PcmPlaybackFormat {
    const val SAMPLE_RATE_HZ = 16_000
    const val CHANNEL_COUNT = 1
    const val BYTES_PER_SAMPLE = 2
    const val CHANNEL_OUT_MASK = 4 // AudioFormat.CHANNEL_OUT_MONO
    const val ENCODING_PCM_16BIT = 2 // AudioFormat.ENCODING_PCM_16BIT
    const val WRITE_CHUNK_BYTES = 8 * 1024

    init {
        check(WRITE_CHUNK_BYTES % BYTES_PER_SAMPLE == 0)
    }

    @Throws(IOException::class)
    fun validateAuthenticatedPcm(pcm: ByteArray) {
        if (pcm.isEmpty()) throw IOException("This recording is empty.")
        if (pcm.size % BYTES_PER_SAMPLE != 0) {
            throw IOException("This recording contains an incomplete PCM16 sample.")
        }
    }
}
