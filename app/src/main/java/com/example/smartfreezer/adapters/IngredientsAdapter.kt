package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.databinding.ItemIngredientBinding
import com.example.smartfreezer.models.Ingredients

class IngredientsAdapter : RecyclerView.Adapter<IngredientsAdapter.IngredientViewHolder>() {

    private var ingredientsList = listOf<Ingredients>()
    private val selectedIngredientIcons = mutableSetOf<String>() // Store icons

    // Modifica la función setIngredients para eliminar duplicados basados en el nombre
    fun setIngredients(ingredients: List<Ingredients>, preselectedIcons: List<String> = emptyList()) {
        val uniqueIngredients = ingredients
            .groupBy { it.name }
            .map { it.value.first() }

        ingredientsList = uniqueIngredients
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
        val ingredient = ingredientsList[position]
        holder.bind(ingredient)
    }

    override fun getItemCount(): Int {
        return ingredientsList.size
    }

    inner class IngredientViewHolder(private val binding: ItemIngredientBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: Ingredients) {
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
