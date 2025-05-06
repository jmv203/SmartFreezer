package com.example.smartfreezer.navigation

import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher

import android.view.*
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
import com.skydoves.powerspinner.PowerSpinnerView

class InventoryFragment : Fragment(R.layout.fragment_inventory) {

    private lateinit var spinnerLocation: PowerSpinnerView
    private lateinit var tvProductCount: TextView
    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserProductAdapter
    private lateinit var btnFilterOptions: Button
    private lateinit var filterBadge: TextView


    private var fullItemList: List<UserProduct> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()

    private var currentCategoryFilter: String = "Todos"
    private var currentConditionFilter: String = "Todos"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        spinnerLocation = view.findViewById(R.id.spinnerLocation)

        tvProductCount = view.findViewById(R.id.tvProductCount)
        searchBar = view.findViewById(R.id.searchBar)
        recyclerView = view.findViewById(R.id.rvInventory)
        btnFilterOptions = view.findViewById(R.id.btnFilterOptions)
        filterBadge = view.findViewById(R.id.filterBadge)


        adapter = UserProductAdapter(emptyList()) { userProduct  ->
            showBasicProductDetails(userProduct.icon)
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        setupSpinner()
        setupSearch()
        setupFilterMenu()

        setupGreeting(view)
        setupAddButton(view)

        loadItemsFromFirestore()
    }

    private fun setupSpinner() {
        val locations = listOf("Todos", "Nevera", "Congelador", "Despensa")
        spinnerLocation.setItems(locations)
        spinnerLocation.selectItemByIndex(0)

        spinnerLocation.setOnClickListener {
            if (spinnerLocation.isShowing) {
                spinnerLocation.dismiss()
            } else {
                spinnerLocation.show()
            }
        }

        spinnerLocation.setOnSpinnerOutsideTouchListener { view, motionEvent ->
            spinnerLocation.dismiss()
        }

        spinnerLocation.setOnSpinnerItemSelectedListener<String> { _, _, _, _ ->
            applyFilters()

        }

    }

    private fun setupSearch() {
        searchBar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = applyFilters()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupFilterMenu() {
        btnFilterOptions.setOnClickListener { showPopupMenu(it) }
    }

    private fun setupGreeting(view: View) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    view.findViewById<TextView>(R.id.tvGreeting).text = "Hola, $name"
                }
            }
    }

    private fun setupAddButton(view: View) {
        view.findViewById<View>(R.id.fabAddProduct).setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_selectCategoryFragment)
        }
    }

    private fun loadItemsFromFirestore() {
        val user = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users")
            .document(user.uid)
            .collection("products")
            .get()
            .addOnSuccessListener { snapshot ->
                fullItemList = snapshot.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    val icon = doc.getString("icon") ?: return@mapNotNull null
                    val iconRes = resources.getIdentifier(icon, "drawable", requireContext().packageName)

                    UserProduct(
                        idProduct = doc.id,
                        name = name,
                        icon = icon,
                        category = doc.getString("category") ?: "",
                        condition = doc.getString("condition") ?: "",
                        location = doc.getString("location") ?: "",
                        idUser = user.email ?: "",
                        nutritionFacts = mapOf()
                    ).apply { iconDrawableRes = iconRes }
                }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar inventario", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val location = spinnerLocation.text.toString().lowercase()
        val query = searchBar.text.toString().lowercase()

        val filtered = fullItemList.filter { item ->
            (location == "todos" || item.location == location) &&
                    (currentCategoryFilter == "Todos" || item.category == currentCategoryFilter) &&
                    (currentConditionFilter == "Todos" || item.condition == currentConditionFilter) &&
                    (query.isBlank() || item.name.lowercase().contains(query))
        }

        adapter.updateData(filtered)
        tvProductCount.text = "${filtered.size} productos encontrados"
    }

    private fun showPopupMenu(anchor: View) {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.PopupMenuInventory)
        val popup = PopupMenu(themedContext, anchor)
        popup.menuInflater.inflate(R.menu.popup_filter_menu, popup.menu)

        // Set the initial checked states if needed (e.g., based on current filters)
        val menu = popup.menu
        menu.findItem(R.id.filter_fruit)?.isChecked = currentCategoryFilter == "fruta"
        menu.findItem(R.id.filter_vegetable)?.isChecked = currentCategoryFilter == "verdura"
        menu.findItem(R.id.filter_fresh)?.isChecked = currentConditionFilter == "fresco"
        menu.findItem(R.id.filter_rotten)?.isChecked = currentConditionFilter == "podrido"

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.filter_fruit -> currentCategoryFilter = if (item.isChecked) "Todos" else "fruta" // Toggle
                R.id.filter_vegetable -> currentCategoryFilter = if (item.isChecked) "Todos" else "verdura" // Toggle
                R.id.filter_fresh -> currentConditionFilter = if (item.isChecked) "Todos" else "fresco" // Toggle
                R.id.filter_rotten -> currentConditionFilter = if (item.isChecked) "Todos" else "podrido" // Toggle
                R.id.filter_clear -> {
                    currentCategoryFilter = "Todos"
                    currentConditionFilter = "Todos"
                    // Uncheck all filter items
                    menu.findItem(R.id.filter_fruit)?.isChecked = false
                    menu.findItem(R.id.filter_vegetable)?.isChecked = false
                    menu.findItem(R.id.filter_fresh)?.isChecked = false
                    menu.findItem(R.id.filter_rotten)?.isChecked = false
                }
            }
            item.isChecked = !item.isChecked // Update the checked state visually
            applyFilters()

            val activeFilters = listOf(currentCategoryFilter, currentConditionFilter).count { it != "Todos" }
            if (activeFilters > 0) {
                filterBadge.visibility = View.VISIBLE
                filterBadge.text = activeFilters.toString()
            } else {
                filterBadge.visibility = View.GONE
            }
            true
        }
        popup.show()
    }

    private fun showBasicProductDetails(iconName: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_basic_product)

        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        val ivIcon = dialog.findViewById<ImageView>(R.id.iconImageView)
        val tvName = dialog.findViewById<TextView>(R.id.nameTextView)
        val tvCategory = dialog.findViewById<TextView>(R.id.categoryTextView)
        val tvCondition = dialog.findViewById<TextView>(R.id.conditionTextView)
        val tvLocation = dialog.findViewById<TextView>(R.id.locationTextView)
        val tvQuantity = dialog.findViewById<TextView>(R.id.quantityTextView)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(), "Usuario no autenticado", Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("products")
            .whereEqualTo("icon", iconName)
            .limit(1) // solo el primero
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()

                    val iconRes = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
                    ivIcon.setImageResource(iconRes)

                    tvName.text = document.getString("name") ?: "Nombre no disponible"
                    tvCategory.text = "${document.getString("category") ?: "Desconocida"}"
                    tvCondition.text = "${document.getString("condition") ?: "Desconocida"}"
                    tvLocation.text = "${document.getString("location") ?: "Desconocida"}"
                    tvQuantity.text = "${document.get("quantity") ?: 0}"
                } else {
                    Toast.makeText(requireContext(), "Producto no encontrado", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error al cargar detalles", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

        dialog.show()
    }



}
