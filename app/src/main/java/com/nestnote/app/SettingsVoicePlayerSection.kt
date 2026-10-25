package com.nestnote.app

import android.content.Context
import android.graphics.Typeface as AndroidTypeface
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun SettingsVoicePlayerSection(
    reducedMotion: Boolean,
    onOpenVoicePlayerSettings: () -> Unit
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val playerPrefs = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var compactVoicePlayer by remember {
        mutableStateOf(playerPrefs.getBoolean(PREF_VOICE_PLAYER_COMPACT, true))
    }
                    SettingsSection(
                        title = "Voice player",
                        subtitle = "Playback controls, waveform and appearance",
                        icon = Icons.Default.Mic
                    ) {
                        SettingLabel("Player style")
    
                        // Interactive style selector with live graphics
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            VoicePlayerStyleOption(
                                label = "Compact",
                                description = "Minimal circular pill with waveform dots",
                                isCompact = true,
                                selected = compactVoicePlayer,
                                onClick = {
                                    compactVoicePlayer = true
                                    playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, true).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.COMPACT.name).apply()
                                },
                                modifier = Modifier.weight(1f),
                                reducedMotion = reducedMotion
                            )
    
                            VoicePlayerStyleOption(
                                label = "Full",
                                description = "Expanded player with scrubber and seek buttons",
                                isCompact = false,
                                selected = !compactVoicePlayer,
                                onClick = {
                                    compactVoicePlayer = false
                                    playerPrefs.edit().putBoolean(PREF_VOICE_PLAYER_COMPACT, false).putString(PREF_VOICE_PLAYER_STYLE, VoicePlayerStyle.FULL.name).apply()
                                },
                                modifier = Modifier.weight(1f),
                                reducedMotion = reducedMotion
                            )
                        }
    
                        // Interactive Live Waveform Preview Card
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenVoicePlayerSettings() }
                                .testTag("settings_voice_player_open"),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceContainerLow,
                            border = BorderStroke(1.dp, colors.outlineVariant)
                        ) {
                            Column(
                                Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = colors.primaryContainer,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Default.VolumeUp,
                                                contentDescription = null,
                                                tint = colors.onPrimaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Column(Modifier.weight(1f)) {
                                        Text(
                                            "Interactive waveform preview",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            if (compactVoicePlayer) "Compact mode active" else "Full player active",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colors.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ChevronRight,
                                        contentDescription = "Open voice player settings",
                                        tint = colors.onSurfaceVariant
                                    )
                                }
    
                                // Dynamic animated waveform bars
                                AnimatedWaveformPreview(
                                    active = true,
                                    reducedMotion = reducedMotion,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                )
                            }
                        }
                    }
}
