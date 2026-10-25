package com.nestnote.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** Destination heading and welcome presentation; all navigation and preference writes are callbacks. */
@Composable
internal fun AppDashboardHeader(
    selectedView: String,
    notes: List<NestNote>,
    filteredCount: Int,
    dark: Boolean,
    layout: DashboardLayout,
    sortByTitle: Boolean,
    sortByCreated: Boolean,
    canEncryptAll: Boolean,
    onCreateNote: () -> Unit,
    onImportMarkdown: () -> Unit,
    onToggleTheme: () -> Unit,
    onSetSort: (String) -> Unit,
    onEncryptAll: () -> Unit,
    onToggleLayout: () -> Unit
) {
    val colors = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectedView == "Home") {
            Text(
                remember { NoteText.greetingFor(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) },
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant
            )
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = colors.primaryContainer
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface.copy(alpha = 0.72f)
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.padding(12.dp).size(26.dp),
                            tint = colors.primary
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("A little space for big ideas", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${notes.count { !it.privateNote && !it.archived }} active notes · ${notes.count { it.pinned && !it.privateNote && !it.archived }} pinned",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onCreateNote) {
                        Icon(Icons.Default.Add, contentDescription = "Create a note", tint = colors.primary)
                    }
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    when (selectedView) {
                        "All notes" -> "All your notes"
                        "Pinned" -> "Pinned notes"
                        "Reminders" -> "Things to remember"
                        "Archived" -> "Archived notes"
                        else -> selectedView
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$filteredCount notes",
                    color = colors.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        AppNoteCollectionToolbar(
            selectedView = selectedView,
            dark = dark,
            sortByTitle = sortByTitle,
            sortByCreated = sortByCreated,
            dashboardLayout = layout,
            canEncryptAll = canEncryptAll,
            onImportMarkdown = onImportMarkdown,
            onThemeToggle = onToggleTheme,
            onSortChange = onSetSort,
            onEncryptAll = onEncryptAll,
            onLayoutToggle = onToggleLayout
        )
    }
}
