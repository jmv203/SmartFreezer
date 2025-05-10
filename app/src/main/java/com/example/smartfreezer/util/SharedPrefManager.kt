package com.example.smartfreezer.util

import android.content.Context
import android.content.SharedPreferences

class SharedPrefManager(context: Context) {
    private val sharedPref: SharedPreferences = context.getSharedPreferences("SmartFreezerPrefs", Context.MODE_PRIVATE)

    // Notificaciones
    fun setNotificationStatus(enabled: Boolean) {
        sharedPref.edit().putBoolean("notifications_enabled", enabled).apply()
    }

    fun getNotificationStatus(): Boolean {
        return sharedPref.getBoolean("notifications_enabled", true)
    }

    // Modo oscuro
    fun setDarkModeEnabled(enabled: Boolean) {
        sharedPref.edit().putBoolean("dark_mode_enabled", enabled).apply()
    }

    fun isDarkModeEnabled(): Boolean {
        return sharedPref.getBoolean("dark_mode_enabled", false)
    }

    // Idioma
    fun setAppLanguage(languageCode: String) {
        sharedPref.edit().putString("app_language", languageCode).apply()
    }

    fun getAppLanguage(): String {
        return sharedPref.getString("app_language", "") ?: ""
    }

    // Usuario
    fun setUserName(name: String) {
        sharedPref.edit().putString("user_name", name).apply()
    }

    fun getUserName(): String? {
        return sharedPref.getString("user_name", null)
    }
}