package com.nestnote.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


private const val VOICE_SAMPLE_RATE = 16_000
private const val MAX_WAV_CONTAINER_OVERHEAD_BYTES = 1024 * 1024

/**
 * Records mono PCM and pipes it directly into the repository's encrypted storage stream.
 * No plaintext recording file is written to disk. The store's existing 25 MiB limit applies.
 */

/**
 * Recording UI only. Playback and deletion of finished recordings live in
 * [InlineVoicePlayer], rendered inline in the note preview at each marker.
 *
 * Contract:
 *  - [cursorAtRecordStart] returns the body cursor captured the moment the user taps
 *    Record or Attach WAV. The parent uses it to insert a marker at that position.
 *  - [onRecordingAttached] fires once per completed attachment, passing the id and
 *    the cursor at record-start. The parent should insert `[[voice:id]]` at that cursor.
 *  - [addedThisSession] is owned by the editor; it removes unattached ids on save and all ids on discard.
 *    discards the whole editor session. All attachments created during this session
 *    are removed, any in-flight recording is discarded, and any pending review is
 *    deleted. Pre-existing attachments are never touched.
 */
@Composable
internal fun VoiceNoteControls(
    noteId: Long,
    cursorAtRecordStart: () -> Int,
    onRecordingAttached: (attachmentId: String, cursorAtRecordStart: Int) -> Unit,
    addedThisSession: MutableSet<String>
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val voiceNotes = remember(context) { com.nestnote.app.voicenote.VoiceNoteRepository(context) }
    val scope = rememberCoroutineScope()
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Recording state
    var recording by remember { mutableStateOf(false) }
    var startingRecording by remember { mutableStateOf(false) }
    var recordingPaused by remember { mutableStateOf(false) }
    var elapsedRecordingSeconds by remember { mutableIntStateOf(0) }
    var recordingSession by remember { mutableStateOf<EncryptedPcmRecording?>(null) }

    // Review state (finished recording, not yet attached)
    var pendingReviewId by remember(noteId) { mutableStateOf<String?>(null) }
    var pendingReviewName by remember(noteId) { mutableStateOf(defaultVoiceName()) }
    var pendingReviewDurationMs by remember(noteId) { mutableIntStateOf(0) }
    var pendingReviewCursor by remember(noteId) { mutableIntStateOf(0) }
    var confirmDiscardReview by remember { mutableStateOf(false) }

    // Review playback state
    var previewingReview by remember { mutableStateOf(false) }
    var reviewPaused by remember { mutableStateOf(false) }
    var reviewPositionMs by remember { mutableIntStateOf(0) }
    var reviewDurationMs by remember { mutableIntStateOf(0) }
    var seekingReview by remember { mutableStateOf(false) }
    var pendingReviewSeekMs by remember { mutableIntStateOf(0) }
    var reviewSession by remember { mutableStateOf<PcmVoicePlayback?>(null) }

    // Import state
    var importingAudio by remember { mutableStateOf(false) }

    // Permission state
    var pendingPermission by remember { mutableStateOf(false) }
    var showPermissionSettings by remember { mutableStateOf(false) }

    // Audio focus
    var requestedAudioFocus by remember { mutableStateOf(false) }
    var audioFocusListener by remember {
        mutableStateOf<AudioManager.OnAudioFocusChangeListener?>(null)
    }


    var status by remember { mutableStateOf("") }

    // --- WAV import ---
    val audioFileLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val cursor = cursorAtRecordStart()
        importingAudio = true
        status = "Importing audio securely…"
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val pcm = context.contentResolver.openInputStream(uri)
                        ?.use { decodePcmWav(readBoundedWavBytes(it)) }
                        ?: throw IOException("Could not open the selected audio file.")
                    if (pcm.isEmpty()) throw IOException("The selected audio file contains no audio.")
                    if (pcm.size > com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES) {
                        throw IOException("Audio exceeds the 25 MiB attachment limit.")
                    }
                    val id = UUID.randomUUID().toString()
                    val duration = pcm.size.toLong() * 1000L / (VOICE_SAMPLE_RATE * 2L)
                    voiceNotes.save(
                        noteId, id, ByteArrayInputStream(pcm), System.currentTimeMillis(),
                        duration, defaultVoiceName()
                    )
                    id
                }
            }
            importingAudio = false
            result.onSuccess { id ->
                addedThisSession.add(id)
                onRecordingAttached(id, cursor)
                status = "Audio attached at cursor position."
            }.onFailure {
                status = it.message ?: "Could not attach this audio file."
            }
        }
    }

    // --- Microphone permission ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        when (resolveVoicePermissionResult(granted, pendingPermission)) {
            VoicePermissionDecision.START_RECORDING -> {
                pendingPermission = false
                startingRecording = true
                showPermissionSettings = false
                val cursor = cursorAtRecordStart()
                startVoiceRecording(
                    context, audioManager, voiceNotes,
                    noteId, addedThisSession,
                    onStarted = { session, listener ->
                        startingRecording = false
                        recordingSession = session
                        recording = true
                        recordingPaused = false
                        elapsedRecordingSeconds = 0
                        status = "Recording…"
                        requestedAudioFocus = true
                        audioFocusListener = listener
                        pendingReviewCursor = cursor
                    },
                    onStatus = { msg ->
                        startingRecording = false
                        recordingSession = null
                        recording = false
                        recordingPaused = false
                        if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                        requestedAudioFocus = false
                        audioFocusListener = null
                        status = msg
                    },
                    onInterrupted = {
                        startingRecording = false
                        recordingSession?.stop()
                        recordingSession = null
                        recording = false
                        recordingPaused = false
                        if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                        requestedAudioFocus = false
                        audioFocusListener = null
                        status = "Recording stopped because audio focus was interrupted."
                    }
                )
            }
            VoicePermissionDecision.SHOW_PERMISSION_GUIDANCE -> {
                pendingPermission = false
                startingRecording = false
                showPermissionSettings = true
                status = "Microphone permission was denied. Allow it in app settings to record voice notes."
            }
            VoicePermissionDecision.IGNORE_STALE_RESULT -> Unit
        }
    }

    fun beginRecordingFlow() {
        if (recording || startingRecording || pendingPermission) return
        if (pendingReviewId != null || importingAudio) return
        keyboardController?.hide()
        startingRecording = true
        showPermissionSettings = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            == PackageManager.PERMISSION_GRANTED
        ) {
            val cursor = cursorAtRecordStart()
            startVoiceRecording(
                context, audioManager, voiceNotes,
                noteId, addedThisSession,
                onStarted = { session, listener ->
                    startingRecording = false
                    recordingSession = session
                    recording = true
                    recordingPaused = false
                    elapsedRecordingSeconds = 0
                    status = "Recording…"
                    requestedAudioFocus = true
                    audioFocusListener = listener
                    pendingReviewCursor = cursor
                },
                onStatus = { msg ->
                    startingRecording = false
                    recordingSession = null
                    recording = false
                    recordingPaused = false
                    if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                    requestedAudioFocus = false
                    audioFocusListener = null
                    status = msg
                },
                onInterrupted = {
                    startingRecording = false
                    recordingSession?.stop()
                    recordingSession = null
                    recording = false
                    recordingPaused = false
                    if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                    requestedAudioFocus = false
                    audioFocusListener = null
                    status = "Recording stopped because audio focus was interrupted."
                }
            )
        } else {
            pendingPermission = true
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    fun stopRecording() {
        startingRecording = false
        recordingSession?.stop()
        recording = false
        recordingPaused = false
        if (requestedAudioFocus) {
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            requestedAudioFocus = false
        }
        audioFocusListener = null
        status = "Finishing encrypted recording…"
    }

    fun stopReviewPlayback() {
        reviewSession?.stop()
        reviewSession = null
        previewingReview = false
        reviewPaused = false
        reviewPositionMs = 0
        reviewDurationMs = 0
        if (requestedAudioFocus) {
            abandonVoiceAudioFocus(audioManager, audioFocusListener)
            requestedAudioFocus = false
        }
        audioFocusListener = null
    }

    // Recording timer
    LaunchedEffect(recording, recordingPaused) {
        if (recording && !recordingPaused) {
            while (recording && !recordingPaused) {
                delay(1_000)
                if (recording && !recordingPaused) elapsedRecordingSeconds += 1
            }
        }
    }

    // Handle recorder completion -> transition to review
    LaunchedEffect(status) {
        if (status.startsWith("Recording ready for review:")) {
            val id = status.substringAfter("Recording ready for review:").trim()
            val payload = runCatching {
                voiceNotes.listPayloads(noteId).firstOrNull { it.attachmentId == id }
            }.getOrNull()
            recording = false
            recordingPaused = false
            recordingSession = null
            requestedAudioFocus = false
            audioFocusListener = null
            if (payload != null) {
                pendingReviewId = id
                pendingReviewName = defaultVoiceName()
                pendingReviewDurationMs = (
                    ((payload.encryptedSizeBytes - 32L).coerceAtLeast(0L) * 1000L) /
                        (VOICE_SAMPLE_RATE * 2L)
                ).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                status = "Recording ready. Preview and attach, or discard."
            } else {
                status = "The recording could not be loaded for review."
            }
        }
    }

    // Background / dispose safety net
    DisposableEffect(lifecycleOwner, noteId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                pendingPermission = false
                startingRecording = false
                recordingSession?.stop()
                recording = false
                recordingPaused = false
                if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
                requestedAudioFocus = false
                audioFocusListener = null
                reviewSession?.stop()
                reviewSession = null
                previewingReview = false
                reviewPaused = false
                if (status.startsWith("Recording…") || status.startsWith("Recording paused")) {
                    status = "Recording stopped when the app left the foreground."
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            recordingSession?.stop()
            reviewSession?.stop()
            if (requestedAudioFocus) abandonVoiceAudioFocus(audioManager, audioFocusListener)
            requestedAudioFocus = false
            audioFocusListener = null
        }
    }

    val reducedMotion = rememberSystemReducedMotion()

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // ---------- Recording pill (in progress) ----------
        if (recording || startingRecording || pendingPermission) {
            VoiceRecordingStatusPill(
                recording = recording, starting = startingRecording,
                permissionPending = pendingPermission, paused = recordingPaused,
                elapsedSeconds = elapsedRecordingSeconds, reducedMotion = reducedMotion,
                onTogglePause = {
                    if (recordingPaused) {
                        runCatching { recordingSession?.resume() }
                            .onSuccess { recordingPaused = false; status = "Recording…" }
                            .onFailure { stopRecording(); status = it.message ?: "Couldn't resume recording" }
                    } else {
                        recordingSession?.pause()
                        recordingPaused = true
                        status = "Recording paused."
                    }
                },
                onStop = { stopRecording() }
            )
        } else if (pendingReviewId == null) {
            VoiceRecordingIdleActions(
                importing = importingAudio,
                onRecord = { beginRecordingFlow() },
                onAttachWav = {
                    keyboardController?.hide()
                    audioFileLauncher.launch(arrayOf("audio/wav", "audio/x-wav", "audio/wave", "audio/vnd.wave"))
                }
            )
        }

        // ---------- Review card ----------
        if (pendingReviewId != null) {
            val reviewId = pendingReviewId!!
            val reviewAttachment = remember(reviewId) {
                runCatching { voiceNotes.listPayloads(noteId).firstOrNull { it.attachmentId == reviewId } }.getOrNull()
            }
            VoiceRecordingReviewCard(
                hasAttachment = reviewAttachment != null,
                name = pendingReviewName,
                onNameChange = { pendingReviewName = it.take(160) },
                durationMs = if (reviewDurationMs > 0) reviewDurationMs else pendingReviewDurationMs,
                positionMs = reviewPositionMs, seekPositionMs = pendingReviewSeekMs,
                seeking = seekingReview, previewing = previewingReview, paused = reviewPaused,
                onSeekChange = { pendingReviewSeekMs = it; seekingReview = true },
                onSeekFinished = {
                    val target = pendingReviewSeekMs.coerceIn(0, (if (reviewDurationMs > 0) reviewDurationMs else pendingReviewDurationMs))
                    reviewPositionMs = target
                    reviewSession?.seekTo(target.toLong())
                    seekingReview = false
                },
                onTogglePreview = {
                    if (previewingReview) {
                        if (reviewPaused) { reviewSession?.resume(); reviewPaused = false; status = "Preview resumed." }
                        else { reviewSession?.pause(); reviewPaused = true; status = "Preview paused." }
                    } else if (reviewAttachment != null) {
                        stopReviewPlayback()
                        val focus = requestVoiceAudioFocus(audioManager) {
                            stopReviewPlayback()
                            status = "Preview stopped because audio focus was interrupted."
                        }
                        if (focus == null) status = "Audio focus is unavailable. Try again."
                        else {
                            requestedAudioFocus = true; audioFocusListener = focus
                            reviewPositionMs = 0; reviewDurationMs = 0
                            val player = PcmVoicePlayback(
                                context, readAttachment = voiceNotes::read, attachment = reviewAttachment,
                                playbackSpeed = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(PREF_VOICE_PLAYER_SPEED, 1f),
                                onProgress = { positionMs, durationMs ->
                                    if (!seekingReview) {
                                        reviewPositionMs = positionMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                        pendingReviewSeekMs = reviewPositionMs
                                    }
                                    reviewDurationMs = durationMs.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
                                }
                            ) { error ->
                                abandonVoiceAudioFocus(audioManager, focus)
                                audioFocusListener = null; requestedAudioFocus = false
                                status = error ?: "Preview finished."
                            }
                            reviewSession = player
                            runCatching { player.start() }.onFailure {
                                status = it.message ?: "Couldn't preview recording."
                                stopReviewPlayback()
                            }
                        }
                    }
                },
                onAttach = {
                    runCatching {
                        voiceNotes.attachExisting(
                            noteId = noteId,
                            attachmentId = reviewId,
                            createdAtMillis = System.currentTimeMillis(),
                            durationMillis = pendingReviewDurationMs.toLong(),
                            displayName = pendingReviewName.trim()
                        )
                    }.onSuccess {
                        stopReviewPlayback(); pendingReviewId = null
                        addedThisSession.add(reviewId)
                        onRecordingAttached(reviewId, pendingReviewCursor)
                        status = "Recording attached at cursor position."
                    }.onFailure { status = it.message ?: "Couldn't attach reviewed recording." }
                },
                onDiscard = { confirmDiscardReview = true }
            )
        }

        // ---------- Status ----------
        if (status.isNotBlank()) {
            Text(
                status,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        VoicePermissionSettingsActions(
            visible = showPermissionSettings,
            onRetry = { beginRecordingFlow() },
            onDismiss = { showPermissionSettings = false },
            onStatus = { status = it }
        )
    }

    VoiceRecordingDiscardDialog(
        visible = confirmDiscardReview && pendingReviewId != null,
        onDismiss = { confirmDiscardReview = false },
        onConfirm = {
            confirmDiscardReview = false
            val id = pendingReviewId
            stopReviewPlayback()
            if (id != null) {
                addedThisSession.remove(id)
                runCatching { voiceNotes.delete(noteId, id) }
            }
            pendingReviewId = null
            pendingReviewName = defaultVoiceName()
            pendingReviewDurationMs = 0
            pendingReviewCursor = 0
            status = "Recording discarded. You can record again at any time."
        }
    )
}

/**
 * Reads an imported WAV with a hard container-size limit before decoding. The decoded PCM
 * is checked against the repository limit separately because WAV headers/chunks add overhead.
 */
private fun readBoundedWavBytes(input: java.io.InputStream): ByteArray {
    val maxBytes = com.nestnote.app.voicenote.VoiceNoteRepository.MAX_RECORDING_BYTES.toLong() +
        MAX_WAV_CONTAINER_OVERHEAD_BYTES
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0L
    while (true) {
        val count = input.read(buffer)
        if (count < 0) break
        if (count == 0) {
            val single = input.read()
            if (single < 0) break
            if (++total > maxBytes) throw IOException("WAV file exceeds the supported import size.")
            output.write(single)
            continue
        }
        total += count
        if (total > maxBytes) throw IOException("WAV file exceeds the supported import size.")
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}

/** e.g. "Voice note · Oct 20, 3:42 PM". Editable in the review step and stored in the manifest. */
internal fun defaultVoiceName(): String =
    "Voice note \u00b7 " + java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date())