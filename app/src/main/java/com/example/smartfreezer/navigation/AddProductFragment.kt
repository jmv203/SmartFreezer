package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AddProductFragment : Fragment(R.layout.fragment_add_product) {

    private lateinit var purchaseInput: EditText
    private lateinit var expirationInput: EditText
    private lateinit var locationSpinner: Spinner
    private lateinit var conditionSpinner: Spinner
    private lateinit var saveButton: Button

    private lateinit var categoryTitle: TextView
    private lateinit var productIcon: ImageView
    private lateinit var productName: TextView
    private lateinit var quantityText: TextView
    private lateinit var btnIncrease: Button
    private lateinit var btnDecrease: Button

    private var quantity = 1

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inputs
        purchaseInput = view.findViewById(R.id.purchaseDateInput)
        expirationInput = view.findViewById(R.id.expirationDateInput)
        locationSpinner = view.findViewById(R.id.locationSpinner)
        conditionSpinner = view.findViewById(R.id.conditionSpinner)
        saveButton = view.findViewById(R.id.saveButton)

        // Info del producto
        categoryTitle = view.findViewById(R.id.categoryTitle)
        productIcon = view.findViewById(R.id.productIcon)
        productName = view.findViewById(R.id.productName)

        // Contador
        quantityText = view.findViewById(R.id.quantityText)
        btnIncrease = view.findViewById(R.id.btnIncrease)
        btnDecrease = view.findViewById(R.id.btnDecrease)

        val locations = listOf("nevera", "congelador", "despensa")
        val conditions = listOf("fresco", "podrido")

        locationSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locations)
        conditionSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, conditions)

        // Obtener argumentos
        val productId = arguments?.getString("productId") ?: return
        val iconStr = arguments?.getString("icon") ?: ""
        val category = arguments?.getString("category") ?: ""

        // Mostrar info producto
        categoryTitle.text = category
        productName.text = productId
        val iconResId = resources.getIdentifier(iconStr.removePrefix("@drawable/"), "drawable", requireContext().packageName)
        productIcon.setImageResource(iconResId)

        // Lógica cantidad
        updateQuantityDisplay()
        btnIncrease.setOnClickListener {
            quantity++
            updateQuantityDisplay()
        }

        btnDecrease.setOnClickListener {
            if (quantity > 1) {
                quantity--
                updateQuantityDisplay()
            }
        }

        saveButton.setOnClickListener {
            val user = auth.currentUser ?: return@setOnClickListener
            val uid = user.uid

            val purchaseDate = purchaseInput.text.toString().toDate()
            val expirationDate = expirationInput.text.toString().toDate()
            val location = locationSpinner.selectedItem.toString()
            val condition = conditionSpinner.selectedItem.toString()

            val productMap = hashMapOf(
                "purchaseDate" to purchaseDate,
                "expirationDate" to expirationDate,
                "location" to location,
                "condition" to condition,
                "quantity" to quantity,
                "name" to productId,
                "icon" to iconStr,
                "category" to category
            )

            // Generar ID único para producto individual
            firestore.collection("users")
                .document(uid)
                .collection("products")
                .add(productMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                    findNavController().navigateUp()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateQuantityDisplay() {
        quantityText.text = quantity.toString()
    }

    private fun String.toDate(): Date? {
        return try {
            SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(this)
        } catch (e: Exception) {
            null
        }
    }
}
