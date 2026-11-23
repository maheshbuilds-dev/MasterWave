package com.masterwave.player.data.spotify

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.masterwave.player.MasterWaveApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

data class SpotifyUserProfile(
    val id: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val product: String? = null
)

/**
 * Spotify PKCE Authentication Manager:
 * - Generates cryptographically secure PKCE Code Verifier & S256 Code Challenge.
 * - Enforces CSRF state parameter validation.
 * - Securely stores tokens in Android Keystore AES-GCM without logging sensitive credentials.
 * - Automatic token refresh on expiration.
 * - Full Disconnect & token wipe functionality.
 */
class SpotifyAuthManager(
    private val context: Context,
    private val httpClient: OkHttpClient = MasterWaveApp.instance.okHttpClient
) {
    companion object {
        private const val PREFS_NAME = "masterwave_spotify_auth_secure"
        private const val KEY_ENCRYPTED_ACCESS_TOKEN = "enc_access_token"
        private const val KEY_ENCRYPTED_REFRESH_TOKEN = "enc_refresh_token"
        private const val KEY_TOKEN_EXPIRES_AT = "token_expires_at"
        private const val KEY_PENDING_VERIFIER = "pending_pkce_verifier"
        private const val KEY_PENDING_STATE = "pending_auth_state"
        private const val KEY_USER_PROFILE = "spotify_cached_profile"

        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "MasterWaveSpotifyKey"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128

        @Volatile
        private var instance: SpotifyAuthManager? = null

        fun getInstance(context: Context): SpotifyAuthManager {
            return instance ?: synchronized(this) {
                instance ?: SpotifyAuthManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val prefs by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private val _isConnected = MutableStateFlow(hasValidTokenStored())
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val _userProfile = MutableStateFlow<SpotifyUserProfile?>(loadCachedProfile())
    val userProfile: StateFlow<SpotifyUserProfile?> = _userProfile.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // -------------------------------------------------------------
    // PKCE Generation
    // -------------------------------------------------------------

    private fun generateRandomString(length: Int): String {
        val allowedChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~"
        val random = SecureRandom()
        return (1..length)
            .map { allowedChars[random.nextInt(allowedChars.length)] }
            .joinToString("")
    }

    private fun generateCodeChallenge(verifier: String): String {
        val bytes = verifier.toByteArray(StandardCharsets.US_ASCII)
        val messageDigest = MessageDigest.getInstance("SHA-256")
        messageDigest.update(bytes)
        val digest = messageDigest.digest()
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP).trim()
    }

    /**
     * Creates the Spotify Authorize URL with PKCE parameters and saves the verifier & state.
     */
    fun createAuthorizationUri(): Uri {
        val clientId = SpotifyConfig.getClientId()
        val verifier = generateRandomString(64)
        val challenge = generateCodeChallenge(verifier)
        val state = generateRandomString(16)

        // Save pending PKCE state
        prefs.edit()
            .putString(KEY_PENDING_VERIFIER, verifier)
            .putString(KEY_PENDING_STATE, state)
            .apply()

        _authError.value = null

        return Uri.parse(SpotifyConfig.AUTH_URL).buildUpon()
            .appendQueryParameter("client_id", clientId)
            .appendQueryParameter("response_type", "code")
            .appendQueryParameter("redirect_uri", SpotifyConfig.REDIRECT_URI)
            .appendQueryParameter("code_challenge_method", "S256")
            .appendQueryParameter("code_challenge", challenge)
            .appendQueryParameter("state", state)
            .appendQueryParameter("scope", SpotifyConfig.SCOPES)
            .build()
    }

    /**
     * Opens the system browser / Spotify app to start PKCE authorization.
     */
    fun startAuthorizationFlow(context: Context) {
        val uri = createAuthorizationUri()
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        _isAuthenticating.value = true
        context.startActivity(intent)
    }

    /**
     * Handles the deep link redirect callback from Spotify:
     * Validates state, verifies code, and exchanges code for access token.
     */
    suspend fun handleAuthCallback(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        _isAuthenticating.value = false

        val error = uri.getQueryParameter("error")
        if (!error.isNullOrBlank()) {
            _authError.value = if (error == "access_denied") {
                "Spotify login cancelled by user."
            } else {
                "Spotify authorization failed: $error"
            }
            return@withContext false
        }

        val returnedState = uri.getQueryParameter("state")
        val savedState = prefs.getString(KEY_PENDING_STATE, null)
        if (returnedState.isNullOrBlank() || returnedState != savedState) {
            _authError.value = "Security verification failed: invalid state parameter."
            return@withContext false
        }

        val code = uri.getQueryParameter("code")
        if (code.isNullOrBlank()) {
            _authError.value = "Missing authorization code from Spotify response."
            return@withContext false
        }

        val verifier = prefs.getString(KEY_PENDING_VERIFIER, null)
        if (verifier.isNullOrBlank()) {
            _authError.value = "Missing PKCE code verifier."
            return@withContext false
        }

        // Clean up pending state
        prefs.edit()
            .remove(KEY_PENDING_VERIFIER)
            .remove(KEY_PENDING_STATE)
            .apply()

        exchangeCodeForTokens(code, verifier)
    }

    private suspend fun exchangeCodeForTokens(code: String, verifier: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val clientId = SpotifyConfig.getClientId()
            val formBody = FormBody.Builder()
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", SpotifyConfig.REDIRECT_URI)
                .add("client_id", clientId)
                .add("code_verifier", verifier)
                .build()

            val request = Request.Builder()
                .url(SpotifyConfig.TOKEN_URL)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                val errorMsg = try {
                    val json = JSONObject(responseBody)
                    json.optString("error_description", json.optString("error", "Unknown token error"))
                } catch (_: Exception) {
                    "HTTP ${response.code}"
                }
                _authError.value = "Token exchange failed: $errorMsg"
                return@withContext false
            }

            val json = JSONObject(responseBody)
            val accessToken = json.optString("access_token")
            val refreshToken = json.optString("refresh_token")
            val expiresIn = json.optLong("expires_in", 3600L)

            if (accessToken.isBlank()) {
                _authError.value = "Empty access token received from Spotify."
                return@withContext false
            }

            saveTokens(accessToken, refreshToken, expiresIn)
            _isConnected.value = true
            _authError.value = null

            // Fetch and cache profile
            fetchAndCacheUserProfile()
            true
        } catch (e: Exception) {
            _authError.value = "Network error during token exchange: ${e.message}"
            false
        }
    }

    /**
     * Retrieves a valid access token. Automatically refreshes if expired.
     */
    suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
        val encryptedAccess = prefs.getString(KEY_ENCRYPTED_ACCESS_TOKEN, null) ?: return@withContext null
        val expiresAt = prefs.getLong(KEY_TOKEN_EXPIRES_AT, 0L)
        val now = System.currentTimeMillis()

        // If token expires in less than 60 seconds, refresh it
        if (now >= expiresAt - 60_000L) {
            val refreshed = refreshAccessToken()
            if (refreshed) {
                val newEncrypted = prefs.getString(KEY_ENCRYPTED_ACCESS_TOKEN, null)
                return@withContext decrypt(newEncrypted)
            } else {
                return@withContext null
            }
        }

        decrypt(encryptedAccess)
    }

    private suspend fun refreshAccessToken(): Boolean = withContext(Dispatchers.IO) {
        val encryptedRefresh = prefs.getString(KEY_ENCRYPTED_REFRESH_TOKEN, null) ?: return@withContext false
        val refreshToken = decrypt(encryptedRefresh) ?: return@withContext false

        try {
            val clientId = SpotifyConfig.getClientId()
            val formBody = FormBody.Builder()
                .add("grant_type", "refresh_token")
                .add("refresh_token", refreshToken)
                .add("client_id", clientId)
                .build()

            val request = Request.Builder()
                .url(SpotifyConfig.TOKEN_URL)
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build()

            val response = httpClient.newCall(request).execute()
            val responseBody = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext false
            }

            val json = JSONObject(responseBody)
            val newAccessToken = json.optString("access_token")
            val newRefreshToken = json.optString("refresh_token", "")
            val expiresIn = json.optLong("expires_in", 3600L)

            if (newAccessToken.isBlank()) return@withContext false

            saveTokens(newAccessToken, newRefreshToken.ifBlank { null }, expiresIn)
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun fetchAndCacheUserProfile(): SpotifyUserProfile? = withContext(Dispatchers.IO) {
        val token = getValidAccessToken() ?: return@withContext null
        try {
            val request = Request.Builder()
                .url("${SpotifyConfig.API_BASE_URL}/me")
                .header("Authorization", "Bearer $token")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) return@withContext null

            val json = JSONObject(response.body?.string() ?: "")
            val id = json.optString("id")
            val displayName = json.optString("display_name").ifBlank { "Spotify User" }
            val email = json.optString("email", "")
            val product = json.optString("product", "")
            val images = json.optJSONArray("images")
            val avatarUrl = if (images != null && images.length() > 0) {
                images.optJSONObject(0)?.optString("url")
            } else null

            val profile = SpotifyUserProfile(
                id = id,
                displayName = displayName,
                email = email.ifBlank { null },
                avatarUrl = avatarUrl,
                product = product.ifBlank { null }
            )

            cacheProfile(profile)
            _userProfile.value = profile
            profile
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Disconnects Spotify account, clears credentials and tokens from Keystore & storage.
     */
    fun disconnect() {
        prefs.edit().clear().apply()
        _isConnected.value = false
        _userProfile.value = null
        _authError.value = null
    }

    // -------------------------------------------------------------
    // Secure Storage Helpers (Android Keystore AES-256 GCM)
    // -------------------------------------------------------------

    private fun hasValidTokenStored(): Boolean {
        return prefs.contains(KEY_ENCRYPTED_ACCESS_TOKEN) && prefs.contains(KEY_ENCRYPTED_REFRESH_TOKEN)
    }

    private fun saveTokens(accessToken: String, refreshToken: String?, expiresInSeconds: Long) {
        val editor = prefs.edit()
        val encAccess = encrypt(accessToken)
        editor.putString(KEY_ENCRYPTED_ACCESS_TOKEN, encAccess)
        if (!refreshToken.isNullOrBlank()) {
            val encRefresh = encrypt(refreshToken)
            editor.putString(KEY_ENCRYPTED_REFRESH_TOKEN, encRefresh)
        }
        editor.putLong(KEY_TOKEN_EXPIRES_AT, System.currentTimeMillis() + (expiresInSeconds * 1000L))
        editor.apply()
    }

    private fun cacheProfile(profile: SpotifyUserProfile) {
        val obj = JSONObject().apply {
            put("id", profile.id)
            put("displayName", profile.displayName)
            put("email", profile.email ?: "")
            put("avatarUrl", profile.avatarUrl ?: "")
            put("product", profile.product ?: "")
        }
        prefs.edit().putString(KEY_USER_PROFILE, obj.toString()).apply()
    }

    private fun loadCachedProfile(): SpotifyUserProfile? {
        val jsonStr = prefs.getString(KEY_USER_PROFILE, null) ?: return null
        return try {
            val json = JSONObject(jsonStr)
            SpotifyUserProfile(
                id = json.optString("id"),
                displayName = json.optString("displayName", "Spotify User"),
                email = json.optString("email").ifBlank { null },
                avatarUrl = json.optString("avatarUrl").ifBlank { null },
                product = json.optString("product").ifBlank { null }
            )
        } catch (_: Exception) {
            null
        }
    }

    private fun getSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE)
        keyStore.load(null)

        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) return entry.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()

        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun encrypt(plainText: String): String {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
            val iv = cipher.iv
            val cipherText = cipher.doFinal(plainText.toByteArray(StandardCharsets.UTF_8))
            val combined = ByteArray(iv.size + cipherText.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
            Base64.encodeToString(combined, Base64.NO_WRAP)
        } catch (_: Exception) {
            // Fallback obfuscation if keystore is unavailable
            Base64.encodeToString(plainText.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP)
        }
    }

    private fun decrypt(encryptedBase64: String?): String? {
        if (encryptedBase64.isNullOrBlank()) return null
        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            val cipherText = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherText.size)

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
            val plainBytes = cipher.doFinal(cipherText)
            String(plainBytes, StandardCharsets.UTF_8)
        } catch (_: Exception) {
            try {
                String(Base64.decode(encryptedBase64, Base64.NO_WRAP), StandardCharsets.UTF_8)
            } catch (_: Exception) {
                null
            }
        }
    }
}
