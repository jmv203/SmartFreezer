package com.example.smartfreezer.navigation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.BasicProductAdapter
import com.example.smartfreezer.models.BasicProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddShoppingListFragment : Fragment(R.layout.fragment_add_shopping_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BasicProductAdapter
    private lateinit var searchBar: EditText
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var productList: List<BasicProduct> = emptyList()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvBasicProducts)
        searchBar = view.findViewById(R.id.searchBar)
        val btnBack = view.findViewById<ImageView>(R.id.btnBackAddShoppingProduct)

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        adapter = BasicProductAdapter(emptyList()) { product ->
            addProductToShoppingList(product)
        }
        recyclerView.adapter = adapter

        loadProductsFromFirestore()

        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = filterProducts(s.toString())
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun loadProductsFromFirestore() {
        firestore.collection("basic_products")
            .orderBy("name") // orden alfabético por nombre
            .get()
            .addOnSuccessListener { snapshot ->
                productList = snapshot.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val icon = doc.getString("icon") ?: return@mapNotNull null
                    BasicProduct(name = name, icon = icon)
                }
                adapter.updateData(productList)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_cargar_productos), Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterProducts(query: String) {
        val filteredList = productList.filter {
            it.name.contains(query, ignoreCase = true)
        }
        adapter.updateData(filteredList)
    }

    private fun addProductToShoppingList(product: BasicProduct) {
        val user = auth.currentUser ?: return
        val shoppingListRef = firestore
            .collection("users")
            .document(user.uid)
            .collection("shopping_list")
            .document(product.name) // Usamos el name como ID

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(shoppingListRef)
            val currentQuantity = snapshot.getLong("quantity") ?: 0
            val newQuantity = currentQuantity + 1

            val data = mapOf(
                "name" to product.name,
                "icon" to product.icon,
                "quantity" to newQuantity
            )
            transaction.set(shoppingListRef, data)
        }.addOnSuccessListener {
            context?.let {
                Toast.makeText(it,
                    getString(R.string.a_adido_a_la_lista, product.name), Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()}
        }.addOnFailureListener {
            Toast.makeText(requireContext(),
                getString(R.string.error_al_a_adir_producto), Toast.LENGTH_SHORT).show()
        }
    }
}
