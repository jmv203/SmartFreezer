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

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Inicializar Firebase Auth
        auth = FirebaseAuth.getInstance()

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

            // Validar email (máx. 40 caracteres y formato correcto)
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

            // Validar contraseña (mín. 8 caracteres, 1 mayúscula y 1 número)
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

            // Si todo está correcto, proceder con el inicio de sesión
            if (isValid) {
                // Iniciar sesión con Firebase Authentication
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Inicio de sesión exitoso
                            Toast.makeText(this, "Inicio de sesión exitoso", Toast.LENGTH_SHORT).show()
                            // Redirigir a la pantalla principal
                            val intent = Intent(this, HomeActivity::class.java)
                            startActivity(intent)
                            finish() // Cerrar la actividad de inicio de sesión
                        } else {
                            // Error al iniciar sesión
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Manejar la redirección al registro
        registerRedirect.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish() // Cierra esta actividad para evitar volver atrás
        }

        // Manejar el clic en "Olvidé mi contraseña"
        forgotPasswordLink.setOnClickListener {
            Toast.makeText(this, "Recuperar contraseña", Toast.LENGTH_SHORT).show()
            // Aquí podrías agregar la lógica para redirigir a una pantalla de recuperación de contraseña
        }
    }
}
