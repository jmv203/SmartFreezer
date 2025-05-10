package com.example.smartfreezer

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import com.example.smartfreezer.util.LocaleHelper
import com.example.smartfreezer.util.SharedPrefManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Configurar el tema según las preferencias guardadas
        val sharedPrefManager = SharedPrefManager(this)
        if (sharedPrefManager.isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

    }

    override fun attachBaseContext(base: Context) {
        val sharedPref = SharedPrefManager(base)
        val language = sharedPref.getAppLanguage()
        super.attachBaseContext(LocaleHelper.setLocale(base, language))
    }
}
