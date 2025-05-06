package com.example.smartfreezer.navigation

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.RecipesAdapter
import com.example.smartfreezer.adapters.UserIngredientsAdapter
import com.example.smartfreezer.databinding.DialogRecipesFilterBinding
import com.example.smartfreezer.databinding.FragmentSavedRecipesBinding
import com.example.smartfreezer.models.Result
import com.example.smartfreezer.models.UserIngredient
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.viewmodels.RecipesViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedRecipesFragment : Fragment() {

    private var _binding: FragmentSavedRecipesBinding? = null
    private val binding get() = _binding!!
    private val recipesViewModel: RecipesViewModel by activityViewModels()
    private var recipesList = mutableListOf<Result>()
    private val savedRecipeAdapter by lazy { RecipesAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupGreeting()
        setupRecyclerView()
        setupFilterButton()
        setupClearFiltersButton()
        setupClearSavedRecipesButton()
        requestSavedRecipes()
        setupTabLayout()
    }

    private fun setupTabLayout() {
        binding.tabSelectorSavedRecipe.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> {
                        // "Recetas guardadas" (Stay in this fragment, no action needed)
                    }
                    1 -> {
                        // "Recetas disponibles" -> Navigate to RecipesFragment
                        val action = SavedRecipesFragmentDirections.actionSavedRecipesFragmentToRecipesFragment()
                        findNavController().navigate(action)
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }


    private fun setupRecyclerView() {
        binding.savedRecipesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = savedRecipeAdapter
        }

        savedRecipeAdapter.setOnItemClickListener { recipeId ->
            val action = SavedRecipesFragmentDirections.actionSavedRecipesFragmentToRecipeDetailsFragment(recipeId)
            findNavController().navigate(action)
        }
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    binding.tvGreetingSavedRecipe.text = "Hola, $name"
                }
            }
    }

    private fun requestSavedRecipes() {
        showLoading()

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("savedRecipes")
            .get()
            .addOnSuccessListener { snapshot ->
                val recipeIds = snapshot.documents.mapNotNull { it.getLong("id")?.toInt() }

                if (recipeIds.isEmpty()) {
                    hideLoading()
                    savedRecipeAdapter.setData(emptyList())
                    return@addOnSuccessListener
                }
                //No hay recetas guardadas
                binding.tvEmpty.visibility = View.GONE

                val idsString = recipeIds.joinToString(",")
                recipesViewModel.getBulkRecipes(idsString)

                recipesViewModel.savedRecipesResponse.observe(viewLifecycleOwner) { result ->
                    when (result) {
                        is NetworkResult.Success -> {
                            hideLoading()
                            result.data?.let { recipes ->
                                savedRecipeAdapter.setData(recipes)
                            }
                        }
                        is NetworkResult.Error -> {
                            hideLoading()
                            Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                        }
                        is NetworkResult.Loading -> showLoading()
                    }
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(requireContext(), "Error loading saved recipes", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading() {
        binding.shimmerLayoutSavedRecipe.startShimmer()
        binding.shimmerLayoutSavedRecipe.visibility = View.VISIBLE
        binding.savedRecipesRecyclerView.visibility = View.GONE

    }

    private fun hideLoading() {
        binding.shimmerLayoutSavedRecipe.stopShimmer()
        binding.shimmerLayoutSavedRecipe.visibility = View.GONE
        binding.savedRecipesRecyclerView.visibility = View.VISIBLE

    }

    private fun setupFilterButton() {
        binding.btnFilterSavedRecipes.setOnClickListener {
            showFilterBottomSheet()
        }
    }

    private fun showFilterBottomSheet() {
        val dialog = BottomSheetDialog(requireContext(), R.style.BottomSheetDialogTheme)
        val filterBinding = DialogRecipesFilterBinding.inflate(layoutInflater)
        dialog.setContentView(filterBinding.root)

        var selectedType = recipesViewModel.selectedType
        var selectedDiet = recipesViewModel.selectedDiet
        var selectedRating = recipesViewModel.selectedRating
        val selectedIngredientIcons = recipesViewModel.selectedIngredients.toMutableList()

        // Configuración inicial del tipo de plato
        when (selectedType) {
            "main course" -> filterBinding.chipMainCourse.isChecked = true
            "salad" -> filterBinding.chipSalad.isChecked = true
            "soup" -> filterBinding.chipSoup.isChecked = true
            "dessert" -> filterBinding.chipDessert.isChecked = true
            "drink" -> filterBinding.chipDrink.isChecked = true
        }

        // Listener del ChipGroup de tipo de plato (SIN LLAMADA A LA API)
        filterBinding.chipGroupDishType.setOnCheckedChangeListener { group, checkedId ->
            selectedType = when (checkedId) {
                R.id.chipMainCourse -> "main course"
                R.id.chipSalad -> "salad"
                R.id.chipSoup -> "soup"
                R.id.chipDessert -> "dessert"
                R.id.chipDrink -> "drink"
                else -> ""
            }
        }

        // Configuración inicial del tipo de dieta
        when (selectedDiet) {
            "vegan" -> filterBinding.chipVegan.isChecked = true
            "vegetarian" -> filterBinding.chipVegetarian.isChecked = true
            "gluten free" -> filterBinding.chipGlutenFree.isChecked = true
        }

        // Listener del ChipGroup de tipo de dieta
        filterBinding.chipGroupDiet.setOnCheckedChangeListener { group, checkedId ->
            selectedDiet = when (checkedId) {
                R.id.chipVegan -> "vegan"
                R.id.chipVegetarian -> "vegetarian"
                R.id.chipGlutenFree -> "gluten free"
                else -> ""
            }
        }

        val userIngredientsAdapter = UserIngredientsAdapter()
        filterBinding.ingredientsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = userIngredientsAdapter
        }

        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .collection("products")
                .get()
                .addOnSuccessListener { documents ->
                    val products = documents.mapNotNull { it.toObject<UserIngredient>() }
                    userIngredientsAdapter.setIngredients(
                        products,
                        recipesViewModel.selectedIngredients
                    )
                }
        }

        // OnClickListener del botón "Aplicar Filtros"
        filterBinding.btnApplyFilters.setOnClickListener {
            val selectedIngredientIcons = userIngredientsAdapter.getSelectedIngredientIcons()

            recipesViewModel.updateFilters(
                type = selectedType,
                diet = selectedDiet,
                rating = selectedRating,
                ingredients = selectedIngredientIcons
            )

            val appliedFilterCount = calculateAppliedFilterCount()
            updateFilterCountRecipe(appliedFilterCount)
            updateFilterIndicatorsVisibility(appliedFilterCount > 0)

            recipesList.clear()
            requestSavedRecipes()
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun setupClearFiltersButton() {
        binding.btnClearFiltersSavedRecipe.setOnClickListener {
            recipesViewModel.updateFilters("", "", 0, emptyList())  // Reset filters
            recipesList.clear()
            requestSavedRecipes()
            updateFilterIndicatorsVisibility(false)
        }
    }

    private fun updateFilterIndicatorsVisibility(filtersApplied: Boolean) {
        binding.filterCountSavedRecipe.visibility = if (filtersApplied) View.VISIBLE else View.GONE
        binding.btnClearFiltersSavedRecipe.visibility = if (filtersApplied) View.VISIBLE else View.GONE
    }

    private fun updateFilterCountRecipe(count: Int) {
        binding.filterCountSavedRecipe.text = count.toString()
    }

    private fun calculateAppliedFilterCount(): Int {
        var count = 0
        if (recipesViewModel.selectedType.isNotEmpty()) count++
        if (recipesViewModel.selectedDiet.isNotEmpty()) count++
        if (recipesViewModel.selectedIngredients.isNotEmpty()) count += recipesViewModel.selectedIngredients.size
        return count
    }

    private fun setupClearSavedRecipesButton() {
        binding.btnClearSavedRecipes.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Eliminar todas las recetas")
                .setMessage("¿Estás seguro de que quieres eliminar todas las recetas guardadas?")
                .setPositiveButton("Sí") { _, _ -> clearAllSavedRecipes() }
                .setNegativeButton("Cancelar", null)
                .show()
        }
    }

    private fun clearAllSavedRecipes() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val savedRecipesRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(uid)
            .collection("savedRecipes")

        savedRecipesRef.get().addOnSuccessListener { snapshot ->
            val batch = FirebaseFirestore.getInstance().batch()
            snapshot.documents.forEach { doc -> batch.delete(doc.reference) }
            batch.commit().addOnSuccessListener {
                savedRecipeAdapter.setData(emptyList())
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
