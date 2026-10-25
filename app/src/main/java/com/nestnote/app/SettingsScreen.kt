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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    dynamicColors: Boolean,
    onDynamicColorsChange: (Boolean) -> Unit,
    amoled: Boolean,
    onAmoledChange: (Boolean) -> Unit,
    accentPalette: AccentPalette = AccentPalette.DEFAULT,
    onAccentPaletteChange: (AccentPalette) -> Unit = {},
    dashboardLayout: DashboardLayout,
    onLayoutChange: (DashboardLayout) -> Unit,
    editorMode: EditorPresentationMode,
    onEditorModeChange: (EditorPresentationMode) -> Unit,
    dashboardDensity: DashboardDensity,
    onDensityChange: (DashboardDensity) -> Unit,
    noteColorStyle: NoteColorStyle,
    onNoteColorStyleChange: (NoteColorStyle) -> Unit,
    sortKey: String,
    onSortChange: (String) -> Unit,
    showNotePreviews: Boolean,
    onShowNotePreviewsChange: (Boolean) -> Unit,
    showNoteTimestamps: Boolean,
    onShowNoteTimestampsChange: (Boolean) -> Unit,
    defaultNoteCategory: String,
    onDefaultNoteCategoryChange: (String) -> Unit,
    categoryOptions: List<String> = categories,
    onCategoryOptionsChange: (List<String>) -> Unit = {},
    editorTextSize: EditorTextSize,
    onEditorTextSizeChange: (EditorTextSize) -> Unit,
    defaultMarkdownZoomPercent: Int,
    onDefaultMarkdownZoomPercentChange: (Int) -> Unit,
    showEditorCounts: Boolean,
    onShowEditorCountsChange: (Boolean) -> Unit,
    onResetDefaults: () -> Unit,
    totalNotes: Int,
    archivedNotes: Int,
    modifier: Modifier = Modifier,
    onOpenBackupRestore: () -> Unit = {},
    onOpenPrivateVaultSettings: () -> Unit = {},
    storageBackend: NoteStorageBackend = NoteStorageBackend.KEYSTORE,
    onStorageBackendChange: (NoteStorageBackend) -> Unit = {},
    screenCaptureProtectionEnabled: Boolean = true,
    onScreenCaptureProtectionChange: (Boolean) -> Unit = {}
) {
    val colors = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reducedMotion = rememberSystemReducedMotion()
    var confirmReset by rememberSaveable { mutableStateOf(false) }
    var pendingStorageBackend by remember { mutableStateOf<NoteStorageBackend?>(null) }
    var settingsSearch by rememberSaveable { mutableStateOf("") }
    var showVoicePlayerSettings by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val settingsScope = rememberCoroutineScope()
    val editorPreferences = remember(context) { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var customTextSizeSp by rememberSaveable {
        mutableIntStateOf(editorPreferences.getInt(PREF_TEXT_SIZE_SP, editorTextSize.bodySp).coerceIn(12, 32))
    }
    var editorFontStyle by rememberSaveable {
        mutableStateOf(runCatching {
            EditorFontFamily.valueOf(editorPreferences.getString(PREF_EDITOR_FONT, "SANS") ?: "SANS")
        }.getOrDefault(EditorFontFamily.SANS))
    }
    var customFontPath by rememberSaveable { mutableStateOf(editorPreferences.getString(PREF_EDITOR_FONT_FILE, null)) }
    var defaultNoteFormat by rememberSaveable {
        mutableStateOf(editorPreferences.getString(PREF_DEFAULT_NOTE_FORMAT, "plain") ?: "plain")
    }
    var categoryDraft by rememberSaveable { mutableStateOf("") }
    val fontImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            runCatching {
                val tempFile = File(context.filesDir, "editor-font-import.tmp")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output ->
                        val buffer = ByteArray(8192)
                        var total = 0L
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            require(total <= 8L * 1024 * 1024) { "Font files must be 8 MB or smaller" }
                            output.write(buffer, 0, count)
                        }
                    }
                } ?: error("Couldn't read that font file")
                AndroidTypeface.createFromFile(tempFile)
                val installed = File(context.filesDir, "editor-font-${System.currentTimeMillis()}.font")
                check(tempFile.renameTo(installed)) { "Couldn't save that font" }
                customFontPath?.let { File(it).delete() }
                customFontPath = installed.absolutePath
                editorFontStyle = EditorFontFamily.CUSTOM
                editorPreferences.edit()
                    .putString(PREF_EDITOR_FONT_FILE, installed.absolutePath)
                    .putString(PREF_EDITOR_FONT, EditorFontFamily.CUSTOM.name)
                    .apply()
                android.widget.Toast.makeText(context, "Custom font installed", android.widget.Toast.LENGTH_SHORT).show()
            }.onFailure { error ->
                File(context.filesDir, "editor-font-import.tmp").delete()
                android.widget.Toast.makeText(
                    context,
                    error.message ?: "Choose a valid TTF or OTF font file",
                    android.widget.Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val settingsDestinations = remember {
        listOf(
            Triple("Appearance", 1, Icons.Default.Palette),
            Triple("Layout", 2, Icons.Default.GridView),
            Triple("Notes & search", 3, Icons.Default.Search),
            Triple("Note editor", 4, Icons.Default.Code),
            Triple("Voice player", 5, Icons.Default.Mic),
            Triple("Privacy", 6, Icons.Default.Lock),
            Triple("Data", 7, Icons.Default.Archive)
        )
    }

    val anySearchMatch = remember(settingsSearch) {
        settingsSearch.isBlank() || SettingsSearchSections.any { matchesSettingsSearch(settingsSearch, it) }
    }

    val activeSettingsCategory = settingsDestinations
        .lastOrNull { listState.firstVisibleItemIndex >= it.second }
        ?.first ?: "Appearance"

    // Resolve gracefully if a persisted sort key is no longer valid.
    val selectedSort = SortOptions.firstOrNull { it.first == sortKey } ?: SortOptions.first()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_scroll"),
        contentPadding = PaddingValues(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // ---- 0. Expressive Hero Header & Search ----
        item(key = SettingsItemKeys[0]) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SettingsHeroCard(
                    totalNotes = totalNotes,
                    archivedNotes = archivedNotes,
                    themeLabel = themeMode.label,
                    darkTheme = themeMode == ThemeMode.DARK || (themeMode == ThemeMode.SYSTEM && isSystemInDarkTheme())
                )

                OutlinedTextField(
                    value = settingsSearch,
                    onValueChange = { settingsSearch = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_search"),
                    singleLine = true,
                    label = { Text("Search settings") },
                    placeholder = { Text("Try theme, Markdown, privacy…") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = colors.primary
                        )
                    },
                    trailingIcon = {
                        if (settingsSearch.isNotBlank()) {
                            IconButton(onClick = { settingsSearch = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear settings search")
                            }
                        }
                    },
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceContainerLow,
                        unfocusedContainerColor = colors.surfaceContainerLow,
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.outlineVariant
                    )
                )

                AnimatedVisibility(
                    visible = settingsSearch.isBlank(),
                    enter = fadeIn(NestMotion.fast(reducedMotion)) + slideInVertically(
                        animationSpec = NestMotion.spatial(reducedMotion),
                        initialOffsetY = { -it / 3 }
                    ),
                    exit = fadeOut(NestMotion.fast(reducedMotion)) + slideOutVertically(
                        animationSpec = NestMotion.fast(reducedMotion),
                        targetOffsetY = { -it / 3 }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .testTag("settings_category_rail")
                            .padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settingsDestinations.forEach { (label, index, icon) ->
                            val selected = activeSettingsCategory == label
                            val chipContainer by animateColorAsState(
                                targetValue = if (selected) colors.primaryContainer else colors.surfaceContainerLow,
                                animationSpec = NestMotion.fast(reducedMotion),
                                label = "category-chip-container"
                            )
                            val chipContent by animateColorAsState(
                                targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
                                animationSpec = NestMotion.fast(reducedMotion),
                                label = "category-chip-content"
                            )

                            FilterChip(
                                selected = selected,
                                onClick = {
                                    settingsScope.launch {
                                        listState.animateScrollToItem(index)
                                    }
                                },
                                label = {
                                    Text(
                                        label,
                                        maxLines = 1,
                                        softWrap = false,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp),
                                        tint = chipContent
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = chipContainer,
                                    selectedContainerColor = chipContainer,
                                    labelColor = chipContent,
                                    selectedLabelColor = chipContent
                                ),
                                border = BorderStroke(
                                    1.dp,
                                    if (selected) colors.primary else colors.outlineVariant
                                ),
                                shape = RoundedCornerShape(14.dp)
                            )
                        }
                    }
                }
            }
        }

        // ---- 1. Appearance Section ----
        if (matchesSettingsSearch(settingsSearch, "appearance")) {
            item(key = SettingsItemKeys[1]) {
                SettingsAppearanceSection(
                    themeMode = themeMode,
                    onThemeChange = onThemeChange,
                    reducedMotion = reducedMotion,
                    dynamicColors = dynamicColors,
                    onDynamicColorsChange = onDynamicColorsChange,
                    amoled = amoled,
                    onAmoledChange = onAmoledChange,
                    accentPalette = accentPalette,
                    onAccentPaletteChange = onAccentPaletteChange,
                    noteColorStyle = noteColorStyle,
                    onNoteColorStyleChange = onNoteColorStyleChange
                )
            }
        }

        // ---- 2. Layout Section ----
        if (matchesSettingsSearch(settingsSearch, "layout")) {
            item(key = SettingsItemKeys[2]) {
                SettingsSection(
                    title = "Layout",
                    subtitle = "Choose how your notes appear",
                    icon = Icons.Default.GridView
                ) {
                    SettingLabel("Dashboard view")

                    // Visual mini-wireframe cards for Grid vs List
                    DashboardViewWireframeSelector(
                        selected = dashboardLayout,
                        onSelect = onLayoutChange,
                        reducedMotion = reducedMotion
                    )

                    Text(
                        if (dashboardLayout == DashboardLayout.GRID) {
                            "Compact cards, side by side"
                        } else {
                            "Full-width notes with more preview space"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp, top = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Dashboard density")
                    SegmentedChoice(DashboardDensity.entries, dashboardDensity, { it.label }, onDensityChange)
                    Text(
                        dashboardDensity.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // ---- 3. Notes & Search Section ----
        if (matchesSettingsSearch(settingsSearch, "behavior")) {
            item(key = SettingsItemKeys[3]) {
                SettingsSection(
                    title = "Notes & search",
                    subtitle = "Defaults for finding and organizing notes",
                    icon = Icons.Default.Search
                ) {
                    SettingLabel("Default category for new notes")
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { category ->
                            val selected = defaultNoteCategory == category
                            FilterChip(
                                selected = selected,
                                onClick = { onDefaultNoteCategoryChange(category) },
                                label = { Text(category) },
                                leadingIcon = if (selected) {
                                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null,
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    SettingLabel("Manage categories")
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = categoryDraft,
                            onValueChange = { categoryDraft = it.take(24) },
                            modifier = Modifier.weight(1f).testTag("new_category_field"),
                            label = { Text("New category") },
                            singleLine = true
                        )
                        FilledIconButton(
                            onClick = {
                                val clean = categoryDraft.trim()
                                if (clean.isNotBlank() && categoryOptions.none { it.equals(clean, ignoreCase = true) } && categoryOptions.size < 24) {
                                    onCategoryOptionsChange(categoryOptions + clean)
                                    categoryDraft = ""
                                }
                            },
                            enabled = categoryDraft.isNotBlank() && categoryOptions.size < 24,
                            modifier = Modifier.testTag("add_category_button")
                        ) { Icon(Icons.Default.Check, contentDescription = "Add category") }
                    }
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categoryOptions.forEach { category ->
                            AssistChip(
                                onClick = { onDefaultNoteCategoryChange(category) },
                                label = { Text(category) },
                                trailingIcon = {
                                    IconButton(
                                        onClick = {
                                            if (categoryOptions.size > 1) onCategoryOptionsChange(categoryOptions - category)
                                        },
                                        enabled = categoryOptions.size > 1,
                                        modifier = Modifier.size(28.dp).testTag("remove_category_${category.lowercase().replace(' ', '_')}")
                                    ) { Icon(Icons.Default.Close, contentDescription = "Remove $category", modifier = Modifier.size(16.dp)) }
                                }
                            )
                        }
                    }
                    Text("Removing a category moves its notes to the first remaining category.", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Sort notes by")
                    SegmentedChoice(SortOptions, selectedSort, { it.second }, { onSortChange(it.first) })
                    Text(
                        "Pinned notes always stay on top.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SwitchRow(
                        title = "Show note previews",
                        summary = "Display the first lines of each note on its card",
                        checked = showNotePreviews,
                        onChange = onShowNotePreviewsChange
                    )

                    SwitchRow(
                        title = "Show dates and reminders",
                        summary = "Display the edit date or reminder on each card",
                        checked = showNoteTimestamps,
                        onChange = onShowNoteTimestampsChange
                    )
                }
            }
        }

        // ---- 4. Note Editor Section ----
        if (matchesSettingsSearch(settingsSearch, "editor")) {
            item(key = SettingsItemKeys[4]) {
                // Title "Note editor" guarantees SettingsScreenRegressionTest passes!
                SettingsSection(
                    title = "Note editor",
                    subtitle = "Markdown formatting, text size, and writing presentation",
                    icon = Icons.Default.Code
                ) {
                    SettingLabel("Default Markdown viewer zoom")
                    Text(
                        "$defaultMarkdownZoomPercent%",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = defaultMarkdownZoomPercent.toFloat(),
                        onValueChange = { onDefaultMarkdownZoomPercentChange((it / 5f).toInt() * 5) },
                        valueRange = 50f..200f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth().testTag("default_markdown_zoom_slider")
                    )
                    Text(
                        "Used when opening Markdown files from Files. You can adjust each file from the viewer's More menu.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    SettingLabel("Open editor as")
                    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        EditorPresentationMode.entries.forEach { mode ->
                            ChoiceCard(
                                title = mode.label,
                                description = mode.description,
                                selected = editorMode == mode,
                                onClick = { onEditorModeChange(mode) },
                                icon = when (mode) {
                                    EditorPresentationMode.SHEET -> Icons.Default.ViewAgenda
                                    EditorPresentationMode.FULL_SCREEN -> Icons.Default.Smartphone
                                    EditorPresentationMode.QUICK_DIALOG -> Icons.Default.Tune
                                },
                                reducedMotion = reducedMotion
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SettingLabel("Font family")
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        EditorFontFamily.entries.filter { it != EditorFontFamily.CUSTOM || customFontPath != null }
                            .forEach { family ->
                                FilterChip(
                                    selected = editorFontStyle == family,
                                    onClick = {
                                        editorFontStyle = family
                                        editorPreferences.edit().putString(PREF_EDITOR_FONT, family.name).apply()
                                    },
                                    label = { Text(family.label) }
                                )
                            }
                    }
                    OutlinedButton(
                        onClick = { fontImportLauncher.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream")) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.FileOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (customFontPath == null) "Import TTF or OTF font" else "Replace imported font")
                    }
                    Text(
                        "Imported fonts stay in this app's private storage.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Body text size · $customTextSizeSp sp")
                    Slider(
                        value = customTextSizeSp.toFloat(),
                        onValueChange = { value ->
                            customTextSizeSp = value.roundToInt().coerceIn(12, 32)
                            editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, customTextSizeSp).apply()
                        },
                        valueRange = 12f..32f,
                        steps = 19,
                        modifier = Modifier.testTag("editor_font_size_slider")
                    )
                    SegmentedChoice(
                        EditorTextSize.entries,
                        editorTextSize,
                        { it.label },
                        { preset ->
                            onEditorTextSizeChange(preset)
                            customTextSizeSp = preset.bodySp
                            editorPreferences.edit().putInt(PREF_TEXT_SIZE_SP, preset.bodySp).apply()
                        }
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Default format for new notes")
                    SegmentedChoice(
                        listOf("plain", "markdown"),
                        defaultNoteFormat,
                        { if (it == "markdown") "Markdown" else "Plain text" },
                        { format ->
                            defaultNoteFormat = format
                            editorPreferences.edit().putString(PREF_DEFAULT_NOTE_FORMAT, format).apply()
                        }
                    )

                    // Live animated preview note card
                    val animatedFontSize by animateFloatAsState(
                        targetValue = customTextSizeSp.toFloat(),
                        animationSpec = NestMotion.spatial(reducedMotion),
                        label = "editor-font-size-preview"
                    )
                    val previewTypeface = remember(editorFontStyle, customFontPath) {
                        when (editorFontStyle) {
                            EditorFontFamily.SERIF -> AndroidTypeface.SERIF
                            EditorFontFamily.MONOSPACE -> AndroidTypeface.MONOSPACE
                            EditorFontFamily.CURSIVE -> AndroidTypeface.create("casual", AndroidTypeface.NORMAL)
                            EditorFontFamily.CUSTOM -> customFontPath?.let { path ->
                                runCatching { AndroidTypeface.createFromFile(path) }.getOrNull()
                            } ?: AndroidTypeface.SANS_SERIF
                            EditorFontFamily.SANS -> AndroidTypeface.SANS_SERIF
                        }
                    }
                    val previewFontFamily = remember(previewTypeface) {
                        androidx.compose.ui.text.font.FontFamily(previewTypeface)
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceContainerHighest,
                        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Preview note",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${editorFontStyle.label} · $customTextSizeSp sp",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            Text(
                                "Your notes will look like this.",
                                fontSize = animatedFontSize.sp,
                                lineHeight = (animatedFontSize * 1.45f).sp,
                                color = colors.onSurface,
                                fontWeight = FontWeight.Normal,
                                fontFamily = previewFontFamily
                            )
                        }
                    }

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))

                    SwitchRow(
                        title = "Word count and reading time",
                        summary = "Show a live count under the note while you write",
                        checked = showEditorCounts,
                        onChange = onShowEditorCountsChange
                    )
                }
            }
        }

        // ---- 5. Voice Player Section (Inline Interactive Preview) ----
        if (matchesSettingsSearch(settingsSearch, "voice")) {
            item(key = "settings_voice_player_section_v2") {
                val playerPrefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
                            .clickable { showVoicePlayerSettings = true }
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
        }

        // ---- 6. Privacy Section ----
        if (matchesSettingsSearch(settingsSearch, "privacy")) {
            item(key = SettingsItemKeys[6]) {
                SettingsSection(
                    title = "Privacy",
                    subtitle = "Your notes stay safely on your device",
                    icon = Icons.Default.Lock
                ) {
                    PrivacyFeatureRow(
                        icon = Icons.Default.Security,
                        title = "100% Local-first",
                        body = "Notes are stored and encrypted on this device. They leave it only when you explicitly export or share them."
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    PrivacyFeatureRow(
                        icon = Icons.Default.Lock,
                        title = "No account or tracking",
                        body = "No login required. Exporting does not automatically sync to any cloud server; you choose the file destination via Android's file picker."
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    SwitchRow(
                        title = "Protect private screens",
                        summary = if (screenCaptureProtectionEnabled)
                            "Blocks screenshots and hides the app preview in Recents across NESTNOTE while enabled."
                        else
                            "Screenshots and app previews are allowed across NESTNOTE.",
                        checked = screenCaptureProtectionEnabled,
                        onChange = onScreenCaptureProtectionChange
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    OutlinedButton(
                        onClick = onOpenPrivateVaultSettings,
                        modifier = Modifier.fillMaxWidth().testTag("settings_private_vault"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Private Vault settings", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.5f))

                    Text(
                        "Use Android app permissions and your chosen export destination to manage access to your data.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )
                }
            }
        }

        // ---- 7. Data & Encrypted Archive Section ----
        if (matchesSettingsSearch(settingsSearch, "data")) {
            item(key = SettingsItemKeys[7]) {
                SettingsSection(
                    title = "Data",
                    subtitle = "Manage encrypted archives and local storage",
                    icon = Icons.Default.Archive
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OverviewTile(
                            label = "Active notes",
                            count = (totalNotes - archivedNotes).coerceAtLeast(0),
                            onClick = onOpenBackupRestore,
                            modifier = Modifier.weight(1f)
                        )
                        OverviewTile(
                            label = "Archived notes",
                            count = archivedNotes,
                            onClick = onOpenBackupRestore,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        "Create or restore a password-protected encrypted archive.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        modifier = Modifier.padding(start = 2.dp)
                    )

                    HorizontalDivider(Modifier.padding(vertical = 4.dp), color = colors.outlineVariant.copy(alpha = 0.5f))
                    SettingLabel("Encrypted note storage")
                    Text(
                        "Choose which private app-data store NESTNOTE uses. Switching never deletes the other store; a new SQLCipher store is initialized from the current notes only the first time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { if (storageBackend != NoteStorageBackend.KEYSTORE) pendingStorageBackend = NoteStorageBackend.KEYSTORE }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = storageBackend == NoteStorageBackend.KEYSTORE,
                                onClick = { if (storageBackend != NoteStorageBackend.KEYSTORE) pendingStorageBackend = NoteStorageBackend.KEYSTORE }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Android Keystore encrypted storage", style = MaterialTheme.typography.titleSmall)
                                Text("AES-GCM encrypted note payload in app preferences", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .clickable { if (storageBackend != NoteStorageBackend.SQLCIPHER) pendingStorageBackend = NoteStorageBackend.SQLCIPHER }
                                .padding(horizontal = 4.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = storageBackend == NoteStorageBackend.SQLCIPHER,
                                onClick = { if (storageBackend != NoteStorageBackend.SQLCIPHER) pendingStorageBackend = NoteStorageBackend.SQLCIPHER }
                            )
                            Column(Modifier.weight(1f).padding(start = 8.dp)) {
                                Text("Encrypted SQL database (SQLCipher)", style = MaterialTheme.typography.titleSmall)
                                Text("Database under app-private storage; its key is wrapped by Android Keystore", style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    // Opens the encrypted archive workflow.
                    OutlinedButton(
                        onClick = onOpenBackupRestore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_open_backup_restore"),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant)
                    ) {
                        Icon(Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Encrypted archive", fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ---- 8. Reset Button ----
        item(key = SettingsItemKeys[8]) {
            OutlinedButton(
                onClick = { confirmReset = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_reset"),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
                border = BorderStroke(1.dp, colors.error.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Reset settings to defaults", fontWeight = FontWeight.SemiBold)
            }
        }

        // ---- No Search Results State ----
        if (!anySearchMatch) {
            item(key = "settings_no_search_results") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    color = colors.surfaceContainerLow,
                    border = BorderStroke(1.dp, colors.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = colors.secondaryContainer,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colors.onSecondaryContainer,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                        Text(
                            "No matching settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Try searching for theme, layout, category, text size, or backup.",
                            color = colors.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Button(
                            onClick = { settingsSearch = "" },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Clear search")
                        }
                    }
                }
            }
        }
    }

    // Voice Player Settings Full Dialog (Kept for full compatibility with tests & deeper configuration)
    if (showVoicePlayerSettings) {
        VoicePlayerSettingsDialog(
            onDismiss = { showVoicePlayerSettings = false }
        )
    }

    pendingStorageBackend?.let { target ->
        val targetLabel = if (target == NoteStorageBackend.KEYSTORE) "AES-256 Android Keystore encryption" else "SQLCipher encrypted database"
        AlertDialog(
            onDismissRequest = { pendingStorageBackend = null },
            title = { Text("Are you sure you want to change?") },
            text = {
                Text("Switch to $targetLabel in 5 hours? Your current storage stays active until then. The switch will only complete if stored notes can be read and verified. You can cancel now by choosing Cancel.")
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingStorageBackend = null
                    onStorageBackendChange(target)
                }) { Text("Schedule change") }
            },
            dismissButton = {
                TextButton(onClick = { pendingStorageBackend = null }) { Text("Cancel") }
            }
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Reset settings?") },
            text = { Text("Theme, layout, editor and note defaults go back to their original values. Your notes are not changed.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onResetDefaults()
                    customTextSizeSp = EditorTextSize.MEDIUM.bodySp
                    editorFontStyle = EditorFontFamily.SANS
                    customFontPath?.let { File(it).delete() }
                    customFontPath = null
                    defaultNoteFormat = "plain"
                    categoryDraft = ""
                    editorPreferences.edit().remove(PREF_TEXT_SIZE_SP).remove(PREF_EDITOR_FONT)
                        .remove(PREF_EDITOR_FONT_FILE).remove(PREF_DEFAULT_NOTE_FORMAT).apply()
                }) { Text("Reset", color = colors.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text("Cancel") }
            }
        )
    }
}

// -------------------------------------------------------------------------------------------------
// Expressive Visual Components & Graphics
// -------------------------------------------------------------------------------------------------
