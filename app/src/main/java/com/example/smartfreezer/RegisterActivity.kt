package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
                    nameInputLayout.error = "El nombre no puede estar vacío"
                    isValid = false
                }
                !name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]{1,15}$")) -> {
                    nameInputLayout.error = "Máximo 15 letras, sin símbolos"
                    isValid = false
                }
                else -> nameInputLayout.error = null
            }

            // Validar email
            when {
                email.isEmpty() -> {
                    emailInputLayout.error = "El correo no puede estar vacío"
                    isValid = false
                }
                email.length > 40 -> {
                    emailInputLayout.error = "El correo no debe superar 40 caracteres"
                    isValid = false
                }
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                    emailInputLayout.error = "Formato de correo no válido"
                    isValid = false
                }
                else -> emailInputLayout.error = null
            }

            // Validar contraseña
            when {
                password.isEmpty() -> {
                    passwordInputLayout.error = "La contraseña no puede estar vacía"
                    isValid = false
                }
                password.length < 8 -> {
                    passwordInputLayout.error = "Debe tener al menos 8 caracteres"
                    isValid = false
                }
                !password.matches(Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@\$!%*?&]+$")) -> {
                    passwordInputLayout.error = "Debe incluir una mayúscula y un número"
                    isValid = false
                }
                else -> passwordInputLayout.error = null
            }

            // Validar confirmación
            when {
                confirmPassword.isEmpty() -> {
                    confirmPasswordInputLayout.error = "Confirma tu contraseña"
                    isValid = false
                }
                confirmPassword != password -> {
                    confirmPasswordInputLayout.error = "Las contraseñas no coinciden"
                    isValid = false
                }
                else -> confirmPasswordInputLayout.error = null
            }

            // Términos y condiciones
            if (!acceptTermsCheckBox.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
                isValid = false
            }

            // Registro en Firebase
            if (isValid) {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            val uuid = user?.uid ?: return@addOnCompleteListener

                            user.sendEmailVerification()
                                .addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        val userMap = hashMapOf(
                                            "name" to name,
                                            "email" to email,
                                            "uuid" to uuid
                                        )

                                        db.collection("users").document(uuid)
                                            .set(userMap)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Verificación enviada a $email", Toast.LENGTH_LONG).show()
                                                auth.signOut()
                                                startActivity(Intent(this, LoginActivity::class.java).apply {
                                                    putExtra("emailPendingVerification", true)
                                                })
                                                finish()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Error al guardar datos: ${e.message}", Toast.LENGTH_LONG).show()
                                            }
                                    } else {
                                        Toast.makeText(this, "Error al enviar verificación: ${verifyTask.exception?.message}", Toast.LENGTH_LONG).show()
                                    }
                                }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }
    }
}
