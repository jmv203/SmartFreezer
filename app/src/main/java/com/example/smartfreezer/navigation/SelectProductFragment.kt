package com.example.smartfreezer.navigation

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.BasicProductAdapter
import com.example.smartfreezer.models.BasicProduct
import com.google.firebase.firestore.FirebaseFirestore

class SelectProductFragment : Fragment(R.layout.fragment_select_product) {

    private lateinit var recyclerView: RecyclerView
    private val firestore = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerProducts)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val category = arguments?.getString("category")?.lowercase() ?: return
        Log.d("SelectProductFragment", "Categoría recibida: $category")

        firestore.collection("basic_products")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { result ->
                // Mapeo de los datos a BasicProduct
                val products = result.map { document ->
                    val name = document.getString("name") ?: ""
                    val iconStr = document.getString("icon") ?: ""
                    BasicProduct(name, iconStr) // Aquí estamos usando la clase BasicProduct
                }

                // Configuramos el adaptador con la lista de productos y la acción del clic
                recyclerView.adapter = BasicProductAdapter(products) { selectedProduct ->
                    val action = SelectProductFragmentDirections
                        .actionSelectProductFragmentToAddProductFragment(productId = selectedProduct.name, icon = selectedProduct.icon,
                            category = category)
                    findNavController().navigate(action)
                }
            }
    }
}
