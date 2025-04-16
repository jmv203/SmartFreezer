package com.example.smartfreezer.navigation

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.UserProductAdapter
import com.example.smartfreezer.models.UserProduct
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private lateinit var spinnerLocation: Spinner
    private lateinit var tvProductCount: TextView
    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserProductAdapter
    private lateinit var btnFilterOptions: Button

    private var fullItemList: List<UserProduct> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()
    private val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

    private var currentCategoryFilter: String = "Todos"
    private var currentConditionFilter: String = "Todos"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerLocation = view.findViewById(R.id.spinnerLocation)
        tvProductCount = view.findViewById(R.id.tvProductCount)
        searchBar = view.findViewById(R.id.searchBar)
        recyclerView = view.findViewById(R.id.rvInventory)
        btnFilterOptions = view.findViewById(R.id.btnFilterOptions)

        adapter = UserProductAdapter(emptyList())
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        setupSpinners()

        spinnerLocation.onItemSelectedListener = spinnerListener
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilters()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        btnFilterOptions.setOnClickListener { showPopupMenu(it) }

        // Saludo al usuario
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userRef = firestore.collection("users").document(user.uid)
            userRef.get().addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val userName = document.getString("name")
                    view.findViewById<TextView>(R.id.tvGreeting).text = "Hola, $userName"
                }
            }
        }

        val fabAddProduct = view.findViewById<View>(R.id.fabAddProduct)
        fabAddProduct.setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_selectCategoryFragment)
        }

        loadItemsFromFirestore()
    }

    private fun setupSpinners() {
        val locations = listOf("Todos", "nevera", "congelador", "despensa")
        spinnerLocation.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, locations)
    }

    private fun loadItemsFromFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        val email = user.email ?: return
        val uid = user.uid

        firestore.collection("users")
            .document(uid)
            .collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                val products = snapshot.mapNotNull { doc ->
                    val idProduct = doc.id
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val iconName = doc.getString("icon") ?: return@mapNotNull null
                    val iconRes = resources.getIdentifier(iconName, "drawable", requireContext().packageName)

                    UserProduct(
                        idProduct = idProduct,
                        name = name,
                        icon = iconName,
                        category = doc.getString("category") ?: "",
                        condition = doc.getString("condition") ?: "",
                        location = doc.getString("location") ?: "",
                        expirationDate = doc.getDate("expirationDate"),
                        purchaseDate = doc.getDate("purchaseDate"),
                        idUser = email,
                        nutritionFacts = mapOf(),
                        startSeason = 0,
                        lastSeason = 0
                    ).apply {
                        iconDrawableRes = iconRes
                    }
                }

                fullItemList = products
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar inventario", Toast.LENGTH_SHORT).show()
            }
    }

    private val spinnerListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) = applyFilters()
        override fun onNothingSelected(parent: AdapterView<*>) {}
    }

    private fun applyFilters() {
        val location = spinnerLocation.selectedItem.toString()
        val query = searchBar.text.toString().lowercase()

        val filtered = fullItemList.filter { item ->
            (location == "Todos" || item.location == location) &&
                    (currentCategoryFilter == "Todos" || item.category == currentCategoryFilter) &&
                    (currentConditionFilter == "Todos" || item.condition == currentConditionFilter) &&
                    (query.isBlank() || item.name.lowercase().contains(query))
        }

        adapter.updateData(filtered)
        tvProductCount.text = "${filtered.size} productos encontrados"
    }

    private fun showPopupMenu(anchor: View) {
        val popup = PopupMenu(requireContext(), anchor)
        val inflater: MenuInflater = popup.menuInflater
        inflater.inflate(R.menu.popup_filter_menu, popup.menu)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.filter_fruit -> currentCategoryFilter = "fruta"
                R.id.filter_vegetable -> currentCategoryFilter = "verdura"
                R.id.filter_fresh -> currentConditionFilter = "fresco"
                R.id.filter_rotten -> currentConditionFilter = "podrido"
                R.id.filter_clear -> {
                    currentCategoryFilter = "Todos"
                    currentConditionFilter = "Todos"
                }
            }
            applyFilters()
            true
        }

        popup.show()
    }
}
