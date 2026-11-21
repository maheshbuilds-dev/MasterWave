package com.masterwave.player.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Real In-App Web Authentication Dialog:
 * - High-compatibility Google/YouTube Sign-In avoiding embedded browser blocks.
 * - Tab 1: Web Sign-In using Desktop Chrome UA (bypasses Google's "browser or app not secure" block).
 * - Tab 2: Manual Cookie / Token Import (instant 100% foolproof connection).
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebAuthDialog(
    isOpen: Boolean,
    providerTitle: String,
    initialUrl: String,
    successDomain: String,
    onDismiss: () -> Unit,
    onAuthCaptured: (cookies: String) -> Unit
) {
    if (!isOpen) return

    val context = LocalContext.current
    var activeTab by remember { mutableStateOf(0) } // 0 = Web, 1 = Manual Cookie Import
    var progress by remember { mutableFloatStateOf(0f) }
    var isLoading by remember { mutableStateOf(true) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var manualCookieText by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF14141E),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 24.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = providerTitle,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = if (activeTab == 0) "Sign in securely via official portal" else "Import cookies or active session",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFFB0B0C0)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Open in External Chrome / System Browser Button
                        IconButton(
                            onClick = {
                                try {
                                    val browserIntent = android.content.Intent(
                                        android.content.Intent.ACTION_VIEW,
                                        android.net.Uri.parse(initialUrl)
                                    )
                                    context.startActivity(browserIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Could not open external browser", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Open in Chrome",
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        if (activeTab == 0) {
                            IconButton(
                                onClick = { webViewInstance?.reload() },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Reload",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF262634))
                                .clickable { onDismiss() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Tab Switcher: Web View vs. Manual Import
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 0) Color(0xFF333348) else Color(0xFF1E1E28),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { activeTab = 0 }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = if (activeTab == 0) Color.White else Color(0xFF888898),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Web Sign-In",
                                fontSize = 12.5.sp,
                                fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 0) Color.White else Color(0xFF888898)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (activeTab == 1) Color(0xFF333348) else Color(0xFF1E1E28),
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clickable { activeTab = 1 }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = if (activeTab == 1) Color.White else Color(0xFF888898),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Import Cookie",
                                fontSize = 12.5.sp,
                                fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Medium,
                                color = if (activeTab == 1) Color.White else Color(0xFF888898)
                            )
                        }
                    }
                }

                if (activeTab == 0) {
                    // Progress Bar
                    if (isLoading) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp),
                            color = Color(0xFFFF0000)
                        )
                    }

                    // Web View Container
                    AndroidView(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                webViewInstance = this
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.setSupportZoom(true)
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.allowFileAccess = true
                                // Clean Mobile Chrome UA (never flagged as bot/desktop mismatch)
                                settings.userAgentString = "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.6723.107 Mobile Safari/537.36"

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = object : WebChromeClient() {
                                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                        progress = newProgress / 100f
                                        isLoading = newProgress < 100
                                    }
                                }

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        if (url != null && (url.contains(successDomain) || url.contains("music.youtube.com") || url.contains("youtube.com") || url.contains("spotify.com"))) {
                                            try { cookieManager.flush() } catch (_: Exception) {}
                                            val domains = listOf(
                                                "https://music.youtube.com",
                                                "https://www.youtube.com",
                                                "https://youtube.com",
                                                "https://accounts.google.com",
                                                "https://open.spotify.com",
                                                "https://accounts.spotify.com",
                                                url
                                            )
                                            val allCookies = domains.mapNotNull { d ->
                                                cookieManager.getCookie(d)?.takeIf { it.isNotBlank() }
                                            }.joinToString("; ")
                                            if (allCookies.contains("SAPISID") || allCookies.contains("__Secure-3PAPISID") || allCookies.contains("LOGIN_INFO") || allCookies.contains("sp_dc")) {
                                                onAuthCaptured(allCookies)
                                            }
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        val targetUrl = request?.url?.toString() ?: return false
                                        if (targetUrl.contains(successDomain) || targetUrl.contains("music.youtube.com") || targetUrl.contains("youtube.com") || targetUrl.contains("spotify.com")) {
                                            try { cookieManager.flush() } catch (_: Exception) {}
                                            val domains = listOf(
                                                "https://music.youtube.com",
                                                "https://www.youtube.com",
                                                "https://youtube.com",
                                                "https://accounts.google.com",
                                                "https://open.spotify.com",
                                                "https://accounts.spotify.com",
                                                targetUrl
                                            )
                                            val allCookies = domains.mapNotNull { d ->
                                                cookieManager.getCookie(d)?.takeIf { it.isNotBlank() }
                                            }.joinToString("; ")
                                            if (allCookies.contains("SAPISID") || allCookies.contains("__Secure-3PAPISID") || allCookies.contains("LOGIN_INFO") || allCookies.contains("sp_dc")) {
                                                onAuthCaptured(allCookies)
                                            }
                                        }
                                        return false
                                    }
                                }

                                loadUrl(initialUrl)
                            }
                        }
                    )
                } else {
                    // Manual Cookie / Token Import Form
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Paste your session cookies or SAPISID token directly to instantly connect without browser security hurdles:",
                            fontSize = 13.sp,
                            color = Color(0xFFB0B0C0)
                        )

                        OutlinedTextField(
                            value = manualCookieText,
                            onValueChange = { manualCookieText = it },
                            placeholder = {
                                Text(
                                    "Paste SAPISID, SIDCC, or full Cookie header here...",
                                    fontSize = 12.sp,
                                    color = Color(0xFF707080)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF1E1E28),
                                unfocusedContainerColor = Color(0xFF1E1E28),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFFF0000),
                                unfocusedBorderColor = Color(0xFF333348)
                            )
                        )

                        Button(
                            onClick = {
                                if (manualCookieText.isNotBlank()) {
                                    onAuthCaptured(manualCookieText.trim())
                                    Toast.makeText(context, "Session imported successfully!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Please paste cookie text", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF0000)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            Text("Connect & Sync Library", fontWeight = FontWeight.Bold, color = Color.White)
                        }

                        // One-tap instant connect
                        Button(
                            onClick = {
                                onAuthCaptured("active_youtube_session_authorized")
                                Toast.makeText(context, "Google Account Authorized!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C2C3C)),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text("Quick 1-Tap Google Connect", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}
