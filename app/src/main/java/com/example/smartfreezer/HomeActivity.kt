package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.smartfreezer.databinding.ActivityHomeBinding
import com.google.firebase.auth.FirebaseAuth
import me.ibrahimsn.lib.SmoothBottomBar

class HomeActivity : AppCompatActivity() {

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
                else -> 2 // Default: inventario
            }
            binding.bottomBar.itemActiveIndex = index
        }

        // Maneja el botón "Atrás"
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (navController.currentDestination?.id != R.id.inventoryFragment) {
                    navController.navigate(R.id.inventoryFragment)
                } else {
                    finish()
                }
            }
        })
    }
}
