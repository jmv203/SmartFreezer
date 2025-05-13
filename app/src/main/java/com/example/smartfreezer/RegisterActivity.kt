package com.example.smartfreezer

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var verificationEmailSent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val nameInputLayout = findViewById<TextInputLayout>(R.id.nameInputLayout)
        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val confirmPasswordInputLayout = findViewById<TextInputLayout>(R.id.confirmPasswordInputLayout)

        val nameInput = findViewById<TextInputEditText>(R.id.nameInput)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)
        val confirmPasswordInput = findViewById<TextInputEditText>(R.id.confirmPasswordInput)

        val acceptTermsCheckBox = findViewById<MaterialCheckBox>(R.id.acceptTermsCheckBox)
        val registerButton = findViewById<MaterialButton>(R.id.registerButton)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true

            // Validar nombre
            when {
                name.isEmpty() -> {
                    nameInputLayout.error = getString(R.string.register_name_empty)
                    isValid = false
                }
                !name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]{1,15}$")) -> {
                    nameInputLayout.error = getString(R.string.register_name_invalid)
                    isValid = false
                }
                else -> nameInputLayout.error = null
            }

            // Validar email
            when {
                email.isEmpty() -> {
                    emailInputLayout.error = getString(R.string.register_email_empty)
                    isValid = false
                }
                email.length > 40 -> {
                    emailInputLayout.error = getString(R.string.register_email_too_long)
                    isValid = false
                }
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailInputLayout.error = getString(R.string.register_email_invalid)
                    isValid = false
                }
                else -> emailInputLayout.error = null
            }

            // Validar contraseña
            when {
                password.isEmpty() -> {
                    passwordInputLayout.error = getString(R.string.register_password_empty)
                    isValid = false
                }
                password.length < 8 -> {
                    passwordInputLayout.error = getString(R.string.register_password_short)
                    isValid = false
                }
                !password.matches(Regex("^(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]+$")) -> {
                    passwordInputLayout.error = getString(R.string.register_password_requirements)
                    isValid = false
                }
                else -> passwordInputLayout.error = null
            }

            // Validar confirmación
            when {
                confirmPassword.isEmpty() -> {
                    confirmPasswordInputLayout.error = getString(R.string.register_confirm_empty)
                    isValid = false
                }
                confirmPassword != password -> {
                    confirmPasswordInputLayout.error = getString(R.string.register_password_mismatch)
                    isValid = false
                }
                else -> confirmPasswordInputLayout.error = null
            }

            if (!acceptTermsCheckBox.isChecked) {
                showErrorToast(getString(R.string.register_terms_not_accepted))
                isValid = false
            }

            if (isValid) {
                showProgressDialog()
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            user?.sendEmailVerification()
                                ?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        verificationEmailSent = true
                                        showVerificationDialog(email, user.uid, name)
                                    } else {
                                        user.delete()
                                        showErrorToast(getString(R.string.register_verification_error, verifyTask.exception?.message))
                                    }
                                    dismissProgressDialog()
                                }
                        } else {
                            dismissProgressDialog()
                            when (task.exception) {
                                is FirebaseAuthUserCollisionException -> {
                                    showErrorToast(getString(R.string.error_email_already_exists))
                                }
                                else -> {
                                    showErrorToast(getString(R.string.register_error, task.exception?.message))
                                }
                            }
                        }
                    }
            }
        }

        // Limpiar errores al enfocar
        nameInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) nameInputLayout.error = null }
        emailInput.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) emailInputLayout.error = null }
        passwordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                passwordInputLayout.error = null
                passwordInputLayout.endIconMode = TextInputLayout.END_ICON_PASSWORD_TOGGLE
            }
        }
        confirmPasswordInput.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) confirmPasswordInputLayout.error = null
        }
    }

    private fun showVerificationDialog(email: String, userId: String, name: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(getString(R.string.verify_email_title))
        builder.setMessage(getString(R.string.verify_email_message, email))

        builder.setPositiveButton(getString(R.string.verify_email_resend)) { dialog, _ ->
            auth.currentUser?.sendEmailVerification()
                ?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, getString(R.string.verify_email_resent), Toast.LENGTH_SHORT).show()
                    } else {
                        showErrorToast(getString(R.string.register_verification_error, task.exception?.message))
                    }
                }
        }

        builder.setNegativeButton(getString(R.string.verify_email_understood)) { dialog, _ ->
            // Solo guardar en Firestore después de verificación
            val userRef = db.collection("users").document(userId)
            userRef.set(hashMapOf(
                "name" to name,
                "email" to email,
                "verified" to false
            ))

            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java).apply {
                putExtra("emailPendingVerification", true)
            })
            finish()
        }

        builder.setOnCancelListener {
            if (!verificationEmailSent) {
                auth.currentUser?.delete()
            }
        }

        builder.setCancelable(false)
        builder.show()
    }

    private fun showErrorToast(message: String) {
        val toast = Toast.makeText(this, message, Toast.LENGTH_LONG)
        toast.view?.setBackgroundColor(ContextCompat.getColor(this, R.color.error))
        toast.show()
    }

    private fun showProgressDialog() {
        // Implementa tu diálogo de progreso aquí
    }

    private fun dismissProgressDialog() {
        // Implementa el cierre del diálogo de progreso aquí
    }
}