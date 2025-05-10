package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.smartfreezer.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.skydoves.powerspinner.PowerSpinnerView


class AddProductFragment : Fragment(R.layout.fragment_add_product) {

    private lateinit var locationSpinner: PowerSpinnerView
    private lateinit var conditionSpinner: PowerSpinnerView
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
        btnBack = view.findViewById(R.id.btnBackAddProduct)

        val locations = listOf("Nevera", "Congelador", "Despensa")
        val conditions = listOf("Fresco", "Podrido")

        // Configurar PowerSpinners
        locationSpinner.setItems(locations)
        conditionSpinner.setItems(conditions)

        // Mostrar/ocultar manualmente
        locationSpinner.setOnClickListener {
            if (locationSpinner.isShowing) locationSpinner.dismiss() else locationSpinner.show()
        }

        conditionSpinner.setOnClickListener {
            if (conditionSpinner.isShowing) conditionSpinner.dismiss() else conditionSpinner.show()
        }

        // Cerrar al tocar fuera
        locationSpinner.setOnSpinnerOutsideTouchListener { _, _ ->
            locationSpinner.dismiss()
        }

        conditionSpinner.setOnSpinnerOutsideTouchListener { _, _ ->
            conditionSpinner.dismiss()
        }



        // Personalizar comportamiento de los spinners
        locationSpinner.apply {
            setOnSpinnerItemSelectedListener<String> { _, _, _, newItem ->
                val capitalizedLocation = newItem.replaceFirstChar { it.uppercase() }
                locationSpinner.text = capitalizedLocation
            }
        }

        conditionSpinner.apply {
            setOnSpinnerItemSelectedListener<String> { _, _, _, newItem ->
                val capitalizedCondition = newItem.replaceFirstChar { it.uppercase() }
                conditionSpinner.text = capitalizedCondition
            }
        }

        // Obtener argumentos
        val productId = arguments?.getString("productId") ?: return
        val iconStr = arguments?.getString("icon") ?: ""
        val category = arguments?.getString("category") ?: ""

        // Mostrar info producto
        categoryTitle.text = category.replaceFirstChar { it.uppercase() }
        productName.text = productId.replaceFirstChar { it.uppercase() }
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



            // Obtener los valores seleccionados como String
            val selectedLocation = locationSpinner.text.toString().takeIf { it.isNotBlank() } ?: run {
                Toast.makeText(requireContext(),
                    getString(R.string.selecciona_una_ubicaci_n), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedCondition = conditionSpinner.text.toString().takeIf { it.isNotBlank() } ?: run {
                Toast.makeText(requireContext(),
                    getString(R.string.selecciona_una_condici_n), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }



            // Verificar si existe un producto con los mismos criterios
            productsCollection
                .whereEqualTo("name", productId)
                .whereEqualTo("category", category)
                .whereEqualTo("condition", selectedCondition.lowercase())
                .whereEqualTo("location", selectedLocation.lowercase())
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
                                Toast.makeText(requireContext(),
                                    getString(R.string.cantidad_actualizada), Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(),
                                    getString(R.string.error_al_actualizar_cantidad), Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // No existe el producto, crear uno nuevo
                        val productMap = hashMapOf(
                            "location" to selectedLocation.lowercase(),
                            "condition" to selectedCondition.lowercase(),
                            "quantity" to quantity,
                            "name" to productId,
                            "icon" to iconStr,
                            "category" to category
                        )

                        productsCollection.add(productMap)
                            .addOnSuccessListener {
                                Toast.makeText(requireContext(),
                                    getString(R.string.producto_guardado), Toast.LENGTH_SHORT).show()
                                findNavController().navigateUp()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(),
                                    getString(R.string.error_al_guardar), Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(),
                        getString(R.string.error_al_verificar_el_producto), Toast.LENGTH_SHORT).show()
                }
        }

        // Lógica para la flecha de retroceso
        btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateQuantityDisplay() {
        quantityText.text = quantity.toString()
    }
}