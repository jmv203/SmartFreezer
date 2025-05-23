package com.example.smartfreezer

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import com.example.smartfreezer.databinding.ActivitySettingsBinding
import com.example.smartfreezer.util.LocaleHelper
import com.example.smartfreezer.util.SharedPrefManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging

class SettingsActivity : BaseActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var sharedPrefManager: SharedPrefManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefManager = SharedPrefManager(this)

        setupGreeting()
        setupHeader()
        setupNotificationSwitch()
        setupDarkModeSwitch()
        setupLanguageSelection()
        setupInfoOptions()
        setupSupportOption()
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    binding.tvGreetingSettings.text = getString(R.string.hola, name)
                }
            }
    }

    private fun setupHeader() {
        // Configurar botones del header
        binding.btnAccountSettings.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
    }

    private fun setupNotificationSwitch() {
        binding.notificationsSwitch.isChecked = sharedPrefManager.getNotificationStatus()

        binding.notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            sharedPrefManager.setNotificationStatus(isChecked)

            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this,
                                getString(R.string.notificaciones_activadas), Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("all")
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this,
                                getString(R.string.notificaciones_desactivadas), Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
    }


    private fun setupDarkModeSwitch() {
        // Configurar el switch según el modo actual
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        binding.darkModeSwitch.isChecked = currentNightMode == Configuration.UI_MODE_NIGHT_YES

        binding.darkModeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
            sharedPrefManager.setDarkModeEnabled(isChecked)

        }
    }

    private fun setupLanguageSelection() {
        // Mostrar el idioma actual
        updateLanguageText()

        binding.languageOption.setOnClickListener {
            showLanguageDialog()
        }
    }

    private fun updateLanguageText() {
        val currentLang = sharedPrefManager.getAppLanguage()
        binding.selectedLanguageTextView.text = when (currentLang) {
            "en" -> "English"
            "es" -> "Español"
            else -> "Español" // Default
        }
    }

    private fun showLanguageDialog() {
        val languages = arrayOf(
            LanguageOption("Español", "es"),
            LanguageOption("English", "en")
        )

        val currentLang = sharedPrefManager.getAppLanguage()
        var selectedIndex = languages.indexOfFirst { it.code == currentLang }
        if (selectedIndex == -1) selectedIndex = 0

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.language))
            .setSingleChoiceItems(
                languages.map { it.name }.toTypedArray(),
                selectedIndex
            ) { _, which ->
                selectedIndex = which
            }
            .setPositiveButton(android.R.string.ok) { dialog, _ ->
                val selectedLanguage = languages[selectedIndex]
                if (selectedLanguage.code != sharedPrefManager.getAppLanguage()) {
                    changeAppLanguage(selectedLanguage.code)
                }
                dialog.dismiss()
            }
            .setNegativeButton((getString(R.string.cancelar)), null)
            .show()
    }

    private fun changeAppLanguage(languageCode: String) {
        sharedPrefManager.setAppLanguage(languageCode)
        LocaleHelper.setLocale(this, languageCode)

        // Forzar recreación de toda la aplicación
        val intent = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("navigateTo", "inventory")
        }
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }



    data class LanguageOption(val name: String, val code: String)



    private fun setupInfoOptions() {
        binding.aboutOption.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        binding.termsConditionsOption.setOnClickListener {
            startActivity(Intent(this, TermsActivity::class.java))
        }
    }

    private fun setupSupportOption() {
        binding.contactSupportOption.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                type = "message/rfc822"
                putExtra(Intent.EXTRA_EMAIL, arrayOf("soporte@smartfreezer.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Soporte SmartFreezer")
            }

            if (emailIntent.resolveActivity(packageManager) != null) {
                startActivity(Intent.createChooser(emailIntent, "Enviar correo..."))
            } else {

            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Podrías añadir una animación personalizada aquí
        finish()
    }
}