package com.clipper.app.auth

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Google Account session cookies for Google NotebookLM.
 * Captures `__Secure-1PSID`, `__Secure-3PSID`, `HSID`, `SSID`, `OSID`, and `SNlM0e` CSRF tokens.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = try {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "clipper_secure_session",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback for devices without keystore hardware support
        context.getSharedPreferences("clipper_session_fallback", Context.MODE_PRIVATE)
    }

    private val _isLoggedIn = MutableStateFlow(hasValidSession())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    fun getCookies(): String {
        val stored = prefs.getString(KEY_COOKIES, null)
        if (!stored.isNullOrBlank()) return stored
        return CookieManager.getInstance().getCookie(NOTEBOOKLM_URL).orEmpty()
    }

    fun saveCookies(cookieString: String) {
        prefs.edit().putString(KEY_COOKIES, cookieString).apply()
        _isLoggedIn.value = hasValidSession()
    }

    fun getCsrfToken(): String? {
        return prefs.getString(KEY_CSRF_TOKEN, null)
    }

    fun saveCsrfToken(token: String) {
        prefs.edit().putString(KEY_CSRF_TOKEN, token).apply()
    }

    fun getGeminiApiKey(): String? {
        return prefs.getString(KEY_GEMINI_KEY, null)
    }

    fun saveGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_GEMINI_KEY, key).apply()
        _isLoggedIn.value = true
    }

    fun hasValidSession(): Boolean {
        if (!getGeminiApiKey().isNullOrBlank()) return true
        val cookies = getCookies()
        // Essential Google session identifiers
        return cookies.contains("SID=") || 
               cookies.contains("__Secure-1PSID=") || 
               cookies.contains("HSID=")
    }

    fun clearSession() {
        prefs.edit().clear().apply()
        CookieManager.getInstance().removeAllCookies(null)
        CookieManager.getInstance().flush()
        _isLoggedIn.value = false
    }

    companion object {
        const val NOTEBOOKLM_URL = "https://notebooklm.google.com"
        private const val KEY_COOKIES = "google_cookies"
        private const val KEY_CSRF_TOKEN = "google_csrf_token"
        private const val KEY_GEMINI_KEY = "gemini_api_key"
    }
}
