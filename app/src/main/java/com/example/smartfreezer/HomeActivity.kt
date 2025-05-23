package com.example.smartfreezer

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.example.smartfreezer.databinding.ActivityHomeBinding
import com.example.smartfreezer.util.OnInventoryTabSelectedListener
import com.example.smartfreezer.util.OnRecipeTabSelectedListener
import com.example.smartfreezer.util.SharedPrefManager
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class HomeActivity : BaseActivity(), OnRecipeTabSelectedListener, OnInventoryTabSelectedListener  {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        applyLocale()
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
        // Manejar enlace de verificación de email
        val intent = intent
        val emailLink = intent.data?.toString()

        if (emailLink != null) {
            if (Firebase.auth.isSignInWithEmailLink(emailLink)) {
                // El enlace es válido, puedes marcar el email como verificado
                // Aquí podrías guardar en SharedPreferences que el email está verificado
                // o mostrar un diálogo confirmando la verificación
                Toast.makeText(this,
                    getString(R.string.correo_electr_nico_verificado_correctamente), Toast.LENGTH_SHORT).show()
            }
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

        // Si venimos del cambio de idioma, ir directamente a InventoryFragment
        intent.getStringExtra("navigateTo")?.let {
            if (it == "inventory") {
                navController.navigate(R.id.inventoryFragment)
                binding.bottomBar.itemActiveIndex = 2
            }
        }

        handleIntentExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.let {
            handleIntentExtras(it)
        }
    }
    // En HomeActivity.kt, dentro de handleIntentExtras
    private fun handleIntentExtras(intent: Intent) {
        val filterDiet = intent.getStringExtra("FILTER_DIET") // Coincide con la clave de MyFirebaseMessagingService
        val recipeIdString = intent.getStringExtra("RECIPE_ID") // Coincide con la clave de MyFirebaseMessagingService

        var navigated = false
        if (recipeIdString != null) {
            try {
                val recipeId = recipeIdString.toInt()
                val bundle = Bundle().apply {
                    putString("RECIPE_ID", recipeIdString) // Argumento para RecipesFragment o RecipeDetailsFragment
                    if (filterDiet != null) {
                        putString("FILTER_DIET_NOTIFICATION", filterDiet) // Argumento para RecipesFragment
                    }
                }

                navController.navigate(R.id.recipesFragment, bundle)
                navigated = true
            } catch (e: NumberFormatException) {
                Log.e("HomeActivity", "Invalid recipeId from intent: $recipeIdString")
            }
        }

        if (!navigated && filterDiet != null) { // Si no hubo recipeId o fue inválido, pero hay filtro de dieta
            val bundle = Bundle().apply {
                putString("FILTER_DIET_NOTIFICATION", filterDiet)
            }
            navController.navigate(R.id.recipesFragment, bundle)
            navigated = true
        }

        // Limpiar extras para evitar reprocesamiento
        if (intent.hasExtra("RECIPE_ID")) intent.removeExtra("RECIPE_ID")
        if (intent.hasExtra("FILTER_DIET")) intent.removeExtra("FILTER_DIET")
    }


    private fun applyLocale() {
        val sharedPrefManager = SharedPrefManager(this)
        val languageCode = sharedPrefManager.getAppLanguage()
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = resources.configuration
        config.setLocale(locale)
        createConfigurationContext(config)
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

    override fun onInventoryTabSelected(tabIndex: Int) {
        binding.bottomBar.itemActiveIndex = 2
        when (tabIndex) {
            0 -> {
                if (navController.currentDestination?.id != R.id.inventoryFragment) {
                    navController.navigate(R.id.inventoryFragment)
                }
            }
            1 -> {
                if (navController.currentDestination?.id != R.id.wastedProductsFragment) {
                    navController.navigate(R.id.wastedProductsFragment)
                }
            }
        }
    }
}

