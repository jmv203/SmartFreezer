package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.smartfreezer.databinding.ActivityProfileBinding
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class ProfileActivity : BaseActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var binding: ActivityProfileBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnSettingsProfile.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        // Inicializar Firebase
        auth = Firebase.auth
        db = Firebase.firestore
        userId = auth.currentUser?.uid ?: ""

        // Cargar datos del usuario
        loadUserData()

        // Configurar listeners para los botones
        setupClickListeners()
    }


    private fun loadUserData() {
        // Obtener datos de Firestore
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Mostrar nombre
                    val name = document.getString("name") ?: "Usuario"
                    binding.userNameTextView.text = name
                    binding.tvGreetingProfile.text = getString(R.string.hola, name)

                    // Mostrar preferencias
                    val vegan = document.getBoolean("vegan") ?: false
                    val vegetarian = document.getBoolean("vegetarian") ?: false
                    val glutenFree = document.getBoolean("glutenFree") ?: false
                    val dairyFree = document.getBoolean("dairyFree") ?: false

                    val dietText = buildString {
                        if (vegan) append(getString(R.string.vegano))
                        else if (vegetarian) append(getString(R.string.vegetariano))
                        else append(getString(R.string.omn_voro))
                    }

                    val intolerancesText = buildString {
                        val intolerances = mutableListOf<String>()
                        if (glutenFree) intolerances.add(getString(R.string.gluten))
                        if (dairyFree) intolerances.add(getString(R.string.lactosa))

                        if (intolerances.isEmpty()) {
                            append(getString(R.string.ninguna))
                        } else {
                            append(intolerances.joinToString(", "))
                        }
                    }

                    binding.dietTextView.text = getString(R.string.dieta, dietText)
                    binding.intolerancesTextView.text =
                        getString(R.string.intolerancias, intolerancesText)
                }
            }
            .addOnFailureListener { e ->
                Log.w("ProfileActivity", getString(R.string.error_cargando_datos_del_usuario), e)
            }

        // Mostrar email de Authentication
        auth.currentUser?.email?.let { email ->
            binding.emailTextView.text = email
        }
    }

    private fun setupClickListeners() {
        // Modificar nombre
        binding.editNameOption.setOnClickListener {
            showEditNameDialog()
        }

        // Cambiar correo electrónico
        binding.changeEmailOption.setOnClickListener {
            showChangeEmailDialog()
        }

        // Restablecer contraseña
        binding.resetPasswordOption.setOnClickListener {
            showResetPasswordDialog()
        }

        // Modificar preferencias
        binding.editPreferencesOption.setOnClickListener {
            showEditPreferencesDialog()
        }

        // Cerrar sesión
        binding.logoutOption.setOnClickListener {
            showLogoutConfirmationDialog()
        }

        // Eliminar cuenta
        binding.deleteAccountOption.setOnClickListener {
            showDeleteAccountConfirmationDialog()
        }
    }

    private fun showEditNameDialog() {
        val currentName = binding.userNameTextView.text.toString()

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDialog)
        val editHead = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.editHeadDialog)
        editText.setText(currentName)
        editHead.hint = getString(R.string.nombre)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.modificar_nombre))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.guardar)) { dialog, _ ->
                val newName = editText.text.toString().trim()
                if (newName.isNotEmpty() && newName != currentName) {
                    updateUserName(newName)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun updateUserName(newName: String) {
        showProgressDialog(getString(R.string.actualizando_nombre))

        db.collection("users").document(userId)
            .update("name", newName)
            .addOnSuccessListener {
                dismissProgressDialog()
                binding.userNameTextView.text = newName
                binding.tvGreetingProfile.text = getString(R.string.hola, newName)
                showSuccessSnackbar(getString(R.string.nombre_actualizado_correctamente))
            }
            .addOnFailureListener { e ->
                dismissProgressDialog()
                showErrorSnackbar(getString(R.string.error_al_actualizar_el_nombre, e.message))
            }
    }

    private fun showChangeEmailDialog() {
        val currentEmail = binding.emailTextView.text.toString()

        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDialog)
        editText.setText(currentEmail)
        editText.hint = getString(R.string.nuevo_correo_electr_nico)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cambiar_correo_electr_nico))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.cambiar)) { dialog, _ ->
                val newEmail = editText.text.toString().trim()
                if (newEmail.isNotEmpty() && newEmail != currentEmail) {
                    showPasswordConfirmationDialog(getString(R.string.para_cambiar_tu_correo_confirma_tu_contrase_a_actual)) { password ->
                        changeUserEmail(newEmail, password)
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun changeUserEmail(newEmail: String, password: String) {
        showProgressDialog(getString(R.string.verificando_credenciales))

        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    updateProgressDialog(getString(R.string.enviando_correo_de_verificaci_n))

                    user.verifyBeforeUpdateEmail(newEmail)
                        .addOnCompleteListener { task ->
                            dismissProgressDialog()

                            if (task.isSuccessful) {
                                showEmailVerificationDialog(newEmail)
                            } else {
                                showErrorSnackbar(getString(
                                    R.string.error_al_enviar_correo,
                                    task.exception?.message ?: getString(R.string.error_desconocido)
                                ))
                            }
                        }
                } else {
                    dismissProgressDialog()
                    showErrorSnackbar(getString(R.string.contrase_a_incorrecta))
                }
            }
    }

    private fun showEmailVerificationDialog(newEmail: String) {
        val dialogBuilder = MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.verificaci_n_requerida))
            .setMessage(
                getString(
                    R.string.hemos_enviado_un_correo_de_verificaci_n_a_por_favor,
                    newEmail
                ))
            .setPositiveButton(getString(R.string.entendido), null)
            .setNeutralButton(getString(R.string.reenviar_correo), null)
            .setNegativeButton(getString(R.string.cancelar), null)
            .setCancelable(false)

        val dialog = dialogBuilder.create()

        dialog.setOnShowListener {
            val btnPositive = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val btnNeutral = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            val btnNegative = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)

            btnPositive.setOnClickListener {
                updateEmailInFirestore(newEmail)
                dialog.dismiss()
            }

            btnNeutral.setOnClickListener {
                resendVerificationEmail(newEmail)
                Snackbar.make(binding.root,
                    getString(R.string.correo_reenviado_a, newEmail), Snackbar.LENGTH_SHORT).show()
            }

            btnNegative.setOnClickListener {
                dialog.dismiss()
            }
        }

        dialog.show()
    }


    private fun resendVerificationEmail(newEmail: String) {
        showProgressDialog(getString(R.string.reenviando_correo))

        val user = auth.currentUser ?: return

        user.verifyBeforeUpdateEmail(newEmail)
            .addOnCompleteListener { task ->
                dismissProgressDialog()

                if (task.isSuccessful) {
                    Toast.makeText(
                        this,
                        getString(R.string.correo_de_verificaci_n_reenviado_a, newEmail),
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.error_al_reenviar, task.exception?.message),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun updateEmailInFirestore(newEmail: String) {
        showProgressDialog(getString(R.string.actualizando_datos))

        db.collection("users").document(userId)
            .update("email", newEmail)
            .addOnSuccessListener {
                dismissProgressDialog()
                binding.emailTextView.text = newEmail
                showEmailChangeSuccessDialog()
            }
            .addOnFailureListener { e ->
                dismissProgressDialog()
                Toast.makeText(
                    this,
                    getString(R.string.error_al_actualizar_el_correo_en_firestore, e.message),
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun showEmailChangeSuccessDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cambio_completado))
            .setMessage(getString(R.string.tu_correo_electr_nico_se_ha_actualizado_correctamente_para_continuar_por_favor_vuelve_a_iniciar_sesi_n_con_tu_nuevo_correo))
            .setPositiveButton(getString(R.string.entendido)) { dialog, _ ->
                logoutUser()
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }


    private fun showResetPasswordDialog() {
        val passwordDialogView = layoutInflater.inflate(R.layout.dialog_password_confirmation, null)
        val passwordEditText = passwordDialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextPassword)


        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirmaci_n))
            .setMessage(getString(R.string.para_cambiar_tu_contrase_a_confirma_tu_contrase_a_actual))
            .setView(passwordDialogView)
            .setPositiveButton(getString(R.string.confirmar), null)
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .also { dialog ->
                dialog.setOnShowListener {
                    val confirmButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                    confirmButton.setOnClickListener {
                        val currentPassword = passwordEditText.text.toString().trim()

                        if (currentPassword.isEmpty()) {
                            Toast.makeText(this,
                                getString(R.string.la_contrase_a_no_puede_estar_vac_a), Toast.LENGTH_SHORT).show()
                            return@setOnClickListener
                        }

                        val user = auth.currentUser
                        val credential = EmailAuthProvider.getCredential(user?.email ?: "", currentPassword)

                        showProgressDialog(getString(R.string.verificando_contrase_a))

                        user?.reauthenticate(credential)?.addOnCompleteListener { reauthTask ->
                            dismissProgressDialog()
                            if (reauthTask.isSuccessful) {
                                dialog.dismiss()
                                showNewPasswordDialog(currentPassword)
                            } else {
                                Toast.makeText(this, getString(R.string.contrase_a_incorrecta), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                dialog.show()
            }
    }

    private fun showNewPasswordDialog(currentPassword: String) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_text, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextDialog)
        val editHead = dialogView.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.editHeadDialog)
        editText.hint = getString(R.string.nueva_contrase_a)
        editHead.hint = getString(R.string.nueva_contrase_a)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.restablecer_contrase_a))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.cambiar)) { dialog, _ ->
                val newPassword = editText.text.toString().trim()
                if (newPassword.isNotEmpty()) {
                    resetPassword(currentPassword, newPassword)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }



    private var progressDialog: AlertDialog? = null

    private fun showProgressDialog(message: String) {
        progressDialog?.dismiss()

        progressDialog = MaterialAlertDialogBuilder(this)
            .setView(R.layout.dialog_progress)
            .setMessage(message)
            .setCancelable(false)
            .show()
    }

    private fun updateProgressDialog(message: String) {
        progressDialog?.findViewById<TextView>(R.id.message)?.text = message
    }

    private fun dismissProgressDialog() {
        progressDialog?.dismiss()
        progressDialog = null
    }

    private fun resetPassword(currentPassword: String, newPassword: String) {
        showProgressDialog(getString(R.string.actualizando_contrase_a))

        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                dismissProgressDialog()
                                showSuccessSnackbar(getString(R.string.contrase_a_actualizada_correctamente))
                            } else {
                                dismissProgressDialog()
                                showErrorSnackbar(getString(
                                    R.string.error_al_actualizar_la_contrase_a,
                                    updateTask.exception?.message ?: getString(R.string.error_desconocido)
                                ))
                            }
                        }
                } else {
                    dismissProgressDialog()
                    showErrorSnackbar(getString(R.string.contrase_a_actual_incorrecta))
                }
            }
    }

    private fun showEditPreferencesDialog() {
        // Obtener preferencias actuales
        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val vegan = document.getBoolean("vegan") ?: false
                    val vegetarian = document.getBoolean("vegetarian") ?: false
                    val glutenFree = document.getBoolean("glutenFree") ?: false
                    val dairyFree = document.getBoolean("dairyFree") ?: false

                    val items = arrayOf(
                        getString(R.string.vegano),
                        getString(R.string.vegetariano),
                        getString(R.string.sin_gluten),
                        getString(R.string.sin_lactosa)
                    )

                    val checkedItems = booleanArrayOf(
                        vegan,
                        vegetarian,
                        glutenFree,
                        dairyFree
                    )

                    MaterialAlertDialogBuilder(this)
                        .setTitle(getString(R.string.modificar_preferencias))
                        .setMultiChoiceItems(items, checkedItems) { _, which, isChecked ->
                            checkedItems[which] = isChecked
                        }
                        .setPositiveButton(getString(R.string.guardar)) { dialog, _ ->
                            val updates = hashMapOf<String, Any>(
                                "vegan" to checkedItems[0],
                                "vegetarian" to checkedItems[1],
                                "glutenFree" to checkedItems[2],
                                "dairyFree" to checkedItems[3]
                            )

                            updateUserPreferences(updates)
                            dialog.dismiss()
                        }
                        .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }
    }

    private fun updateUserPreferences(preferences: Map<String, Any>) {
        db.collection("users").document(userId)
            .update(preferences)
            .addOnSuccessListener {
                loadUserData() // Recargar datos para mostrar los cambios
                Toast.makeText(this,
                    getString(R.string.preferencias_actualizadas_correctamente), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this,
                    getString(R.string.error_al_actualizar_preferencias, e.message), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLogoutConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.cerrar_sesi_n))
            .setMessage(getString(R.string.est_s_seguro_de_que_quieres_cerrar_sesi_n))
            .setPositiveButton(getString(R.string.s)) { dialog, _ ->
                logoutUser()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun logoutUser() {
        auth.signOut()
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showDeleteAccountConfirmationDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.eliminar_cuenta))
            .setMessage(getString(R.string.est_s_seguro_de_que_quieres_eliminar_tu_cuenta_esta_acci_n_no_se_puede_deshacer))
            .setPositiveButton(getString(R.string.eliminar)) { dialog, _ ->
                showPasswordConfirmationDialog(getString(R.string.para_eliminar_tu_cuenta_confirma_tu_contrase_a)) { password ->
                    deleteUserAccount(password)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteUserAccount(password: String) {
        val user = auth.currentUser ?: return
        val credential = EmailAuthProvider.getCredential(user.email!!, password)
        val userDocRef = db.collection("users").document(user.uid)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {

                    // Paso 1: Eliminar subcolecciones manualmente
                    val collectionsToDelete = listOf("products", "savedRecipes", "shopping_list", "wasted_products")

                    val deletionTasks = collectionsToDelete.map { collection ->
                        userDocRef.collection(collection).get()
                            .continueWithTask { task ->
                                val batch = db.batch()
                                task.result?.documents?.forEach { doc ->
                                    batch.delete(doc.reference)
                                }
                                batch.commit()
                            }
                    }

                    // Paso 2: Esperar a que se borren todas las subcolecciones
                    Tasks.whenAllComplete(deletionTasks)
                        .addOnSuccessListener {
                            // Paso 3: Eliminar documento del usuario
                            userDocRef.delete()
                                .addOnSuccessListener {
                                    // Paso 4: Eliminar cuenta de Authentication
                                    user.delete()
                                        .addOnCompleteListener { deleteTask ->
                                            if (deleteTask.isSuccessful) {
                                                Toast.makeText(this,
                                                    getString(R.string.cuenta_eliminada_correctamente), Toast.LENGTH_SHORT).show()
                                                val intent = Intent(this, MainActivity::class.java)
                                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                                startActivity(intent)
                                                finish()
                                            } else {
                                                Toast.makeText(this,
                                                    getString(R.string.error_al_eliminar_la_cuenta,
                                                        deleteTask.exception?.message), Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this,
                                        getString(R.string.error_al_eliminar_datos_del_usuario, e.message), Toast.LENGTH_SHORT).show()
                                }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this,
                                getString(R.string.error_al_eliminar_datos_del_usuario, e.message), Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, getString(R.string.contrase_a_incorrecta), Toast.LENGTH_SHORT).show()
                }
            }
    }

    //Funciones para evitar operaciones en email,name y password
    private fun showSuccessSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.green))
            .show()
    }

    private fun showErrorSnackbar(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG)
            .setBackgroundTint(ContextCompat.getColor(this, R.color.error))
            .show()
    }


    private fun showPasswordConfirmationDialog(message: String, onSuccess: (String) -> Unit) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_password_confirmation, null)
        val editText = dialogView.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editTextPassword)

        MaterialAlertDialogBuilder(this)
            .setTitle(getString(R.string.confirmaci_n))
            .setMessage(message)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.confirmar)) { dialog, _ ->
                val password = editText.text.toString().trim()
                if (password.isNotEmpty()) {
                    onSuccess(password)
                } else {
                    Toast.makeText(this, getString(R.string.la_contrase_a_no_puede_estar_vac_a), Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}