package com.nestnote.app

/**
 * Stable item keys for the settings LazyColumn, in display order.
 *
 * Keep these keys stable: settings deep-links and programmatic scrolling depend on them.
 */
internal val SettingsItemKeys = listOf(
    "settings_header",
    "settings_appearance",
    "settings_layout",
    "settings_behavior",
    "settings_editor",
    "settings_voice_player_section_v2",
    "settings_privacy",
    "settings_backup",
    "settings_reset"
)

/** Stable sort choices shared by the settings UI. */
internal val SortOptions: List<Pair<String, String>> = listOf(
    "RECENT" to "Edited",
    "CREATED" to "Created",
    "TITLE" to "Title"
)
