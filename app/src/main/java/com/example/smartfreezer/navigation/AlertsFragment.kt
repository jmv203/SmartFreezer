package com.example.smartfreezer.navigation

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import android.view.ViewGroup
import android.view.LayoutInflater
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.databinding.FragmentAlertsBinding
import com.example.smartfreezer.adapters.SpoiledAlertAdapter
import com.example.smartfreezer.adapters.SeasonAlertAdapter
import com.example.smartfreezer.models.SeasonProduct
import com.example.smartfreezer.models.SpoiledProduct
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Calendar

class AlertsFragment : Fragment() {
    private var _binding: FragmentAlertsBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var spoiledProductsListener: ListenerRegistration? = null

    private val spoiledAdapter by lazy {
        SpoiledAlertAdapter { product -> handleDeleteSpoiledProduct(product) }
    }
    private val seasonAdapter by lazy {
        SeasonAlertAdapter { product -> handleAddToShoppingList(product) }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAlertsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        setupGreeting()
        setupRecyclerViews()
        loadUserData()
        setupClickListeners()
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    _binding?.tvGreetingAlert?.text = getString(R.string.hola, name)
                }
            }
    }

    private fun setupRecyclerViews() {
        binding.rvSpoiledAlerts.adapter = spoiledAdapter
        binding.rvSeasonAlerts.adapter = seasonAdapter
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            _binding?.tvGreetingAlert?.text = getString(R.string.hola, user.displayName ?: getString(R.string.usuario))
            loadUserProducts(user.uid)
            loadSeasonProducts()
        }
    }

    private fun loadUserProducts(userId: String) {
        spoiledProductsListener = db.collection("users").document(userId).collection("products")
            .whereEqualTo("condition", "podrido")
            .addSnapshotListener { snapshot, error ->
                if (_binding == null) return@addSnapshotListener // Fragment ya destruido

                if (error != null) {
                    Toast.makeText(context, getString(R.string.error_cargando_productos), Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val products = snapshot?.documents?.mapNotNull { doc ->
                    SpoiledProduct(
                        id = doc.id,
                        name = doc.getString("name") ?: "",
                        icon = doc.getString("icon") ?: "ic_default_food",
                        condition = doc.getString("condition") ?: "",
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        location = doc.getString("location") ?: ""
                    )
                } ?: emptyList()

                spoiledAdapter.updateData(products)

                _binding?.let { binding ->
                    if (products.isEmpty()) {
                        binding.tvNoSpoiledProducts.visibility = View.VISIBLE
                        binding.rvSpoiledAlerts.visibility = View.GONE
                    } else {
                        binding.tvNoSpoiledProducts.visibility = View.GONE
                        binding.rvSpoiledAlerts.visibility = View.VISIBLE
                    }
                }
            }
    }


    private fun loadSeasonProducts() {
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1

        db.collection("basic_products")
            .get()
            .addOnSuccessListener { snapshot ->
                val products = mutableListOf<SeasonProduct>()

                for (doc in snapshot.documents) {
                    try {
                        val startSeason = (doc.get("startSeason") as? Number)?.toInt() ?: continue
                        val lastSeason = (doc.get("lastSeason") as? Number)?.toInt() ?: continue

                        if (startSeason !in 1..12 || lastSeason !in 1..12) continue

                        if (isProductInSeason(currentMonth, startSeason, lastSeason)) {
                            products.add(
                                SeasonProduct(
                                    id = doc.id,
                                    name = doc.getString("name") ?: "Producto",
                                    icon = doc.getString("icon") ?: "ic_default_food",
                                    startSeason = startSeason,
                                    lastSeason = lastSeason
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("AlertsFragment", "Error procesando producto: ${e.message}")
                    }
                }

                seasonAdapter.updateData(products)
            }
            .addOnFailureListener {
                Toast.makeText(context, getString(R.string.error_cargando_productos_de_temporada), Toast.LENGTH_SHORT).show()
            }
    }

    private fun isProductInSeason(currentMonth: Int, startSeason: Int, lastSeason: Int): Boolean {
        return if (startSeason <= lastSeason) {
            currentMonth in startSeason..lastSeason
        } else {
            currentMonth >= startSeason || currentMonth <= lastSeason
        }
    }

    private fun handleDeleteSpoiledProduct(product: SpoiledProduct) {
        val userId = auth.currentUser?.uid ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirmar_eliminaci_n))
            .setMessage(getString(R.string.est_s_seguro_que_quieres_eliminar_de_tu_inventario, product.name))
            .setPositiveButton(getString(R.string.aceptar)) { dialog, _ ->
                deleteProductFromInventory(userId, product.id)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteProductFromInventory(userId: String, productId: String) {
        db.collection("users").document(userId).collection("products")
            .document(productId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(context, getString(R.string.producto_eliminado), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, getString(R.string.error_eliminando_producto), Toast.LENGTH_SHORT).show()
            }
    }

    private fun handleAddToShoppingList(product: SeasonProduct) {
        val userId = auth.currentUser?.uid ?: return

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(getString(R.string.confirmar_acci_n))
            .setMessage(getString(R.string.est_s_seguro_que_deseas_a_adir_a_la_lista_de_la_compra, product.name))
            .setPositiveButton(getString(R.string.aceptar)) { dialog, _ ->
                addToShoppingList(userId, product)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.cancelar)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun addToShoppingList(userId: String, product: SeasonProduct) {
        val item = hashMapOf(
            "name" to product.name,
            "icon" to product.icon,
            "quantity" to 1
        )

        db.collection("users").document(userId).collection("shopping_list")
            .document(product.name)
            .set(item)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.a_adido_a_la_lista, product.name), Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, getString(R.string.error_a_adiendo_a_la_lista), Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        binding.btnAccountAlert.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsAlert.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        spoiledProductsListener?.remove()
        _binding = null
    }
}
