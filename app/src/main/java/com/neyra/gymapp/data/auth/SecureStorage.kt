package com.neyra.gymapp.data.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureStorage @Inject constructor(@ApplicationContext context: Context) {

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secure_auth_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveToken(token: String) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .putLong(KEY_TOKEN_EXPIRY, System.currentTimeMillis() + TOKEN_VALIDITY_PERIOD)
            .apply()
    }

    fun getToken(): String? {
        return sharedPreferences.getString(KEY_AUTH_TOKEN, null)
    }

    fun getTokenExpiry(): Long {
        return sharedPreferences.getLong(KEY_TOKEN_EXPIRY, 0)
    }

    fun clearTokens() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .apply()
    }

    fun saveUserId(userId: String) {
        sharedPreferences.edit()
            .putString(KEY_USER_ID, userId)
            .apply()
    }

    fun getUserId(): String? {
        return sharedPreferences.getString(KEY_USER_ID, null)
    }

    fun clearAll() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_TOKEN)
            .remove(KEY_TOKEN_EXPIRY)
            .remove(KEY_USER_ID)
            .apply()
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_TOKEN_EXPIRY = "token_expiry"
        private const val KEY_USER_ID = "user_id"
        private const val TOKEN_VALIDITY_PERIOD = 60 * 60 * 1000L // 1 hour
    }
}