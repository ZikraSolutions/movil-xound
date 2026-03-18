package com.example.xound.data.local

import android.content.Context
import android.content.SharedPreferences

object SessionManager {

    private const val PREFS_NAME = "xound_session"
    private const val KEY_TOKEN = "token"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DARK_MODE = "dark_mode"
    // "system" = follow system, "on" = always dark, "off" = always light
    private const val KEY_DARK_MODE_OPTION = "dark_mode_option"
    // "admin" or "musician"
    private const val KEY_USER_MODE = "user_mode"
    private const val KEY_ROLE_NAME = "role_name"

    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveSession(token: String, userId: Long?, name: String?, email: String?, roleName: String? = null) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putLong(KEY_USER_ID, userId ?: -1L)
            .putString(KEY_USER_NAME, name ?: "")
            .putString(KEY_USER_EMAIL, email ?: "")
            .putString(KEY_ROLE_NAME, roleName ?: "")
            .apply()

        // Auto-set user mode based on backend role
        if (roleName != null) {
            when (roleName.uppercase()) {
                "ADMIN", "SUPER_ADMIN" -> setUserMode("admin")
                "MUSICIAN" -> {
                    // Keep existing mode if already set (user already chose before)
                    // Don't override — musician still needs to join a band first time
                }
            }
        }
    }

    fun getToken(): String? = prefs.getString(KEY_TOKEN, null)

    fun getUserName(): String = prefs.getString(KEY_USER_NAME, "") ?: ""

    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun getUserId(): Long = prefs.getLong(KEY_USER_ID, -1L)

    fun isLoggedIn(): Boolean = !getToken().isNullOrBlank()

    fun getUserMode(): String = prefs.getString(KEY_USER_MODE, "") ?: ""

    fun setUserMode(mode: String) {
        prefs.edit().putString(KEY_USER_MODE, mode).apply()
    }

    fun hasSelectedMode(): Boolean = getUserMode().isNotBlank()

    fun isMusician(): Boolean = getUserMode() == "musician"

    fun getRoleName(): String = prefs.getString(KEY_ROLE_NAME, "") ?: ""

    fun getDarkModeOption(): String = prefs.getString(KEY_DARK_MODE_OPTION, "system") ?: "system"

    fun setDarkModeOption(option: String) {
        prefs.edit().putString(KEY_DARK_MODE_OPTION, option).apply()
    }

    fun clearSession() {
        val darkOption = getDarkModeOption()
        prefs.edit().clear().apply()
        // Preserve dark mode preference across logout
        setDarkModeOption(darkOption)
        // Mode is cleared so the user can choose again on next login
    }
}
