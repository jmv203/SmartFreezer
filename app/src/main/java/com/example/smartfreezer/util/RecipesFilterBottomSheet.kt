package com.example.smartfreezer.util

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfreezer.adapters.UserIngredientsAdapter
import com.example.smartfreezer.databinding.DialogRecipesFilterBinding
import com.example.smartfreezer.models.UserIngredient
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject

class RecipesFilterBottomSheet(
    private val onApplyFilters: (String?, String?, List<String>, Float?) -> Unit
) : BottomSheetDialogFragment() {

    private var _binding: DialogRecipesFilterBinding? = null
    private val binding get() = _binding!!

    private var selectedType: String? = null
    private var selectedDiet: String? = null
    private var selectedIngredientIcons: List<String> = emptyList()
    private var selectedRating: Float? = null

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var userIngredientsAdapter: UserIngredientsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogRecipesFilterBinding.inflate(inflater, container, false)

        setupRecyclerView()
        loadUserIngredients()
        setupChipListeners() // Función para configurar los listeners de los ChipGroup

        binding.btnApplyFilters.setOnClickListener {
            onApplyFilters(selectedType, selectedDiet, selectedIngredientIcons, selectedRating)
            dismiss()
        }

        return binding.root
    }

    private fun setupChipListeners() {
        binding.chipGroupDishType.setOnCheckedChangeListener { _, checkedId ->
            selectedType = when (checkedId) {
                binding.chipMainCourse.id -> "main course"
                binding.chipSalad.id -> "salad"
                binding.chipSoup.id -> "soup"
                binding.chipDessert.id -> "dessert"
                binding.chipDrink.id -> "drink"
                else -> null
            }
        }

        binding.chipGroupDiet.setOnCheckedChangeListener { _, checkedId ->
            selectedDiet = when (checkedId) {
                binding.chipVegan.id -> "vegan"
                binding.chipVegetarian.id -> "vegetarian"
                binding.chipGlutenFree.id -> "gluten free"
                else -> null
            }
        }
    }

    private fun setupRecyclerView() {
        userIngredientsAdapter = UserIngredientsAdapter()
        binding.ingredientsRecyclerView.apply {
            adapter = userIngredientsAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun loadUserIngredients() {
        val currentUser = auth.currentUser?.uid ?: return

        db.collection("users")
            .document(currentUser)
            .collection("products")
            .get()
            .addOnSuccessListener { result ->
                val ingredients = result.documents.mapNotNull { document ->
                    document.toObject<UserIngredient>()
                }
                userIngredientsAdapter.setIngredients(ingredients)
            }
            .addOnFailureListener {

            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}