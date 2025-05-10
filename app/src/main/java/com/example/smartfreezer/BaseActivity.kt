package com.example.smartfreezer

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import com.example.smartfreezer.util.LocaleHelper
import com.example.smartfreezer.util.SharedPrefManager

open class BaseActivity : AppCompatActivity() {
    override fun attachBaseContext(newBase: Context) {
        val langCode = SharedPrefManager(newBase).getAppLanguage()
        super.attachBaseContext(
            if (langCode.isNotEmpty()) LocaleHelper.setLocale(newBase, langCode)
            else newBase
        )
    }
}
