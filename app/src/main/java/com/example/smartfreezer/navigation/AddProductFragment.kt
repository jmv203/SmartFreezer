package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class AddProductFragment : Fragment(R.layout.fragment_add_product) {

    private lateinit var locationSpinner: Spinner
    private lateinit var conditionSpinner: Spinner
    private lateinit var saveButton: Button

    private lateinit var categoryTitle: TextView
    private lateinit var productIcon: ImageView
    private lateinit var productName: TextView
    private lateinit var quantityText: TextView
    private lateinit var btnIncrease: Button
    private lateinit var btnDecrease: Button
    private lateinit var btnBack: ImageView

    private var quantity = 1

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val productsCollection = firestore.collection("users")
        .document(auth.currentUser?.uid ?: "")
        .collection("products")

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Inputs
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

        // Botón de retroceso
        btnBack = view.findViewById(R.id.btnBack)

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

            val location = locationSpinner.selectedItem.toString()
            val condition = conditionSpinner.selectedItem.toString()

            // Verificar si existe un producto con los mismos criterios
            productsCollection
                .whereEqualTo("name", productId)
                .whereEqualTo("category", category)
                .whereEqualTo("condition", condition)
                .whereEqualTo("location", location)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        // Ya existe un producto, actualizar la cantidad
                        val document = querySnapshot.documents[0]
                        val currentQuantity = document.getLong("quantity") ?: 0
                        val newQuantity = currentQuantity + quantity

                        document.reference
                            .update("quantity", newQuantity)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Cantidad actualizada", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al actualizar cantidad", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // No existe el producto, crear uno nuevo
                        val productMap = hashMapOf(
                            "location" to location,
                            "condition" to condition,
                            "quantity" to quantity,
                            "name" to productId,
                            "icon" to iconStr,
                            "category" to category
                        )

                        productsCollection.add(productMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(), "Producto guardado", Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), "Error al guardar", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Error al verificar el producto", Toast.LENGTH_SHORT).show()
                }
        }

        // Lógica para la flecha de retroceso
        btnBack.setOnClickListener {
            findNavController().navigateUp() // Vuelve al Fragment anterior en la pila (SelectProductFragment)
        }
    }

    private fun updateQuantityDisplay() {
        quantityText.text = quantity.toString()
    }
}