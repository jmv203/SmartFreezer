package com.example.smartfreezer.navigation

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
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
import com.example.smartfreezer.ProfileActivity
import com.example.smartfreezer.R
import com.example.smartfreezer.SettingsActivity
import com.example.smartfreezer.adapters.RecipesAdapter
import com.example.smartfreezer.adapters.UserIngredientsAdapter
import com.example.smartfreezer.databinding.DialogRecipesFilterBinding
import com.example.smartfreezer.databinding.FragmentSavedRecipesBinding
import com.example.smartfreezer.models.Result
import com.example.smartfreezer.models.UserIngredient
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.util.OnRecipeTabSelectedListener
import com.example.smartfreezer.viewmodels.RecipesViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SavedRecipesFragment : Fragment(R.layout.fragment_saved_recipes) {

    private var _binding: FragmentSavedRecipesBinding? = null
    private val binding get() = _binding!!
    private var tabSelectedListener: OnRecipeTabSelectedListener? = null
    private val savedRecipeAdapter by lazy { RecipesAdapter() }
    private val recipesViewModel: RecipesViewModel by activityViewModels()

    private var recipesList = mutableListOf<Result>()


    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnRecipeTabSelectedListener) {
            tabSelectedListener = context
        } else {
            throw ClassCastException("$context must implement OnRecipeTabSelectedListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        tabSelectedListener = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedRecipesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnAccountSavedRecipe.setOnClickListener {
            val intent = Intent(requireContext(), ProfileActivity::class.java)
            startActivity(intent)
        }

        binding.btnSettingsSavedRecipe.setOnClickListener {
            val intent = Intent(requireContext(), SettingsActivity::class.java)
            startActivity(intent)
        }

        setupTabLayout()
        setupGreeting()
        setupRecyclerView()
        setupFilterButton()
        setupClearFiltersButton()
        setupClearSavedRecipesButton()

        updateFilterIndicatorsVisibility(false)
        requestSavedRecipes()
    }

    private fun setupTabLayout() {
        binding.tabSelectorSavedRecipe.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                tabSelectedListener?.onRecipeTabSelected(tab.position)
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

        savedRecipeAdapter.setOnSaveClickListener { result ->
            toggleSaveRecipe(result)
        }

        updateAdapterSavedRecipeIds()
    }


    private fun updateAdapterSavedRecipeIds() {
        getSavedRecipeIds { ids ->
            savedRecipeAdapter.setSavedRecipeIds(ids)
        }
    }

    private fun getSavedRecipeIds(callback: (Set<Int>) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val savedRecipeRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("savedRecipes")

        savedRecipeRef.get()
            .addOnSuccessListener { snapshot ->
                val ids = snapshot.documents.mapNotNull { it.getLong("recipeId")?.toInt() }.toSet()
                callback(ids)
            }
            .addOnFailureListener {
                callback(emptySet())
            }
    }

    private fun toggleSaveRecipe(result: Result) {
        checkSavedStatus(result) { isSaved ->
            if (isSaved) {
                deleteSavedRecipe(result)
            } else {
                saveRecipe(result)
            }
        }
    }

    private fun saveRecipe(result: Result) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val savedRecipeRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("savedRecipes")
            .document(result.id.toString())

        val data = mapOf(
            "recipeId" to result.id,
            "title" to result.title
        )

        savedRecipeRef.set(data)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.receta_guardada), Toast.LENGTH_SHORT).show()
                updateAdapterSavedRecipeIds() // Update saved IDs in adapter
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.error_al_guardar), Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteSavedRecipe(result: Result) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val savedRecipeRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("savedRecipes")
            .document(result.id.toString())

        savedRecipeRef.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), getString(R.string.receta_eliminada), Toast.LENGTH_SHORT).show()
                updateAdapterSavedRecipeIds() // Update saved IDs in adapter
                recipesList.removeIf { it.id == result.id } // Remove deleted item from list
                savedRecipeAdapter.setData(recipesList) // Update adapter data
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), getString(R.string.error_al_eliminar), Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkSavedStatus(result: Result, callback: (Boolean) -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val savedRecipeRef = FirebaseFirestore.getInstance()
            .collection("users")
            .document(userId)
            .collection("savedRecipes")
            .document(result.id.toString())

        savedRecipeRef.get()
            .addOnSuccessListener { document ->
                callback(document.exists())
            }
            .addOnFailureListener {
                callback(false) // Assume not saved on error
            }
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        FirebaseFirestore.getInstance().collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    _binding?.tvGreetingSavedRecipe?.text = getString(R.string.hola, name)
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
                if (_binding == null) return@addOnSuccessListener

                val recipesData = snapshot.documents.mapNotNull { document ->
                    val recipeId = document.getLong("recipeId")?.toInt()
                    val recipeTitle = document.getString("title")
                    if (recipeId != null && recipeTitle != null) {
                        Pair(recipeId, recipeTitle)
                    } else {
                        null
                    }
                }

                if (recipesData.isEmpty()) {
                    hideLoading()
                    savedRecipeAdapter.setData(emptyList())
                    _binding?.tvEmpty?.visibility = View.VISIBLE
                    return@addOnSuccessListener
                }
                _binding?.tvEmpty?.visibility = View.GONE

                recipesList.clear()
                val searchResults = mutableListOf<Result>()
                var completedRequests = 0
                val totalRequests = recipesData.size

                if (totalRequests == 0) {
                    hideLoading()
                    savedRecipeAdapter.setData(emptyList())
                    return@addOnSuccessListener
                }

                recipesData.forEach { (recipeId, recipeTitle) ->
                    recipesViewModel.searchRecipesByTitle(recipeTitle) { result ->
                        if (_binding == null) return@searchRecipesByTitle

                        when (result) {
                            is NetworkResult.Success -> {
                                result.data?.let { recipes ->
                                    if (recipes.isNotEmpty()) {
                                        searchResults.add(recipes.first())
                                    } else {
                                        Log.w("SavedRecipes", "No recipe found for $recipeTitle")
                                    }
                                }
                            }
                            is NetworkResult.Error -> {
                                Toast.makeText(requireContext(),
                                    getString(R.string.error_en_buscar, recipeTitle, result.message), Toast.LENGTH_SHORT).show()
                            }
                            is NetworkResult.Loading -> {
                                showLoading()
                            }
                        }

                        completedRequests++
                        if (completedRequests == totalRequests) {
                            hideLoading()
                            val distinctRecipes = searchResults.distinctBy { it.id }
                            savedRecipeAdapter.setData(distinctRecipes)
                        }
                    }
                }
            }
            .addOnFailureListener {
                hideLoading()
                Toast.makeText(requireContext(),
                    getString(R.string.error_en_cargar_las_recetas_guardadas), Toast.LENGTH_SHORT).show()
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
            "dairy free" -> filterBinding.chipDairyFree.isChecked = true
        }

        // Listener del ChipGroup de tipo de dieta
        filterBinding.chipGroupDiet.setOnCheckedChangeListener { group, checkedId ->
            selectedDiet = when (checkedId) {
                R.id.chipVegan -> "vegan"
                R.id.chipVegetarian -> "vegetarian"
                R.id.chipGlutenFree -> "gluten free"
                R.id.chipDairyFree -> "dairy free"
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
                .setTitle(getString(R.string.eliminar_todas_las_recetas))
                .setMessage(getString(R.string.est_s_seguro_de_que_quieres_eliminar_todas_las_recetas_guardadas))
                .setPositiveButton(getString(R.string.s)) { _, _ -> clearAllSavedRecipes() }
                .setNegativeButton(getString(R.string.cancelar), null)
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

    override fun onResume() {
        super.onResume()
        binding.tabSelectorSavedRecipe.selectTab(binding.tabSelectorSavedRecipe.getTabAt(1))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}