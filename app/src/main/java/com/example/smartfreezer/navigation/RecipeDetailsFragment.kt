package com.example.smartfreezer.navigation

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.databinding.BindingAdapter
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.IngredientsAdapter
import com.example.smartfreezer.adapters.NutrientsAdapter
import com.example.smartfreezer.adapters.StepsAdapter
import com.example.smartfreezer.databinding.FragmentRecipeDetailsBinding
import com.example.smartfreezer.databinding.IndicatorItemLayoutBinding
import com.example.smartfreezer.models.RecipeDetails
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.viewmodels.RecipesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlin.math.roundToInt

@AndroidEntryPoint
class RecipeDetailsFragment : Fragment() {

    private var _binding: FragmentRecipeDetailsBinding? = null
    private val binding get() = _binding!!
    private val recipesViewModel: RecipesViewModel by viewModels()

    private val ingredientsAdapter: IngredientsAdapter by lazy { IngredientsAdapter() }
    private val stepsAdapter: StepsAdapter by lazy { StepsAdapter() }
    private val nutrientsAdapter: NutrientsAdapter by lazy { NutrientsAdapter() }


    private var recipeId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            recipeId = it.getInt("recipeId")
            Log.d("RecipeDetailsFragment", "Recipe ID received: $recipeId")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRecipeDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()

        if (recipeId != -1) {
            recipesViewModel.getRecipeDetails(recipeId)
            observeRecipeDetails()
        } else {
            Toast.makeText(context, "Recipe ID is invalid", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackRecipeDetails.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.recipeDetailScrollView.post {
            binding.recipeDetailScrollView.scrollTo(0, 0)
        }

    }

    private fun setupRecyclerViews() {
        binding.recipeDetailIngredientsRecyclerView.apply {
            adapter = ingredientsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        }

        binding.recipeDetailStepsRecyclerView.apply {
            adapter = stepsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }

        binding.recipeDetailNutritionRecyclerView.apply {
            adapter = nutrientsAdapter
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.VERTICAL, false)
        }
    }

    private fun observeRecipeDetails() {
        recipesViewModel.recipeDetailsResponse.observe(viewLifecycleOwner, Observer { response ->
            when (response) {
                is NetworkResult.Success -> {
                    binding.recipeDetailScrollView.visibility = View.VISIBLE
                    binding.recipeDetailProgressBar.visibility = View.INVISIBLE
                    response.data?.let { details ->
                        binding.details = details

                        setupIndicators(details)


                        // Ingredientes
                        val ingredients = details.extendedIngredients?.filterNotNull() ?: emptyList()
                        ingredientsAdapter.setData(ingredients)

                        // Instrucciones
                        val steps = details.analyzedInstructions
                            ?.filterNotNull()
                            ?.flatMap { instruction -> instruction.steps?.filterNotNull() ?: emptyList() }
                            ?: emptyList()

                        stepsAdapter.setData(steps)

                        // Nutrientes
                        val nutrients = details.nutrition?.nutrients?.filterNotNull() ?: emptyList()
                        nutrientsAdapter.setData(nutrients)

                        Log.d("RecipeDetailsFragment", "Recipe Details: $details")

                    }
                }

                is NetworkResult.Error -> {
                    binding.recipeDetailProgressBar.visibility = View.INVISIBLE
                    binding.recipeDetailsErrorTextView.visibility = View.VISIBLE
                    binding.recipeDetailsErrorTextView.text = response.message.toString()
                    Toast.makeText(context, response.message, Toast.LENGTH_SHORT).show()
                    Log.e("RecipeDetailsFragment", "Error fetching recipe details: ${response.message}")
                }

                is NetworkResult.Loading -> {
                    binding.recipeDetailProgressBar.visibility = View.VISIBLE
                    binding.recipeDetailScrollView.visibility = View.INVISIBLE
                    binding.recipeDetailsErrorTextView.visibility = View.INVISIBLE
                    Log.d("RecipeDetailsFragment", "Loading recipe details...")
                }
            }
        })
    }

    private fun setupIndicators(recipe: RecipeDetails) {
        val container = binding.recipeDetailIndicatorsContainer
        container.removeAllViews()

        val inflater = LayoutInflater.from(requireContext())

        fun addIndicator(icon: Int, label: String, visible: Boolean) {
            val indicatorBinding = IndicatorItemLayoutBinding.inflate(inflater, container, false)
            indicatorBinding.iconRes = icon
            indicatorBinding.text = label
            indicatorBinding.isVisible = visible
            if (visible) container.addView(indicatorBinding.root)
        }

        addIndicator(R.drawable.ic_timer, "${recipe.readyInMinutes} min", true)
        addIndicator(R.drawable.ic_flame, "${recipe.nutrition?.nutrients?.firstOrNull { it?.name == "Calories" }?.amount?.roundToInt() ?: 0} kcal", true)
        addIndicator(R.drawable.ic_portion, "${recipe.servings} raciones", true)
        addIndicator(R.drawable.ic_gluten, "No gluten", recipe.glutenFree == true)
        addIndicator(R.drawable.ic_vegan, "Vegana", recipe.vegan == true)


    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_RECIPE_ID = "recipeId"

        fun newInstance(recipeId: Int): RecipeDetailsFragment {
            return RecipeDetailsFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_RECIPE_ID, recipeId)
                }
            }
        }
    }
}

