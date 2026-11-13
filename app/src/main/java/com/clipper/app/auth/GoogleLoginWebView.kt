package com.clipper.app.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * In-App WebView for official Google Login to NotebookLM.
 * Extracts session cookies and Google's internal CSRF tokens automatically upon successful login.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (cookies: String, csrfToken: String?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var currentUrl by remember { mutableStateOf(SessionManager.NOTEBOOKLM_URL) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top header bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Login",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Login Google NotebookLM",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // WebView Container
                Box(modifier = Modifier.weight(1f)) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    userAgentString = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"
                                    mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                                }

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                // Bridge to receive extracted CSRF token from HTML page
                                addJavascriptInterface(object {
                                    @JavascriptInterface
                                    fun onTokenExtracted(token: String) {
                                        val cookies = CookieManager.getInstance().getCookie(SessionManager.NOTEBOOKLM_URL).orEmpty()
                                        onLoginSuccess(cookies, token)
                                    }
                                }, "AndroidBridge")

                                webViewClient = object : WebViewClient() {
                                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                        super.onPageStarted(view, url, favicon)
                                        isLoading = true
                                        currentUrl = url.orEmpty()
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false
                                        currentUrl = url.orEmpty()

                                        val cookies = CookieManager.getInstance().getCookie(SessionManager.NOTEBOOKLM_URL).orEmpty()
                                        val hasGoogleAuth = cookies.contains("SID=") || cookies.contains("__Secure-1PSID=")

                                        if (hasGoogleAuth && url?.contains("notebooklm.google.com") == true) {
                                            // Extract Google WIZ CSRF token (SNlM0e) from JavaScript context
                                            val jsExtractor = """
                                                (function() {
                                                    try {
                                                        var token = window.WIZ_global_data && window.WIZ_global_data.SNlM0e;
                                                        if (token) {
                                                            window.AndroidBridge.onTokenExtracted(token);
                                                        } else {
                                                            // Fallback scan in page source
                                                            var match = document.body.innerHTML.match(/"SNlM0e":"([^"]+)"/);
                                                            if (match && match[1]) {
                                                                window.AndroidBridge.onTokenExtracted(match[1]);
                                                            } else {
                                                                window.AndroidBridge.onTokenExtracted("");
                                                            }
                                                        }
                                                    } catch(e) {
                                                        window.AndroidBridge.onTokenExtracted("");
                                                    }
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(jsExtractor, null)
                                        }
                                    }

                                    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                        return false
                                    }
                                }

                                loadUrl(SessionManager.NOTEBOOKLM_URL)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
