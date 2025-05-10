package com.example.smartfreezer.util

import android.annotation.TargetApi
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {
    fun setLocale(context: Context, language: String): Context {
        return if (language.isNotEmpty()) {
            updateResources(context, language)
        } else {
            context // Mantener contexto original si no hay idioma seleccionado
        }
    }

    private fun updateResources(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            setSystemLocale(configuration, locale)
            return context.createConfigurationContext(configuration)
        } else {
            setSystemLocaleLegacy(configuration, locale)
            resources.updateConfiguration(configuration, resources.displayMetrics)
        }

        return context
    }

    @Suppress("DEPRECATION")
    private fun setSystemLocaleLegacy(config: Configuration, locale: Locale) {
        config.locale = locale
    }

    @TargetApi(Build.VERSION_CODES.N)
    private fun setSystemLocale(config: Configuration, locale: Locale) {
        config.setLocale(locale)
    }
}