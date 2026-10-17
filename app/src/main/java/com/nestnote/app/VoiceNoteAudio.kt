package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.crypto.Cipher

private const val VOICE_SAMPLE_RATE = 16_000
private const val VOICE_CHANNEL_IN = android.media.AudioFormat.CHANNEL_IN_MONO
private const val VOICE_CHANNEL_OUT = android.media.AudioFormat.CHANNEL_OUT_MONO
private const val VOICE_ENCODING = android.media.AudioFormat.ENCODING_PCM_16BIT

/**
 * Records mono PCM and pipes it directly into VoiceAttachmentStore's encrypting stream.
 * No plaintext recording file is written to disk. The store's existing 25 MiB limit applies.
 */
private class EncryptedPcmRecording(
    private val context: Context,
    private val store: VoiceAttachmentStore,
    private val noteId: Long,
    private val attachmentId: String,
    private val onFinished: (Result<VoiceAttachment>) -> Unit
) {
    private val running = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val audioBytes = AtomicLong(0)
    private val limitExceeded = AtomicBoolean(false)
    private var audioRecord: AudioRecord? = null
    private var pipeOutput: PipedOutputStream? = null
    private var recorderThread: Thread? = null
    private var storageThread: Thread? = null

    fun start() {
        val minBuffer = AudioRecord.getMinBufferSize(VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN, VOICE_ENCODING)
        if (minBuffer <= 0) throw IOException("Microphone recording is unavailable on this device")
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC, VOICE_SAMPLE_RATE, VOICE_CHANNEL_IN,
            VOICE_ENCODING, maxOf(minBuffer * 2, 8192)
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            throw IOException("Could not initialize the microphone")
        }
        val input = PipedInputStream(64 * 1024)
        val output = PipedOutputStream(input)
        audioRecord = record
        pipeOutput = output
        running.set(true)

        storageThread = Thread({
            val result = runCatching {
                store.save(noteId, attachmentId, input).also {
                    if (audioBytes.get() == 0L || limitExceeded.get()) {
                        store.delete(noteId, attachmentId)
                        throw IOException(
                            if (limitExceeded.get()) "Recording reached the 25 MiB limit and was discarded."
                            else "No audio was captured. Try recording again."
                        )
                    }
                }
            }
            runCatching { input.close() }
            Handler(Looper.getMainLooper()).post { onFinished(result) }
        }, "nestnote-voice-encrypt").apply { start() }

        recorderThread = Thread({
            val buffer = ByteArray(maxOf(minBuffer, 4096))
            try {
                record.startRecording()
                while (running.get()) {
                    if (paused.get()) {
                        Thread.sleep(30)
                        continue
                    }
                    val count = record.read(buffer, 0, buffer.size)
                    if (count > 0) {
                        val total = audioBytes.addAndGet(count.toLong())
                        if (total > VoiceAttachmentStore.MAX_AUDIO_BYTES) {
                            limitExceeded.set(true)
                            running.set(false)
                            break
                        }
                        output.write(buffer, 0, count)
                    } else if (count < 0 && !paused.get() && running.get()) {
                        throw IOException("Microphone read failed ($count)")
                    }
                }
            } catch (_: IllegalStateException) {
                // stop() can race a blocked read; normal shutdown closes the pipe below.
            } catch (error: Exception) {
                running.set(false)
            } finally {
                runCatching { record.stop() }
                record.release()
                audioRecord = null
                runCatching { output.close() }
            }
        }, "nestnote-voice-record").apply { start() }
    }

    fun pause() {
        if (running.get() && paused.compareAndSet(false, true)) {
            runCatching { audioRecord?.stop() }
        }
    }

    fun resume() {
        if (running.get() && paused.compareAndSet(true, false)) {
            try {
                audioRecord?.startRecording()
            } catch (error: Exception) {
                running.set(false)
                runCatching { pipeOutput?.close() }
                throw IOException("Couldn't resume microphone recording", error)
            }
        }
    }

    fun stop() {
        running.set(false)
        runCatching { audioRecord?.stop() }
        runCatching { pipeOutput?.close() }
    }

    fun stopAndWait() {
        stop()
        runCatching { recorderThread?.join(2_000) }
        runCatching { storageThread?.join(5_000) }
    }
}

private class PcmVoicePlayback(
    private val context: Context,
    private val store: VoiceAttachmentStore,
    private val attachment: VoiceAttachment,
    private val onFinished: (String?) -> Unit
) {
    private val running = AtomicBoolean(true)
    @Volatile private var track: AudioTrack? = null
    private var thread: Thread? = null

    fun start() {
        thread = Thread({
            var errorMessage: String? = null
            try {
                // read() returns only after the attachment's AES-GCM tag authenticates.
                val pcm = store.read(attachment.noteId, attachment.attachmentId)
                val minBuffer = AudioTrack.getMinBufferSize(VOICE_SAMPLE_RATE, VOICE_CHANNEL_OUT, VOICE_ENCODING)
                if (minBuffer <= 0) throw IOException("Audio playback is unavailable on this device")
                val player = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(VOICE_SAMPLE_RATE)
                            .setChannelMask(VOICE_CHANNEL_OUT)
                            .setEncoding(VOICE_ENCODING)
                            .build()
                    )
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(minBuffer, 8192))
                    .build()
                track = player
                player.play()
                var offset = 0
                while (running.get() && offset < pcm.size) {
                    val written = player.write(pcm, offset, minOf(pcm.size - offset, 8192))
                    if (written < 0) throw IOException("Audio playback failed ($written)")
                    if (written == 0) continue
                    offset += written
                }
                player.stop()
            } catch (error: Exception) {
                errorMessage = error.message ?: "Couldn't play this recording"
            } finally {
                track?.release()
                track = null
                Handler(Looper.getMainLooper()).post { onFinished(errorMessage) }
            }
        }, "nestnote-voice-play").apply { start() }
    }

    fun stop() {
        running.set(false)
        runCatching { track?.pause() }
        runCatching { track?.flush() }
        runCatching { track?.stop() }
        thread?.interrupt()
    }
}

@Composable
internal fun VoiceNoteControls(
    noteId: Long,
    onAttachmentCountChanged: (Int) -> Unit,
    onVoiceAttachmentAdded: () -> Unit,
    onDiscardHandlerReady: (() -> Unit) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val attachmentStore = remember(context) { VoiceAttachmentStore(context) }
    val attachmentManifest = remember(context) { VoiceAttachmentManifest(context) }
    var attachments by remember(noteId) { mutableStateOf(runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())) }
    var recording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var playingId by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("") }
    var pendingPermission by remember { mutableStateOf(false) }
    var recordingSession by remember { mutableStateOf<EncryptedPcmRecording?>(null) }
    var playbackSession by remember { mutableStateOf<PcmVoicePlayback?>(null) }
    var requestedAudioFocus by remember { mutableStateOf(false) }
    var audioFocusListener by remember { mutableStateOf<AudioManager.OnAudioFocusChangeListener?>(null) }
    val addedThisSession = remember(noteId) { mutableSetOf<String>() }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted && pendingPermission) {
            pendingPermission = false
            startVoiceRecording(context, audioManager, attachmentStore, attachmentManifest, noteId, addedThisSession,
                { session, listener -> recordingSession = session; recording = true; recordingPaused = false; status = "Recording…"; requestedAudioFocus = true; audioFocusListener = listener; onVoiceAttachmentAdded() },
                { message -> recordingSession = null; recording = false; recordingPaused = false; requestedAudioFocus = false; audioFocusListener = null; status = message },
                { recordingSession?.stop(); recordingSession = null; recording = false; recordingPaused = false; requestedAudioFocus = false; audioFocusListener = null; status = "Recording stopped because audio focus was interrupted." })
        } else {
            pendingPermission = false
            status = "Microphone permission is required to record voice notes."
        }
    }

    fun stopRecording() {
        recordingSession?.stop()
        recording = false
        recordingPaused = false
        if (requestedAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusListener)
            requestedAudioFocus = false
        }
        status = "Finishing encrypted recording…"
    }
    fun stopPlayback() {
        playbackSession?.stop()
        playbackSession = null
        playingId = null
        if (requestedAudioFocus) {
            audioManager.abandonAudioFocus(audioFocusListener)
            requestedAudioFocus = false
        }
    }

    LaunchedEffect(noteId) {
        attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
        onAttachmentCountChanged(attachments.size)
    }
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                recordingSession?.stop()
                recording = false
                recordingPaused = false
                playbackSession?.stop()
                playbackSession = null
                playingId = null
                if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
                requestedAudioFocus = false
                status = "Recording/playback stopped when the app left the foreground."
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recordingSession?.stop()
            playbackSession?.stop()
            if (requestedAudioFocus) audioManager.abandonAudioFocus(audioFocusListener)
        }
    }

    LaunchedEffect(status) {
        if (status == "Recording saved securely." || status.startsWith("Recording encrypted, but its metadata")) {
            recording = false
            recordingPaused = false
            recordingSession = null
            requestedAudioFocus = false
            audioFocusListener = null
            attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
            onAttachmentCountChanged(attachments.size)
            onVoiceAttachmentAdded()
        }
    }

    androidx.compose.runtime.SideEffect {
        onDiscardHandlerReady {
            recordingSession?.stopAndWait()
            recordingSession = null
            recording = false
            recordingPaused = false
            addedThisSession.toList().forEach { id ->
                runCatching { attachmentStore.delete(noteId, id) }
                runCatching { attachmentManifest.remove(noteId, id) }
            }
            addedThisSession.clear()
            attachments = runCatching { attachmentStore.list(noteId) }.getOrDefault(emptyList())
            onAttachmentCountChanged(attachments.size)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Voice recordings", style = MaterialTheme.typography.titleMedium)
            Text(
                "Stored encrypted on this device. Voice audio is not included in note backups.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    enabled = !recording && playingId == null,
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startVoiceRecording(context, audioManager, attachmentStore, attachmentManifest, noteId, addedThisSession,
                                { session, listener -> recordingSession = session; recording = true; status = "Recording…"; requestedAudioFocus = true; audioFocusListener = listener },
                                { message -> status = message },
                                { recordingSession?.stop(); recordingSession = null; recording = false; requestedAudioFocus = false; audioFocusListener = null; status = "Recording stopped because audio focus was interrupted." })
                        } else {
                            pendingPermission = true
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    }
                ) {
                    Icon(Icons.Default.Mic, contentDescription = null)
                    Text("Record")
                }
                OutlinedButton(
                    enabled = recording,
                    onClick = {
                        if (recordingPaused) {
                            runCatching { recordingSession?.resume() }
                                .onSuccess { recordingPaused = false; status = "Recording…" }
                                .onFailure { stopRecording(); status = it.message ?: "Couldn't resume recording" }
                        } else {
                            recordingSession?.pause()
                            recordingPaused = true
                            status = "Recording paused."
                        }
                    }
                ) {
                    Icon(if (recordingPaused) Icons.Default.PlayArrow else Icons.Default.Pause, contentDescription = null)
                    Text(if (recordingPaused) "Resume" else "Pause")
                }
                OutlinedButton(enabled = recording, onClick = { stopRecording() }) {
                    Icon(Icons.Default.Stop, contentDescription = null)
                    Text("Stop")
                }
            }
            if (attachments.isEmpty()) {
                Text("No recordings yet", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                attachments.forEachIndexed { index, attachment ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Recording ${index + 1}", modifier = Modifier.weight(1f))
                        OutlinedButton(
                            enabled = !recording && (playingId == null || playingId == attachment.attachmentId),
                            onClick = {
                                if (playingId == attachment.attachmentId) {
                                    stopPlayback()
                                } else {
                                    stopPlayback()
                                    val focusListener = requestVoiceAudioFocus(audioManager) { stopPlayback(); status = "Playback stopped because audio focus was interrupted." }
                                    if (focusListener == null) {
                                        status = "Audio focus is unavailable. Try again."
                                    } else {
                                        requestedAudioFocus = true
                                        audioFocusListener = focusListener
                                        playingId = attachment.attachmentId
                                        val player = PcmVoicePlayback(context, attachmentStore, attachment) { error ->
                                            playingId = null
                                            playbackSession = null
                                            audioManager.abandonAudioFocus(focusListener)
                                            audioFocusListener = null
                                            requestedAudioFocus = false
                                            status = error ?: ""
                                        }
                                        playbackSession = player
                                        player.start()
                                    }
                                }
                            }
                        ) {
                            Icon(if (playingId == attachment.attachmentId) Icons.Default.Stop else Icons.Default.PlayArrow, contentDescription = null)
                            Text(if (playingId == attachment.attachmentId) "Stop" else "Play")
                        }
                    }
                }
            }
            if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun startVoiceRecording(
    context: Context,
    audioManager: AudioManager,
    store: VoiceAttachmentStore,
    manifest: VoiceAttachmentManifest,
    noteId: Long,
    addedIds: MutableSet<String>,
    onStarted: (EncryptedPcmRecording, AudioManager.OnAudioFocusChangeListener) -> Unit,
    onStatus: (String) -> Unit,
    onInterrupted: () -> Unit
) {
    val focusListener = requestVoiceAudioFocus(audioManager, onInterrupted)
    if (focusListener == null) {
        onStatus("Audio focus is unavailable. Stop other audio and try again.")
        return
    }
    val id = UUID.randomUUID().toString()
    val session = EncryptedPcmRecording(context, store, noteId, id) { result ->
        audioManager.abandonAudioFocus(focusListener)
        when {
            result.isSuccess -> {
                val attachment = result.getOrThrow()
                addedIds.add(id)
                runCatching {
                    val plaintextBytes = (attachment.encryptedSizeBytes - 32L).coerceAtLeast(0L)
                    val durationMillis = plaintextBytes * 1000L / (VOICE_SAMPLE_RATE * 2L)
                    manifest.put(
                        VoiceAttachmentMetadata(
                            noteId = noteId,
                            attachmentId = id,
                            createdAtMillis = System.currentTimeMillis(),
                            durationMillis = durationMillis,
                            encryptedSizeBytes = attachment.encryptedSizeBytes,
                            displayName = "Voice recording"
                        ),
                        store
                    )
                }.onSuccess {
                    onStatus("Recording saved securely.")
                }.onFailure {
                    onStatus("Recording encrypted, but its metadata could not be saved. Keep this note and try again.")
                }
            }
            else -> onStatus(result.exceptionOrNull()?.message ?: "Couldn't save recording")
        }
    }
    try {
        session.start()
        onStarted(session, focusListener)
    } catch (error: Exception) {
        session.stop()
        audioManager.abandonAudioFocus(focusListener)
        onStatus(error.message ?: "Couldn't start recording")
    }
}

@Suppress("DEPRECATION")
private fun requestVoiceAudioFocus(
    audioManager: AudioManager,
    onInterrupted: () -> Unit
): AudioManager.OnAudioFocusChangeListener? {
    val listener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change <= AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) onInterrupted()
    }
    return if (audioManager.requestAudioFocus(
        listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
    ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) listener else null
}
