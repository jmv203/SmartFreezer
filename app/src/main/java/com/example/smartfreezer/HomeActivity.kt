package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.smartfreezer.databinding.ActivityHomeBinding
import com.example.smartfreezer.util.OnRecipeTabSelectedListener
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint
import me.ibrahimsn.lib.SmoothBottomBar
@AndroidEntryPoint
class HomeActivity : AppCompatActivity(), OnRecipeTabSelectedListener {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Firebase - Verifica autenticación
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Setup NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // Mostrar InventoryFragment por defecto
        navController.navigate(R.id.inventoryFragment)

        // Mapeo de ítems por índice
        binding.bottomBar.setOnItemSelectedListener { index ->
            when (index) {
                0 -> navController.navigate(R.id.listFragment)
                1 -> navController.navigate(R.id.scannerFragment)
                2 -> navController.navigate(R.id.inventoryFragment)
                3 -> navController.navigate(R.id.recipesFragment)
                4 -> navController.navigate(R.id.alertsFragment)
            }
            true
        }

        // Mantén la barra sincronizada con el fragmento actual
        navController.addOnDestinationChangedListener { _, destination, _ ->
            val index = when (destination.id) {
                R.id.listFragment -> 0
                R.id.scannerFragment -> 1
                R.id.inventoryFragment -> 2
                R.id.recipesFragment -> 3
                R.id.alertsFragment -> 4
                R.id.addShoppingListFragment -> -1 // No cambiar la selección
                else -> 2 // Default: inventario
            }
            if (index != -1) { // Solo actualiza si no es -1
                binding.bottomBar.itemActiveIndex = index
            }
        }

    }

    override fun onRecipeTabSelected(tabIndex: Int) {
        binding.bottomBar.itemActiveIndex = 3
        when (tabIndex) {
            0 -> {
                if (navController.currentDestination?.id != R.id.recipesFragment) {
                    navController.navigate(R.id.recipesFragment)
                }
            }
            1 -> {
                if (navController.currentDestination?.id != R.id.savedRecipesFragment) {
                    navController.navigate(R.id.savedRecipesFragment)
                }
            }
        }
    }
}

