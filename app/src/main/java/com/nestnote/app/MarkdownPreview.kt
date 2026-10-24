package com.nestnote.app

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.ForegroundColorSpan
import android.text.style.URLSpan
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.tables.TablePlugin
import java.util.Locale

/**
 * CommonMark-based native-text Markdown preview.
 *
 * Note bodies are untrusted. Raw HTML rendering is not enabled, and generated links are
 * restricted to supported schemes. Rendering never rewrites the stored Markdown source.
 */
internal object MarkdownPreview {
    private val allowedLinkSchemes = setOf("http", "https", "mailto")
    private fun readablePipeTables(markdown: String): String {
        val lines = markdown.split("\n").toMutableList()
        var i = 0
        var inFence = false
        var fence = ""
        while (i < lines.size) {
            val trimmed = lines[i].trimStart()
            val marker = when {
                trimmed.startsWith("```") -> "```"
                trimmed.startsWith("~~~") -> "~~~"
                else -> ""
            }
            if (marker.isNotEmpty()) {
                if (!inFence) { inFence = true; fence = marker }
                else if (marker == fence) { inFence = false; fence = "" }
                i++
                continue
            }
            if (inFence || i + 1 >= lines.size || !isPipeRow(lines[i]) || !isTableSeparator(lines[i + 1])) {
                i++
                continue
            }
            val start = i
            val rows = mutableListOf<List<String>>()
            rows += tableCells(lines[i])
            i += 2
            while (i < lines.size && lines[i].isNotBlank() && isPipeRow(lines[i])) {
                rows += tableCells(lines[i])
                i++
            }
            val columns = rows.maxOfOrNull { it.size } ?: 0
            val widths = (0 until columns).map { col ->
                rows.maxOfOrNull { it.getOrElse(col) { "" }.length } ?: 0
            }
            val readable = rows.map { row ->
                (0 until columns).joinToString("   ") { col -> row.getOrElse(col) { "" }.padEnd(widths[col]) }.trimEnd()
            }
            lines.subList(start, i).clear()
            lines.addAll(start, readable)
            i = start + readable.size
        }
        return lines.joinToString("\n")
    }

    private fun isPipeRow(line: String): Boolean =
        line.contains('|') && line.count { it == '|' } >= 2 && line.isNotBlank()

    private fun tableCells(line: String): List<String> {
        var value = line.trim()
        if (value.startsWith("|")) value = value.drop(1)
        if (value.endsWith("|")) value = value.dropLast(1)
        return value.split('|').map(String::trim)
    }

    private fun isTableSeparator(line: String): Boolean =
        tableCells(line).isNotEmpty() && tableCells(line).all { it.matches(Regex(":?-{3,}:?")) }

    fun toSpanned(
        context: Context,
        markdown: String,
        foregroundColor: Int = Color.BLACK
    ): Spanned {
        val renderer = Markwon.builder(context.applicationContext)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(context.applicationContext))
            .usePlugin(TablePlugin.create(context.applicationContext))
            .build()
        // The Markwon table extension uses drawing spans that do not expose their text reliably
        // through Spanned.toString() on all Android/test runtimes. Convert table blocks to
        // aligned readable cells first; preserve all other Markdown for Markwon.
        val rendered = SpannableStringBuilder(renderer.toMarkdown(readablePipeTables(markdown)))

        if (foregroundColor != Color.BLACK) {
            // Normalize every explicit foreground span to the active note color. Markdown
            // renderers may use more than pure black for headings or other block elements;
            // leaving those defaults intact can make text unreadable on dark surfaces.
            rendered.getSpans(0, rendered.length, ForegroundColorSpan::class.java)
                .forEach { span ->
                    val start = rendered.getSpanStart(span)
                    val end = rendered.getSpanEnd(span)
                    val flags = rendered.getSpanFlags(span)
                    rendered.removeSpan(span)
                    if (start >= 0 && end >= start) {
                        rendered.setSpan(ForegroundColorSpan(foregroundColor), start, end, flags)
                    }
                }
        }

        // Never allow note content to launch arbitrary Android intents.
        rendered.getSpans(0, rendered.length, URLSpan::class.java)
            .filter { span ->
                val scheme = runCatching {
                    Uri.parse(span.url).scheme?.lowercase(Locale.ROOT)
                }.getOrNull()
                scheme !in allowedLinkSchemes
            }
            .forEach(rendered::removeSpan)

        return rendered
    }
}
