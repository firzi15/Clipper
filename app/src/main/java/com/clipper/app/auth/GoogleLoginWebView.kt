package com.clipper.app.auth

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.clipper.app.ui.theme.NeonYellow

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (cookies: String, csrfToken: String?) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Desktop User Agent bypasses Google Sign-In WebView blocking ("disallowed_useragent")
    val desktopUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Safari/537.36"
    val loginUrl = "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fnotebooklm.google.com%2F"

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Secure Login",
                        tint = NeonYellow
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Login Akun Google",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Menghubungkan ke NotebookLM",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    // Reload Button
                    IconButton(onClick = {
                        errorMessage = null
                        isLoading = true
                        webViewInstance?.reload()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }

                    // Close Button
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // WebView Container
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(Color.White) // Google Login is naturally white
                ) {
                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                webViewInstance = this
                                setBackgroundColor(AndroidColor.WHITE)

                                settings.apply {
                                    javaScriptEnabled = true
                                    domStorageEnabled = true
                                    databaseEnabled = true
                                    userAgentString = desktopUserAgent
                                    loadWithOverviewMode = true
                                    useWideViewPort = true
                                    cacheMode = WebSettings.LOAD_DEFAULT
                                    mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                    javaScriptCanOpenWindowsAutomatically = true
                                }

                                val cookieManager = CookieManager.getInstance()
                                cookieManager.setAcceptCookie(true)
                                cookieManager.setAcceptThirdPartyCookies(this, true)

                                webChromeClient = WebChromeClient()

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
                                        errorMessage = null
                                    }

                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        isLoading = false

                                        val current = url.orEmpty()
                                        val cookies = CookieManager.getInstance().getCookie(SessionManager.NOTEBOOKLM_URL).orEmpty()
                                        val hasAuth = cookies.contains("SID=") || cookies.contains("__Secure-1PSID=")

                                        if (hasAuth && current.contains("notebooklm.google.com")) {
                                            val jsExtractor = """
                                                (function() {
                                                    try {
                                                        var token = window.WIZ_global_data && window.WIZ_global_data.SNlM0e;
                                                        window.AndroidBridge.onTokenExtracted(token || "");
                                                    } catch(e) {
                                                        window.AndroidBridge.onTokenExtracted("");
                                                    }
                                                })();
                                            """.trimIndent()
                                            view?.evaluateJavascript(jsExtractor, null)
                                        }
                                    }

                                    override fun onReceivedError(
                                        view: WebView?,
                                        request: WebResourceRequest?,
                                        error: WebResourceError?
                                    ) {
                                        super.onReceivedError(view, request, error)
                                        if (request?.isForMainFrame == true) {
                                            isLoading = false
                                            errorMessage = "Gagal memuat: ${error?.description ?: "Periksa koneksi internet Anda"}"
                                        }
                                    }
                                }

                                loadUrl(loginUrl)
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )

                    // Loading Spinner
                    if (isLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.padding(top = 12.dp))
                                Text("Memuat halaman login Google...", color = Color.DarkGray, fontSize = 13.sp)
                            }
                        }
                    }

                    // Error Box if network issues occur
                    if (errorMessage != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = errorMessage.orEmpty(),
                                    color = Color.Red,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.padding(top = 16.dp))
                                Button(
                                    onClick = {
                                        errorMessage = null
                                        isLoading = true
                                        webViewInstance?.loadUrl(loginUrl)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
                                ) {
                                    Text("Coba Muat Ulang", color = Color.Black, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Bottom Demo Bypass Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ingin coba langsung tanpa login?",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = {
                        // Quick demo session mock
                        onLoginSuccess("SID=demo_session; __Secure-1PSID=demo_token;", "demo_csrf")
                    }) {
                        Text("Lewati (Mode Demo)", color = NeonYellow, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
