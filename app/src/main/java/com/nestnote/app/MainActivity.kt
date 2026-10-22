package com.nestnote.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class MainActivity : ComponentActivity() {
    private var incomingMarkdownUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        incomingMarkdownUri = markdownUriFrom(intent)
        val store = NoteStore(this)
        setContent {
            NestNoteApp(
                store = store,
                externalMarkdownUri = incomingMarkdownUri,
                onExternalMarkdownHandled = { incomingMarkdownUri = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        incomingMarkdownUri = markdownUriFrom(intent)
    }

    private fun markdownUriFrom(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        val uri = intent.data ?: return null
        val mimeType = intent.type
        val fileName = uri.lastPathSegment.orEmpty().substringAfterLast('/')
        val isMarkdown = mimeType == "text/markdown" ||
            mimeType == "text/x-markdown" ||
            fileName.endsWith(".md", ignoreCase = true) ||
            fileName.endsWith(".markdown", ignoreCase = true)
        return uri.takeIf { isMarkdown }
    }
}
