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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.clipper.app.ui.theme.NeonGreen
import com.clipper.app.ui.theme.NeonYellow

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun GoogleLoginDialog(
    onDismiss: () -> Unit,
    onLoginSuccess: (cookies: String, csrfToken: String?) -> Unit,
    onSaveApiKey: ((String) -> Unit)? = null
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var apiKeyText by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }

    // Safari iOS User Agent allows Google Sign In without triggering the Android WebView block
    val safariUserAgent = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.5 Mobile/15E148 Safari/604.1"
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
                        contentDescription = "Authentication",
                        tint = NeonYellow
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Koneksi AI Gemini / Google",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White
                        )
                    }
                }

                // Tabs: Login Google vs Masukkan API Key Gratis
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = NeonYellow
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Login Google (Web)", fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("API Key Gratis", fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: Web Login dengan bypass
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color.White)
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
                                        userAgentString = safariUserAgent
                                        loadWithOverviewMode = true
                                        useWideViewPort = true
                                        cacheMode = WebSettings.LOAD_DEFAULT
                                        mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                                        javaScriptCanOpenWindowsAutomatically = true
                                    }

                                    // Remove X-Requested-With header to stop Google WebView detection
                                    if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                                        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
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
                                                errorMessage = "Gagal memuat: ${error?.description ?: "Periksa koneksi internet"}"
                                            }
                                        }
                                    }

                                    loadUrl(loginUrl)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        if (isLoading) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White.copy(alpha = 0.85f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text("Menghubungkan ke Google...", color = Color.DarkGray, fontSize = 13.sp)
                                }
                            }
                        }

                        if (errorMessage != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                                    .padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(errorMessage.orEmpty(), color = Color.Red, fontSize = 13.sp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = {
                                            errorMessage = null
                                            isLoading = true
                                            webViewInstance?.loadUrl(loginUrl)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow)
                                    ) {
                                        Text("Muat Ulang", color = Color.Black, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // TAB 1: Gemini API Key (100% Resmi & Gratis dari Google AI Studio)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = NeonYellow,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Gunakan Gemini API Key Gratis",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Dapatkan API Key gratis seumur hidup dari Google AI Studio (aistudio.google.com) tanpa kartu kredit. Bebas dari blokir WebView!",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        OutlinedTextField(
                            value = apiKeyText,
                            onValueChange = { apiKeyText = it },
                            placeholder = { Text("Tempel AIzaSy... API Key di sini", color = Color.Gray) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonYellow,
                                unfocusedBorderColor = Color.DarkGray,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (apiKeyText.isNotBlank()) {
                                    onSaveApiKey?.invoke(apiKeyText.trim())
                                    onLoginSuccess("GEMINI_KEY=${apiKeyText.trim()}", "api_mode")
                                }
                            },
                            enabled = apiKeyText.isNotBlank(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                            modifier = Modifier.fillMaxWidth().height(50.dp)
                        ) {
                            Text("Simpan & Aktifkan", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Bottom Demo Bypass Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Langsung coba fitur tanpa akun?",
                        fontSize = 12.sp,
                        color = Color.LightGray,
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            onLoginSuccess("DEMO_SESSION=active", "demo")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonYellow),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Masuk Mode Demo", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
