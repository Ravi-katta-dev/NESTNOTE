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
internal fun SettingsAppearanceSection(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    reducedMotion: Boolean,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    amoled: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    accentPalette: AccentPalette,
    onAccentPaletteChange: (AccentPalette) -> Unit,
    noteColorStyle: NoteColorStyle,
    onNoteColorStyleChange: (NoteColorStyle) -> Unit
) {
    val colors = MaterialTheme.colorScheme
                    SettingsSection(
                        title = "Appearance",
                        subtitle = "Colors and themes that match your style",
                        icon = Icons.Default.Palette
                    ) {
                        SettingLabel("Theme")
    
                        // Visual theme graphics cards
                        ThemeModeSelector(
                            selected = themeMode,
                            onSelect = onThemeChange,
                            reducedMotion = reducedMotion
                        )
    
                        if (themeMode == ThemeMode.SYSTEM) {
                            Text(
                                "Follows your Android light/dark setting",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                                modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                            )
                        }
    
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
                        SwitchRow(
                            title = "Wallpaper colors",
                            summary = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                "Use Android dynamic colors from your wallpaper"
                            } else {
                                "Requires Android 12 or newer; older devices use the app palette"
                            },
                            checked = dynamicColors,
                            onChange = onDynamicColorsChange
                        )
    
                        SwitchRow(
                            title = "Pure black in dark mode",
                            summary = "Saves battery on OLED screens. Applies when the theme is dark.",
                            checked = amoled,
                            onChange = onAmoledChange
                        )
    
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
                        SettingLabel("App accent colour")
                        Text(
                            "Choose the colour used for buttons, highlights and selected controls.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            AccentPalette.values().toList().chunked(4).forEach { paletteRow ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    paletteRow.forEach { palette ->
                                        val selectedPalette = accentPalette == palette
                                        val swatch = if (themeMode == ThemeMode.DARK ||
                                            (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                                        ) palette.primaryDark else palette.primaryLight
                                        Surface(
                                            modifier = Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                                .clickable { onAccentPaletteChange(palette) }
                                                .semantics { contentDescription = "${palette.label} accent colour${if (selectedPalette) ", selected" else ""}" },
                                            color = if (selectedPalette) colors.secondaryContainer else colors.surfaceVariant,
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(
                                                if (selectedPalette) 2.dp else 1.dp,
                                                if (selectedPalette) colors.primary else colors.outlineVariant
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(swatch),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (selectedPalette) {
                                                        Icon(Icons.Default.Check, contentDescription = null, tint = if (swatch.luminance() > 0.45f) Color.Black else Color.White, modifier = Modifier.size(17.dp))
                                                    }
                                                }
                                                Text(palette.label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
                                            }
                                        }
                                    }
                                    repeat((4 - paletteRow.size).coerceAtLeast(0)) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                        if (accentPalette != AccentPalette.DEFAULT && dynamicColors) {
                            Text(
                                "Selecting a custom accent disables wallpaper colours so your palette stays consistent.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
    
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
                        SettingLabel("Note card colors")
                        Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            NoteColorStyleCard(
                                title = NoteColorStyle.PASTEL.label,
                                description = NoteColorStyle.PASTEL.description,
                                selected = noteColorStyle == NoteColorStyle.PASTEL,
                                onClick = { onNoteColorStyleChange(NoteColorStyle.PASTEL) },
                                isPastel = true,
                                reducedMotion = reducedMotion
                            )
                            NoteColorStyleCard(
                                title = NoteColorStyle.ADAPTIVE.label,
                                description = NoteColorStyle.ADAPTIVE.description,
                                selected = noteColorStyle == NoteColorStyle.ADAPTIVE,
                                onClick = { onNoteColorStyleChange(NoteColorStyle.ADAPTIVE) },
                                isPastel = false,
                                reducedMotion = reducedMotion
                            )
                        }
    
                        HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
    
                        SettingLabel("Custom note wallpapers")
                        Text(
                            "Open any note and select the palette tool. In the Colour tab, tap + to choose an image from your device. The wallpaper is saved with that note; use the Background tab to return to an illustrated background.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.onSurfaceVariant
                        )
    
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = colors.secondaryContainer.copy(alpha = 0.7f),
                            border = BorderStroke(1.dp, colors.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = colors.onSecondaryContainer.copy(alpha = 0.12f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = colors.onSecondaryContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Personalize each note",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSecondaryContainer
                                    )
                                    Text(
                                        "Choose a color and background independently for each note.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colors.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
}
