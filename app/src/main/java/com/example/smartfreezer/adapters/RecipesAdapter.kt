package com.example.smartfreezer.adapters


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.databinding.RecipesRowLayoutBinding
import com.example.smartfreezer.models.Result


class RecipesAdapter : RecyclerView.Adapter<RecipesAdapter.MyViewHolder>() {

    private var recipes = emptyList<Result>()
    private var onItemClickListener: ((Int) -> Unit)? = null
    private var onSaveClickListener: ((Result) -> Unit)? = null
    private var savedRecipeIds = setOf<Int>()

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    fun setOnSaveClickListener(listener: (Result) -> Unit) {
        onSaveClickListener = listener
    }

    fun setSavedRecipeIds(ids: Set<Int>) {
        savedRecipeIds = ids
        notifyDataSetChanged()
    }


    class MyViewHolder(val binding: RecipesRowLayoutBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(result: Result) {
            binding.result = result
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = RecipesRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun getItemCount(): Int {
        return recipes.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentRecipe = recipes[position]
        holder.bind(currentRecipe)

        // Set click listener on the item view
        holder.itemView.setOnClickListener {
            onItemClickListener?.invoke(currentRecipe.id)
        }

        // Set click listener on the save button
        holder.binding.fabSaveRecipeItem.setOnClickListener {
            onSaveClickListener?.invoke(currentRecipe)
        }

        // Update the save button icon
        updateSaveButtonIcon(holder, currentRecipe)
    }

    private fun updateSaveButtonIcon(holder: MyViewHolder, currentRecipe: Result) {
        if (savedRecipeIds.contains(currentRecipe.id)) {
            holder.binding.fabSaveRecipeItem.setImageResource(R.drawable.ic_save_filled) // Filled icon
        } else {
            holder.binding.fabSaveRecipeItem.setImageResource(R.drawable.ic_save_border) // Bordered icon
        }
    }


    fun setData(newData: List<Result>) {
        recipes = newData
        notifyDataSetChanged()
    }
}