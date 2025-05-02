package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.databinding.NutrientRowLayoutBinding
import com.example.smartfreezer.models.Nutrient

class NutrientsAdapter : RecyclerView.Adapter<NutrientsAdapter.NutrientViewHolder>() {

    private var nutrientsList = emptyList<Nutrient>()

    inner class NutrientViewHolder(private val binding: NutrientRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(nutrient: Nutrient) {
            binding.nutrient = nutrient
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NutrientViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = NutrientRowLayoutBinding.inflate(inflater, parent, false)
        return NutrientViewHolder(binding)
    }

    override fun onBindViewHolder(holder: NutrientViewHolder, position: Int) {
        holder.bind(nutrientsList[position])
    }

    override fun getItemCount(): Int = nutrientsList.size

    fun setData(newData: List<Nutrient>) {
        nutrientsList = newData
        notifyDataSetChanged()
    }
}
