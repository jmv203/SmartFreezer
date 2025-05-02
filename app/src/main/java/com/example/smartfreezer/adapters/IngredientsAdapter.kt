package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.databinding.IngredientsRowLayoutBinding
import com.example.smartfreezer.models.ExtendedIngredient

class IngredientsAdapter : RecyclerView.Adapter<IngredientsAdapter.MyViewHolder>() {

    private var ingredients = emptyList<ExtendedIngredient>()

    class MyViewHolder(private val binding: IngredientsRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(ingredient: ExtendedIngredient) {
            binding.ingredient = ingredient
            binding.name.text = ingredient.nameClean  // Asegurar que el texto se establece aquí
            binding.name.isSelected = true          // Activar el marquee
            binding.name.requestFocus()            // Solicitar el foco
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = IngredientsRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentIngredient = ingredients[position]
        holder.bind(currentIngredient)
    }

    override fun getItemCount(): Int {
        return ingredients.size
    }

    fun setData(newIngredients: List<ExtendedIngredient>) {
        ingredients = newIngredients
        notifyDataSetChanged()
    }
}