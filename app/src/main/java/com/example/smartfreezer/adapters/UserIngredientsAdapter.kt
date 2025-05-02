package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.databinding.ItemIngredientBinding
import com.example.smartfreezer.models.UserIngredient

class UserIngredientsAdapter : RecyclerView.Adapter<UserIngredientsAdapter.IngredientViewHolder>() {

    private var userIngredientList = listOf<UserIngredient>()
    private val selectedIngredientIcons = mutableSetOf<String>() // Store icons

    // Modifica la función setIngredients para eliminar duplicados basados en el nombre
    fun setIngredients(ingredients: List<UserIngredient>, preselectedIcons: List<String> = emptyList()) {
        val uniqueIngredients = ingredients
            .groupBy { it.name }
            .map { it.value.first() }

        userIngredientList = uniqueIngredients
        selectedIngredientIcons.clear()
        selectedIngredientIcons.addAll(preselectedIcons)
        notifyDataSetChanged()
    }

    fun getSelectedIngredientIcons(): List<String> { // Return icons
        return selectedIngredientIcons.toList()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IngredientViewHolder {
        val binding = ItemIngredientBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return IngredientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: IngredientViewHolder, position: Int) {
        val ingredient = userIngredientList[position]
        holder.bind(ingredient)
    }

    override fun getItemCount(): Int {
        return userIngredientList.size
    }

    inner class IngredientViewHolder(private val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: UserIngredient) {
            binding.ingredientCheckBox.text = ingredient.name
            binding.ingredientCheckBox.isChecked = selectedIngredientIcons.contains(ingredient.icon)

            binding.ingredientCheckBox.setOnCheckedChangeListener(null)
            binding.ingredientCheckBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    selectedIngredientIcons.add(ingredient.icon)
                } else {
                    selectedIngredientIcons.remove(ingredient.icon)
                }
            }
        }
    }
}
