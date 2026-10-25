package com.nestnote.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchTest {
    @Test
    fun blankQueryKeepsEverySectionVisible() {
        SettingsSearchSections.forEach { section ->
            assertTrue("$section should match a blank query", matchesSettingsSearch("  ", section))
        }
    }

    @Test
    fun matchingIsCaseInsensitiveAndSupportsPartialKeywords() {
        assertTrue(matchesSettingsSearch("THEME", "appearance"))
        assertTrue(matchesSettingsSearch("wav", "voice"))
        assertTrue(matchesSettingsSearch("mark", "editor"))
    }

    @Test
    fun keywordSearchCanMatchWhenQueryContainsKeyword() {
        assertTrue(matchesSettingsSearch("pure black dark mode", "appearance"))
        assertTrue(matchesSettingsSearch("privacy settings", "privacy"))
    }

    @Test
    fun unrelatedQueryDoesNotMatchSection() {
        assertFalse(matchesSettingsSearch("watermelon", "appearance"))
        assertFalse(matchesSettingsSearch("theme", "voice"))
        assertFalse(matchesSettingsSearch("theme", "unknown"))
    }
}
