package com.example.smartfreezer.navigation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.adapters.ShoppingListAdapter
import com.example.smartfreezer.models.ShoppingItem
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ShoppingListFragment : Fragment(R.layout.fragment_shopping_list) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ShoppingListAdapter
    private lateinit var tvProductCount: TextView
    private lateinit var btnAddToInventory: Button
    private lateinit var fabAddProduct: FloatingActionButton
    private lateinit var btnAccountShoppingList: ImageView
    private lateinit var btnSettingsShoppingList : ImageView
    private lateinit var tvEmptyShoppingList : TextView


    private val shoppingList = mutableListOf<ShoppingItem>()
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnAccountShoppingList = view.findViewById(R.id.btnAccountShoppingList)
        btnSettingsShoppingList = view.findViewById(R.id.btnSettingsShoppingList)

        btnAccountShoppingList.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        btnSettingsShoppingList.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

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

        recyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = adapter

        fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_listFragment_to_addShoppingListFragment)
        }

        setupGreeting(view)

        loadShoppingList()
        tvEmptyShoppingList = view.findViewById(R.id.tvEmptyShoppingList)
        btnAddToInventory = view.findViewById(R.id.btnAddToInventory)
        btnAddToInventory.setOnClickListener {
            showAddToInventoryConfirmation()
        }

    }

    private fun setupGreeting(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    view.findViewById<TextView>(R.id.tvGreetingShoppingList).text = getString(R.string.hola, name)
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
                    val category = doc.getString("category") ?: continue
                    shoppingList.add(ShoppingItem(name = name, icon = icon, quantity = quantity , category = category))
                }
                adapter.notifyDataSetChanged()
                updateProductCount()
                checkEmptyState()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_cargar_la_lista), Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkEmptyState() {
        if (shoppingList.isEmpty()) {
            val topDrawable = ContextCompat.getDrawable(requireContext(), R.drawable.ic_empty_box)
            topDrawable?.setBounds(0, 0, 128, 128)
            tvEmptyShoppingList.setCompoundDrawables(null, topDrawable, null, null)
            tvEmptyShoppingList.visibility = View.VISIBLE
        } else {
            tvEmptyShoppingList.visibility = View.GONE
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
                checkEmptyState()
            }
    }

    @SuppressLint("StringFormatMatches")
    private fun updateProductCount() {
        val total = shoppingList.sumOf { it.quantity }
        tvProductCount.text = getString(R.string.productos, total)
    }

    private fun showAddToInventoryConfirmation() {
        if (shoppingList.isEmpty()) {
            Toast.makeText(requireContext(),
                getString(R.string.la_lista_de_compras_est_vac_a), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.confirmar))
            .setMessage(getString(R.string.deseas_a_adir_todos_los_productos_comprados_al_inventario))
            .setPositiveButton(getString(R.string.a_adir)) { _, _ ->
                addToInventory()
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun addToInventory() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val productsCollection = firestore.collection("users")
            .document(user.uid)
            .collection("products")

        val shoppingCollection = firestore.collection("users")
            .document(user.uid)
            .collection("shopping_list")

        shoppingList.forEach { item ->
            // Comprobamos si ya existe un producto igual
            productsCollection
                .whereEqualTo("name", item.name)
                .whereEqualTo("category", item.category)
                .whereEqualTo("condition", "fresco")
                .whereEqualTo("location", "nevera")
                .get()
                .addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        // Existe: actualizamos la cantidad
                        val document = querySnapshot.documents[0]
                        val currentQuantity = document.getLong("quantity") ?: 0
                        val newQuantity = currentQuantity + item.quantity

                        document.reference
                            .update("quantity", newQuantity)
                            .addOnSuccessListener {
                                // Eliminamos de la lista de la compra
                                shoppingCollection.document(item.name).delete()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), getString(R.string.error_al_actualizar_cantidad), Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // No existe: lo añadimos al inventario
                        val newProduct = hashMapOf(
                            "name" to item.name,
                            "icon" to item.icon,
                            "quantity" to item.quantity,
                            "category" to item.category,
                            "condition" to "fresco",
                            "location" to "nevera"
                        )

                        productsCollection.add(newProduct)
                            .addOnSuccessListener {
                                // Eliminamos de la lista de la compra
                                shoppingCollection.document(item.name).delete()
                            }
                            .addOnFailureListener {
                                Toast.makeText(requireContext(), getString(R.string.error_al_guardar), Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.error_al_verificar_el_producto), Toast.LENGTH_SHORT).show()
                }
        }

        Toast.makeText(requireContext(), getString(R.string.productos_a_adidos_al_inventario), Toast.LENGTH_SHORT).show()
        loadShoppingList()
    }

}
