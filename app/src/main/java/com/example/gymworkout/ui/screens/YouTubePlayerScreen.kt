package com.example.gymworkout.ui.screens

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import java.net.URLEncoder
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubePlayerScreen(
    exerciseName: String,
    youtubeUrl: String,
    savedUrl: String? = null,
    onSetVideo: ((String) -> Unit)? = null,
    onRemoveVideo: (() -> Unit)? = null,
    onBack: () -> Unit
) {
    val videoId = extractYouTubeVideoId(youtubeUrl)
    // Video currently on screen: the embedded one, or whichever the user opened from search.
    var currentVideoId by remember { mutableStateOf(videoId) }
    val savedVideoId = savedUrl?.let { extractYouTubeVideoId(it) }
    val hasSavedVideo = !savedUrl.isNullOrBlank()

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    text = exerciseName,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                val current = currentVideoId
                if (onSetVideo != null && current != null) {
                    if (current == savedVideoId) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Video set for this exercise",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    } else {
                        IconButton(onClick = {
                            onSetVideo("https://www.youtube.com/watch?v=$current")
                        }) {
                            Icon(
                                Icons.Default.StarBorder,
                                contentDescription = "Set as video for this exercise"
                            )
                        }
                    }
                }
                if (onRemoveVideo != null && hasSavedVideo) {
                    IconButton(onClick = onRemoveVideo) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Remove video from this exercise",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.mediaPlaybackRequiresUserGesture = false
                    webChromeClient = WebChromeClient()
                    webViewClient = object : WebViewClient() {
                        // YouTube's mobile site navigates via the history API, so this is
                        // the callback that reliably reports the video the user opened.
                        override fun doUpdateVisitedHistory(
                            view: WebView?,
                            url: String?,
                            isReload: Boolean
                        ) {
                            super.doUpdateVisitedHistory(view, url, isReload)
                            if (videoId == null) {
                                currentVideoId = url?.let { extractYouTubeVideoId(it) }
                            }
                        }
                    }

                    if (videoId != null) {
                        val htmlContent = """
                            <!DOCTYPE html>
                            <html>
                            <head>
                                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                                <meta name="referrer" content="strict-origin-when-cross-origin">
                                <style>
                                    * { margin: 0; padding: 0; }
                                    html, body { width: 100%; height: 100%; background: #000; }
                                    iframe { width: 100%; height: 100%; border: none; }
                                </style>
                            </head>
                            <body>
                                <iframe
                                    src="https://www.youtube.com/embed/$videoId?autoplay=1&rel=0&playsinline=1"
                                    allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"
                                    allowfullscreen
                                    referrerpolicy="strict-origin-when-cross-origin">
                                </iframe>
                            </body>
                            </html>
                        """.trimIndent()
                        val baseUrl = "https://${context.packageName}/"
                        loadDataWithBaseURL(baseUrl, htmlContent, "text/html", "UTF-8", null)
                    } else {
                        val searchQuery = URLEncoder.encode("$exerciseName exercise", "UTF-8")
                        loadUrl("https://m.youtube.com/results?search_query=$searchQuery")
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        )
    }
}

fun extractYouTubeVideoId(url: String): String? {
    val patterns = listOf(
        Regex("(?:youtube\\.com/watch\\?v=)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtu\\.be/)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtube\\.com/embed/)([a-zA-Z0-9_-]{11})"),
        Regex("(?:youtube\\.com/shorts/)([a-zA-Z0-9_-]{11})")
    )
    for (pattern in patterns) {
        val match = pattern.find(url)
        if (match != null) return match.groupValues[1]
    }
    return null
}
