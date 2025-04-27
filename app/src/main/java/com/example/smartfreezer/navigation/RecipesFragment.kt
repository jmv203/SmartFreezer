package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.IngredientsAdapter
import com.example.smartfreezer.adapters.RecipesAdapter
import com.example.smartfreezer.databinding.FragmentRecipesBinding
import com.example.smartfreezer.databinding.DialogRecipesFilterBinding
import com.example.smartfreezer.models.Ingredients
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.viewmodels.RecipesViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private lateinit var binding: FragmentRecipesBinding
    private val recipesAdapter by lazy { RecipesAdapter() }
    private val recipesViewModel: RecipesViewModel by viewModels()
    private val firestore = FirebaseFirestore.getInstance()
    private val user = FirebaseAuth.getInstance().currentUser

    private var recipesList = mutableListOf<com.example.smartfreezer.models.Result>()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecipesBinding.bind(view)

        setupGreeting()
        setupRecyclerView()
        setupPullToRefresh()
        setupLoadMoreButton()
        setupFilterButton()

        requestApiData()
    }

    private fun setupGreeting() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { document ->
                document.getString("name")?.let { name ->
                    binding.tvGreetingRecipe.text = "Hola, $name"
                }
            }
    }

    private fun setupRecyclerView() {
        binding.recipesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = recipesAdapter
        }
    }

    private fun setupPullToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            recipesViewModel.resetOffset()
            recipesList.clear()
            requestApiData()
        }
    }

    private fun setupLoadMoreButton() {
        binding.btnLoadMore.setOnClickListener {
            recipesViewModel.increaseOffset()
            requestApiData(isLoadMore = true)
        }
    }

    private fun requestApiData(isLoadMore: Boolean = false) {
        recipesViewModel.getRecipes(recipesViewModel.applyQueries())

        recipesViewModel.recipesResponse.observe(viewLifecycleOwner) { result ->
            when (result) {
                is NetworkResult.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.recipesRecyclerView.visibility = View.VISIBLE
                    binding.swipeRefreshLayout.isRefreshing = false

                    result.data?.let { newRecipes ->
                        if (!isLoadMore) {
                            recipesList.clear()
                        }
                        recipesList.clear()
                        recipesList.addAll(newRecipes)
                        recipesAdapter.setData(recipesList)
                    }
                }

                is NetworkResult.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    binding.recipesRecyclerView.visibility = View.GONE
                    binding.swipeRefreshLayout.isRefreshing = false
                }

                is NetworkResult.Loading -> {
                    binding.shimmerLayout.startShimmer()
                    binding.shimmerLayout.visibility = View.VISIBLE
                    binding.recipesRecyclerView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupFilterButton() {
        binding.btnFilterRecipes.setOnClickListener {
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
        var selectedIngredients = recipesViewModel.selectedIngredients.toMutableList()

        // Configuración inicial del tipo de plato
        when (selectedType) {
            "main course" -> filterBinding.chipMainCourse.isChecked = true
            "salad" -> filterBinding.chipSalad.isChecked = true
            "soup" -> filterBinding.chipSoup.isChecked = true
            "dessert" -> filterBinding.chipDessert.isChecked = true
            "drink" -> filterBinding.chipDrink.isChecked = true
        }

        filterBinding.chipGroupDishType.setOnCheckedChangeListener { group, checkedId ->
            selectedType = when (checkedId) {
                R.id.chipMainCourse -> "main course"
                R.id.chipSalad -> "salad"
                R.id.chipSoup -> "soup"
                R.id.chipDessert -> "dessert"
                R.id.chipDrink -> "drink"
                else -> ""
            }
            recipesViewModel.updateFilters(
                type = selectedType,
                diet = selectedDiet,
                rating = selectedRating,
                ingredients = selectedIngredients
            )
            recipesList.clear()
            requestApiData()
        }

        // Configuración inicial del tipo de dieta
        when (selectedDiet) {
            "vegan" -> filterBinding.chipVegan.isChecked = true
            "vegetarian" -> filterBinding.chipVegetarian.isChecked = true
            "gluten free" -> filterBinding.chipGlutenFree.isChecked = true
        }

        filterBinding.chipGroupDiet.setOnCheckedChangeListener { group, checkedId ->
            selectedDiet = when (checkedId) {
                R.id.chipVegan -> "vegan"
                R.id.chipVegetarian -> "vegetarian"
                R.id.chipGlutenFree -> "gluten free"
                else -> ""
            }
            recipesViewModel.updateFilters(
                type = selectedType,
                diet = selectedDiet,
                rating = selectedRating,
                ingredients = selectedIngredients
            )
            recipesList.clear()
            requestApiData()
        }

        val ingredientsAdapter = IngredientsAdapter()
        filterBinding.ingredientsRecyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            adapter = ingredientsAdapter
        }

        val db = FirebaseFirestore.getInstance()
        val currentUser = FirebaseAuth.getInstance().currentUser

        currentUser?.let { user ->
            db.collection("users")
                .document(user.uid)
                .collection("products")
                .get()
                .addOnSuccessListener { documents ->
                    val products = documents.mapNotNull { it.toObject<Ingredients>() }
                    ingredientsAdapter.setIngredients(products, recipesViewModel.selectedIngredients)
                }
        }

        filterBinding.btnApplyFilters.setOnClickListener {
            val selectedIngredientIcons = ingredientsAdapter.getSelectedIngredientIcons()

            recipesViewModel.updateFilters(
                type = selectedType,
                diet = selectedDiet,
                rating = selectedRating,
                ingredients = selectedIngredientIcons
            )
            recipesList.clear()
            requestApiData()
            dialog.dismiss()
        }

        dialog.show()
    }
}