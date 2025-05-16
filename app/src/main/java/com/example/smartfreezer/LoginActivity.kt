package com.example.smartfreezer

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.smartfreezer.models.User
import com.example.smartfreezer.util.FCMTopicManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlin.jvm.java


class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loadUserAndSubscribeToTopics()

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("login_prefs", Context.MODE_PRIVATE)

        // Verificar sesión guardada
        if (sharedPreferences.getBoolean("keep_logged_in", false)) {
            val currentUser = auth.currentUser
            if (currentUser != null && currentUser.isEmailVerified) {
                startActivity(Intent(this, HomeActivity::class.java))
                finish()
                return
            }
        }

        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val keepLoggedInCheckBox = findViewById<MaterialCheckBox>(R.id.keepLoggedIn)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val registerRedirect = findViewById<TextView>(R.id.registerLink)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPassword)

        keepLoggedInCheckBox.text = getString(R.string.login_keep_logged_in)

        // Mostrar aviso si viene de registro sin verificar
        if (intent.getBooleanExtra("emailPendingVerification", false)) {
            showErrorToast(getString(R.string.login_email_not_verified))
        }

        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            if (email.isEmpty()) {
                emailInputLayout.error = getString(R.string.login_email_empty)
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                passwordInputLayout.error = getString(R.string.login_password_empty)
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null && user.isEmailVerified) {
                            // Guardar preferencia de mantener sesión
                            sharedPreferences.edit().apply {
                                putBoolean("keep_logged_in", keepLoggedInCheckBox.isChecked)
                                apply()
                            }

                            startActivity(Intent(this, HomeActivity::class.java))
                            finish()
                        } else {
                            auth.signOut()
                            showErrorToast(getString(R.string.login_email_not_verified))
                        }
                    } else {
                        val error = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> getString(R.string.login_error, "Usuario no registrado")
                            is FirebaseAuthInvalidCredentialsException -> getString(R.string.login_error, "Credenciales incorrectas")
                            else -> getString(R.string.login_error, task.exception?.message)
                        }
                        showErrorToast(error)
                    }
                }
        }

        registerRedirect.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
            finish()
        }

        forgotPasswordLink.setOnClickListener {
            showForgotPasswordDialog()
        }

        // Limpiar errores al enfocar
        emailInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) emailInputLayout.error = null
        }

        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                passwordInputLayout.error = null
                passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
        }

        //Obtener el token FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                // Puedes guardarlo en Firestore o loguearlo
                println("FCM Token: $token")
            }
        }
    }
    private fun loadUserAndSubscribeToTopics() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance().collection("users")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java) ?: return@addOnSuccessListener
                FCMTopicManager.subscribeToDietTopics(user)
            }
            .addOnFailureListener { e ->
                Log.e("HomeActivity", "Error loading user preferences", e)
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.forgot_password_title))
        builder.setMessage(getString(R.string.forgot_password_message))

        val input = TextInputEditText(this).apply {
            hint = getString(R.string.forgot_password_hint)
            setPadding(32, 16, 32, 16)
        }

        builder.setView(input)
        builder.setPositiveButton(getString(R.string.forgot_password_send)) { dialog, _ ->
            val email = input.text.toString().trim()
            if (email.isNotEmpty()) {
                auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, getString(R.string.forgot_password_success), Toast.LENGTH_LONG).show()
                        } else {
                            showErrorToast(getString(R.string.forgot_password_error, task.exception?.message))
                        }
                    }
            }
        }
        builder.setNegativeButton(getString(R.string.forgot_password_cancel), null)
        builder.show()
    }

    private fun showErrorToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.view?.setBackgroundColor(ContextCompat.getColor(this, R.color.error))
        toast.show()
    }


}