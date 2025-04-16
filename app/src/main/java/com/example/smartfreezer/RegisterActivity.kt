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

class RegisterActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore // Inicializamos Firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Inicializar Firebase Auth y Firestore
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Referencias a los elementos del layout
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

        // Manejar el clic del botón de registro
        registerButton.setOnClickListener {
            val name = nameInput.text.toString().trim()
            val email = emailInput.text.toString().trim()
            val password = passwordInput.text.toString().trim()
            val confirmPassword = confirmPasswordInput.text.toString().trim()

            var isValid = true

            // Validar nombre (máx. 15 caracteres, sin caracteres especiales)
            if (name.isEmpty()) {
                nameInputLayout.error = "El nombre no puede estar vacío"
                isValid = false
            } else if (!name.matches(Regex("^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]{1,15}$"))) {
                nameInputLayout.error = "Máximo 15 caracteres, sin símbolos"
                isValid = false
            } else {
                nameInputLayout.error = null
            }

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

            // Validar confirmación de contraseña
            if (confirmPassword.isEmpty()) {
                confirmPasswordInputLayout.error = "Confirma tu contraseña"
                isValid = false
            } else if (confirmPassword != password) {
                confirmPasswordInputLayout.error = "Las contraseñas no coinciden"
                isValid = false
            } else {
                confirmPasswordInputLayout.error = null
            }

            // Validar checkbox de términos
            if (!acceptTermsCheckBox.isChecked) {
                Toast.makeText(this, "Debes aceptar los términos y condiciones", Toast.LENGTH_LONG).show()
                isValid = false
            }

            // Si todo está correcto, proceder con el registro
            if (isValid) {
                // Registro de usuario en Firebase
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            // Registro exitoso
                            val user = auth.currentUser
                            val uuid = user?.uid ?: ""

                            // Crear un mapa con los datos del usuario
                            val userMap = hashMapOf(
                                "name" to name,
                                "email" to email,
                                "uuid" to uuid
                            )

                            // Guardar los datos en Firestore
                            db.collection("users")
                                .document(uuid) // Usamos el UUID como ID de documento
                                .set(userMap)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Datos guardados correctamente", Toast.LENGTH_SHORT).show()

                                    // Redirigir al LoginActivity
                                    val intent = Intent(this, LoginActivity::class.java)
                                    startActivity(intent)
                                    finish()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Error al guardar los datos: ${e.message}", Toast.LENGTH_LONG).show()
                                }

                        } else {
                            // Error al registrar el usuario
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
            }
        }

        // Manejar la redirección al login
        loginRedirect.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish() // Cierra esta actividad para evitar volver atrás
        }
    }
}
