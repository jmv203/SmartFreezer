package com.example.smartfreezer.navigation

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AlertDialog
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.util.Log
import com.example.smartfreezer.ProfileActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.adapters.UserProductAdapter
import com.example.smartfreezer.models.UserProduct
import com.example.smartfreezer.models.WastedProduct
import com.example.smartfreezer.util.OnInventoryTabSelectedListener
import com.google.android.material.button.MaterialButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.skydoves.powerspinner.PowerSpinnerView

class InventoryFragment : Fragment(R.layout.fragment_inventory), OnInventoryTabSelectedListener {

    private lateinit var btnAccountInventory : ImageView
    private lateinit var btnAccountSettings : ImageView
    private lateinit var spinnerLocation: PowerSpinnerView
    private lateinit var tvProductCount: TextView
    private lateinit var searchBar: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: UserProductAdapter
    private lateinit var btnFilterOptions: Button
    private lateinit var filterBadge: TextView


    private var fullItemList: List<UserProduct> = emptyList()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var currentCategoryFilter: String
    private lateinit var currentConditionFilter: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TabLayout>(R.id.tabSelectorInventory)?.let { tabLayout ->
            // Selecciona el tab basado en el destino actual
            val selectedTab = when (findNavController().currentDestination?.id) {
                R.id.wastedProductsFragment -> 1
                else -> 0
            }
            tabLayout.selectTab(tabLayout.getTabAt(selectedTab))

            tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    if (tab.position == 1 && findNavController().currentDestination?.id != R.id.wastedProductsFragment) {
                        findNavController().navigate(R.id.action_inventoryFragment_to_wastedProductsFragment)
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab) {}
                override fun onTabReselected(tab: TabLayout.Tab) {}
            })
        }

        currentCategoryFilter = getString(R.string.filter_all)
        currentConditionFilter = getString(R.string.filter_all)

        btnAccountInventory = view.findViewById(R.id.btnAccountInventory)
        btnAccountSettings = view.findViewById(R.id.btnSettingsInventory)

        btnAccountInventory.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        btnAccountSettings.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        spinnerLocation = view.findViewById(R.id.spinnerLocation)

        tvProductCount = view.findViewById(R.id.tvProductCount)
        searchBar = view.findViewById(R.id.searchBar)
        recyclerView = view.findViewById(R.id.rvInventory)
        btnFilterOptions = view.findViewById(R.id.btnFilterOptions)
        filterBadge = view.findViewById(R.id.filterBadge)


        adapter = UserProductAdapter(emptyList(),
            { userProduct -> showBasicProductDetails(userProduct.icon) },
            { userProduct -> showDeleteConfirmation(userProduct) }
        )

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter

        setupSpinner()
        setupSearch()
        setupFilterMenu()

        setupGreeting(view)
        setupAddButton(view)

        loadItemsFromFirestore()
    }

    override fun onInventoryTabSelected(tabIndex: Int) {
        when (tabIndex) {
            0 -> {
                // Ya estamos en InventoryFragment, no necesitamos hacer nada
            }
            1 -> {
                findNavController().navigate(R.id.action_inventoryFragment_to_wastedProductsFragment)
            }
        }
    }

    private fun setupSpinner() {
        val locations = listOf(
            getString(R.string.filter_all),
            getString(R.string.filter_fridge),
            getString(R.string.filter_freezer),
            getString(R.string.filter_pantry)
        )

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
                    view.findViewById<TextView>(R.id.tvGreeting).text = getString(R.string.hola, name)
                }
            }
    }

    private fun setupAddButton(view: View) {
        view.findViewById<View>(R.id.fabAddProduct).setOnClickListener {
            findNavController().navigate(R.id.action_inventoryFragment_to_selectCategoryFragment)
        }
    }
    private fun showDeleteConfirmation(product: UserProduct) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.eliminar_producto))
            .setMessage(getString(R.string.esta_seguro_eliminar_producto, product.name))
            .setPositiveButton(getString(R.string.eliminar)) { _, _ ->
                deleteProduct(product)
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun deleteProduct(product: UserProduct) {
        val user = FirebaseAuth.getInstance().currentUser ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(user.uid)
            .collection("products")
            .document(product.idProduct)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(),
                    getString(R.string.producto_eliminado), Toast.LENGTH_SHORT).show()
                loadItemsFromFirestore() // Recargar la lista
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_eliminar), Toast.LENGTH_SHORT).show()
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
                    val condition = doc.getString("condition") ?: ""
                    val iconRes = resources.getIdentifier(icon, "drawable", requireContext().packageName)

                    UserProduct(
                        idProduct = doc.id,
                        name = name,
                        icon = icon,
                        category = doc.getString("category") ?: "",
                        condition = condition,
                        location = doc.getString("location") ?: "",
                        idUser = user.email ?: "",
                        quantity = 1
                    ).apply { iconDrawableRes = iconRes }
                }
                applyFilters()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_cargar_inventario), Toast.LENGTH_SHORT).show()
            }
    }

    @SuppressLint("StringFormatMatches")
    private fun applyFilters() {
        val location = spinnerLocation.text.toString()
        val query = searchBar.text.toString().lowercase()

        val filtered = fullItemList.filter { item ->
            (location == getString(R.string.filter_all) ||
                    (location == getString(R.string.filter_fridge) && item.location == "nevera") ||
                    (location == getString(R.string.filter_freezer) && item.location == "congelador") ||
                    (location == getString(R.string.filter_pantry) && item.location == "despensa")) &&

                    (currentCategoryFilter == getString(R.string.filter_all) ||
                            (currentCategoryFilter == getString(R.string.filter_category_fruit) && item.category == "fruta") ||
                            (currentCategoryFilter == getString(R.string.filter_category_vegetable) && item.category == "verdura")) &&

                    (currentConditionFilter == getString(R.string.filter_all) ||
                            (currentConditionFilter == getString(R.string.filter_condition_fresh) && item.condition == "fresco") ||
                            (currentConditionFilter == getString(R.string.filter_condition_rotten) && item.condition == "podrido")) &&

                    (query.isBlank() || item.name.lowercase().contains(query))
        }

        adapter.updateData(filtered)
        tvProductCount.text = getString(R.string.productos_encontrados, filtered.size)
    }

    private fun showPopupMenu(anchor: View) {
        val themedContext = ContextThemeWrapper(requireContext(), R.style.PopupMenuInventory)
        val popup = PopupMenu(themedContext, anchor)
        popup.menuInflater.inflate(R.menu.popup_filter_menu, popup.menu)

        val menu = popup.menu
        menu.findItem(R.id.filter_fruit)?.isChecked =
            currentCategoryFilter == getString(R.string.filter_category_fruit)
        menu.findItem(R.id.filter_vegetable)?.isChecked =
            currentCategoryFilter == getString(R.string.filter_category_vegetable)
        menu.findItem(R.id.filter_fresh)?.isChecked =
            currentConditionFilter == getString(R.string.filter_condition_fresh)
        menu.findItem(R.id.filter_rotten)?.isChecked =
            currentConditionFilter == getString(R.string.filter_condition_rotten)

        popup.setOnMenuItemClickListener { item: MenuItem ->
            when (item.itemId) {
                R.id.filter_fruit -> currentCategoryFilter =
                    if (item.isChecked) getString(R.string.filter_all)
                    else getString(R.string.filter_category_fruit)
                R.id.filter_vegetable -> currentCategoryFilter =
                    if (item.isChecked) getString(R.string.filter_all)
                    else getString(R.string.filter_category_vegetable)
                R.id.filter_fresh -> currentConditionFilter =
                    if (item.isChecked) getString(R.string.filter_all)
                    else getString(R.string.filter_condition_fresh)
                R.id.filter_rotten -> currentConditionFilter =
                    if (item.isChecked) getString(R.string.filter_all)
                    else getString(R.string.filter_condition_rotten)
                R.id.filter_clear -> {
                    currentCategoryFilter = getString(R.string.filter_all)
                    currentConditionFilter = getString(R.string.filter_all)
                    menu.findItem(R.id.filter_fruit)?.isChecked = false
                    menu.findItem(R.id.filter_vegetable)?.isChecked = false
                    menu.findItem(R.id.filter_fresh)?.isChecked = false
                    menu.findItem(R.id.filter_rotten)?.isChecked = false
                }
            }
            item.isChecked = !item.isChecked
            applyFilters()

            val activeFilters = listOf(
                currentCategoryFilter != getString(R.string.filter_all),
                currentConditionFilter != getString(R.string.filter_all)
            ).count { it }

            filterBadge.visibility = if (activeFilters > 0) View.VISIBLE else View.GONE
            filterBadge.text = activeFilters.toString()
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
        val btnDelete = dialog.findViewById<MaterialButton>(R.id.tvDeleteProduct)
        val btnMarkAsRotten = dialog.findViewById<MaterialButton>(R.id.btnMarkAsRotten)
        val btnDecrease = dialog.findViewById<MaterialButton>(R.id.btnDecrease)
        val btnIncrease = dialog.findViewById<MaterialButton>(R.id.btnIncrease)

        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (uid == null) {
            Toast.makeText(requireContext(),
                getString(R.string.usuario_no_autenticado), Toast.LENGTH_SHORT).show()
            return
        }

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("products")
            .whereEqualTo("icon", iconName)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val document = documents.first()
                    val productRef = document.reference
                    val currentQuantity = (document.getLong("quantity") ?: 1).toInt()
                    val product = UserProduct(
                        idProduct = document.id,
                        name = document.getString("name") ?: "",
                        icon = iconName,
                        category = document.getString("category") ?: "",
                        condition = document.getString("condition") ?: "",
                        location = document.getString("location") ?: "",
                        idUser = uid,
                        quantity = currentQuantity
                    )

                    val iconRes = resources.getIdentifier(iconName, "drawable", requireContext().packageName)
                    ivIcon.setImageResource(iconRes)

                    tvName.text = document.getString("name") ?: getString(R.string.nombre_no_disponible)
                    tvCategory.text = "${document.getString("category") ?: "Desconocida"}"
                    tvCondition.text = "${document.getString("condition") ?: "Desconocida"}"
                    tvLocation.text = "${document.getString("location") ?: "Desconocida"}"
                    tvQuantity.text = currentQuantity.toString()

                    // Configurar el selector de ubicación
                    setupLocationSelector(tvLocation, productRef)

                    // Configurar los botones de cantidad
                    setupQuantityControls(tvQuantity, btnDecrease, btnIncrease, productRef, currentQuantity)

                    // Configurar el botón de marcar como podrido
                    if (product.condition == "podrido") {
                        btnMarkAsRotten.text = getString(R.string.mark_as_fresh)
                        btnMarkAsRotten.setOnClickListener {
                            showMarkAsFreshConfirmation(product, productRef, dialog)
                        }
                    } else {
                        btnMarkAsRotten.text = getString(R.string.mark_as_rotten)
                        btnMarkAsRotten.setOnClickListener {
                            showMarkAsRottenConfirmation(product, productRef, dialog)
                        }
                    }

                    // Configurar el botón de eliminar
                    btnDelete.setOnClickListener {
                        dialog.dismiss()
                        showDeleteConfirmation(product)
                    }
                } else {
                    Toast.makeText(requireContext(),
                        getString(R.string.producto_no_encontrado), Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_al_cargar_detalles), Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }

        dialog.show()
    }

    private fun setupLocationSelector(tvLocation: TextView, productRef: DocumentReference) {
        val locations = listOf(
            "nevera" to getString(R.string.filter_fridge),
            "congelador" to getString(R.string.filter_freezer),
            "despensa" to getString(R.string.filter_pantry)
        )

        tvLocation.setOnClickListener {
            val items = locations.map { it.second }.toTypedArray()
            AlertDialog.Builder(requireContext())
                .setTitle(getString(R.string.select_location))
                .setItems(items) { _, which ->
                    val selectedLocation = locations[which].first
                    productRef.update("location", selectedLocation)
                        .addOnSuccessListener {
                            tvLocation.text = locations[which].second
                            loadItemsFromFirestore() // Actualizar la lista
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(),
                                getString(R.string.error_updating_location), Toast.LENGTH_SHORT).show()
                        }
                }
                .show()
        }
    }

    private fun setupQuantityControls(
        tvQuantity: TextView,
        btnDecrease: MaterialButton,
        btnIncrease: MaterialButton,
        productRef: DocumentReference,
        initialQuantity: Int
    ) {
        var currentQuantity = initialQuantity
        tvQuantity.text = currentQuantity.toString()

        // Deshabilitar el botón de disminuir si la cantidad es 1
        btnDecrease.isEnabled = currentQuantity > 1

        btnDecrease.setOnClickListener {
            if (currentQuantity > 1) {
                currentQuantity--
                tvQuantity.text = currentQuantity.toString()
                btnDecrease.isEnabled = currentQuantity > 1
                updateQuantityInFirestore(productRef, currentQuantity)
            }
        }

        btnIncrease.setOnClickListener {
            currentQuantity++
            tvQuantity.text = currentQuantity.toString()
            btnDecrease.isEnabled = true
            updateQuantityInFirestore(productRef, currentQuantity)
        }
    }

    private fun updateQuantityInFirestore(productRef: DocumentReference, newQuantity: Int) {
        productRef.update("quantity", newQuantity)
            .addOnSuccessListener {
                loadItemsFromFirestore() // Actualizar la lista principal
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(),
                    getString(R.string.error_updating_quantity), Toast.LENGTH_SHORT).show()
            }
    }

    private fun showMarkAsRottenConfirmation(product: UserProduct, productRef: DocumentReference, dialog: Dialog) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.mark_as_rotten))
            .setMessage(getString(R.string.confirm_mark_as_rotten, product.name))
            .setPositiveButton(getString(R.string.confirmar)) { _, _ ->
                // Actualizar condición en Firestore
                productRef.update("condition", "podrido")
                    .addOnSuccessListener {
                        // Registrar como desperdiciado
                        registerWastedProduct(product)
                        Toast.makeText(requireContext(),
                            getString(R.string.product_marked_rotten), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadItemsFromFirestore() // Recargar lista
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.error_marking_rotten), Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun showMarkAsFreshConfirmation(product: UserProduct, productRef: DocumentReference, dialog: Dialog) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.mark_as_fresh))
            .setMessage(getString(R.string.confirm_mark_as_fresh, product.name))
            .setPositiveButton(getString(R.string.confirmar)) { _, _ ->
                // Actualizar condición en Firestore
                productRef.update("condition", "fresco")
                    .addOnSuccessListener {
                        // Eliminar de productos desperdiciados si existe
                        removeFromWastedProducts(product.idProduct)
                        Toast.makeText(requireContext(),
                            getString(R.string.product_marked_fresh), Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadItemsFromFirestore() // Recargar lista
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(),
                            getString(R.string.error_marking_fresh), Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton(getString(R.string.cancelar), null)
            .show()
    }

    private fun removeFromWastedProducts(productId: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("wasted_products")
            .whereEqualTo("original_product_id", productId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            Log.d("Inventory", "Producto eliminado de wasted_products: $productId")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Inventory", "Error al eliminar de wasted_products", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Inventory", "Error al buscar en wasted_products", e)
            }
    }

    private fun registerWastedProduct(product: UserProduct) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        firestore.collection("users").document(userId)
            .collection("wasted_products")
            .whereEqualTo("original_product_id", product.idProduct)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    val wastedProduct = WastedProduct(
                        name = product.name,
                        icon = product.icon,
                        category = product.category,
                        originalProductId = product.idProduct,
                        date = com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("users").document(userId)
                        .collection("wasted_products")
                        .add(wastedProduct)
                        .addOnSuccessListener {
                            Log.d("Inventory", "Producto desperdiciado registrado: ${product.name}")
                        }
                        .addOnFailureListener { e ->
                            Log.e("Inventory", "Error al registrar producto desperdiciado", e)
                            Toast.makeText(requireContext(),
                                "Error al registrar producto desperdiciado", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Inventory", "Error al verificar producto desperdiciado", e)
            }
    }



}
