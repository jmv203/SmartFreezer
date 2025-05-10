package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

        // Mostrar aviso si viene de registro sin verificar correo
        val emailPending = intent.getBooleanExtra("emailPendingVerification", false)
        if (emailPending) {
            Toast.makeText(this, "Verifica tu correo antes de iniciar sesión", Toast.LENGTH_LONG).show()
        }

        // Referencias a los elementos del layout
        val emailInputLayout = findViewById<TextInputLayout>(R.id.emailInputLayout)
        val passwordInputLayout = findViewById<TextInputLayout>(R.id.passwordInputLayout)
        val emailInput = findViewById<TextInputEditText>(R.id.emailInput)
        val passwordInput = findViewById<TextInputEditText>(R.id.passwordInput)

        val keepLoggedInCheckBox = findViewById<MaterialCheckBox>(R.id.keepLoggedIn)
        val loginButton = findViewById<MaterialButton>(R.id.loginButton)
        val registerRedirect = findViewById<TextView>(R.id.registerLink)
        val forgotPasswordLink = findViewById<TextView>(R.id.forgotPassword)

        // Manejar el clic del botón de inicio de sesión
        loginButton.setOnClickListener {
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()

            var isValid = true

            // Validar email
            if (email.isEmpty()) {
                emailInputLayout.error = "El correo no puede estar vacío"
                isValid = false
            } else if (email.length > 40) {
                emailInputLayout.error = "El correo no debe superar 40 caracteres"
                isValid = false
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailInputLayout.error = "Formato de correo no válido"
                isValid = false
            } else {
                emailInputLayout.error = null
            }

            // Validar contraseña
            if (password.isEmpty()) {
                passwordInputLayout.error = "La contraseña no puede estar vacía"
                isValid = false
            } else if (password.length < 8) {
                passwordInputLayout.error = "Debe tener al menos 8 caracteres"
                isValid = false
            } else if (!password.matches(Regex("^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@\$!%*?&]+$"))) {
                passwordInputLayout.error = "Debe incluir una mayúscula y un número"
                isValid = false
            } else {
                passwordInputLayout.error = null
            }

            if (isValid) {
                // Iniciar sesión con Firebase
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            val user = auth.currentUser
                            if (user != null && user.isEmailVerified) {
                                Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, HomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            } else {
                                auth.signOut()
                                Toast.makeText(this, "Debes verificar tu correo antes de iniciar sesión", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Redirección al registro
        registerRedirect.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }

        // Redirección para recuperar contraseña
        forgotPasswordLink.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
            // Aquí podrías redirigir a una pantalla de recuperación
        }
    }
}
