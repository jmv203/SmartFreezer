package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.ShoppingListAdapter
import com.example.smartfreezer.models.ShoppingItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var tvProductCount: TextView
    private lateinit var fabAddProduct: FloatingActionButton

    private val shoppingList = mutableListOf<ShoppingItem>()
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rvShoppingList)
        tvProductCount = view.findViewById(R.id.tvProductCountShoppingList)
        fabAddProduct = view.findViewById(R.id.fabAddProductShoppingList)

        adapter = ShoppingListAdapter(shoppingList,
            onIncrease = { item ->
                item.quantity++
                updateItemInFirestore(item)
            },
            onDecrease = { item ->
                if (item.quantity > 1) {
                    item.quantity--
                    updateItemInFirestore(item)
                } else {
                    deleteItemFromFirestore(item)
                }
            })

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        recyclerView.adapter = adapter

        fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addShoppingListFragment)
        }

        setupGreeting(view)
        loadShoppingList()
    }

    private fun setupGreeting(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    view.findViewById<TextView>(R.id.tvGreetingShoppingList).text = "Hola, $name"
                }
            }
    }

    private fun loadShoppingList() {
        val uid = user?.uid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("shopping_list")
            .get()
            .addOnSuccessListener { snapshot ->
                shoppingList.clear()
                for (doc in snapshot) {
                    val name = doc.getString("name") ?: continue
                    val icon = doc.getString("icon") ?: continue
                    val quantity = doc.getLong("quantity")?.toInt() ?: 1
                    shoppingList.add(ShoppingItem(name = name, icon = icon, quantity = quantity))
                }
                adapter.notifyDataSetChanged()
                updateProductCount()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar la lista", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateItemInFirestore(item: ShoppingItem) {
        val uid = user?.uid ?: return
        val ref = firestore.collection("users").document(uid)
            .collection("shopping_list").document(item.name)

        ref.set(mapOf(
            "name" to item.name,
            "icon" to item.icon,
            "quantity" to item.quantity
        )).addOnSuccessListener {
            adapter.notifyDataSetChanged()
            updateProductCount()
        }
    }

    private fun deleteItemFromFirestore(item: ShoppingItem) {
        val uid = user?.uid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("shopping_list")
            .document(item.name)
            .delete()
            .addOnSuccessListener {
                shoppingList.remove(item)
                adapter.notifyDataSetChanged()
                updateProductCount()
            }
    }

    private fun updateProductCount() {
        val total = shoppingList.sumOf { it.quantity }
        tvProductCount.text = "$total productos"
    }
}
