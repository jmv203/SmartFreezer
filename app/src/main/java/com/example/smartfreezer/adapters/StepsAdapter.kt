package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.databinding.StepsRowLayoutBinding
import com.example.smartfreezer.models.Step

class StepsAdapter : RecyclerView.Adapter<StepsAdapter.MyViewHolder>() {

    private var steps = emptyList<Step>()

    class MyViewHolder(private val binding: StepsRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(step: Step) {
            binding.step = step
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): MyViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = StepsRowLayoutBinding.inflate(layoutInflater, parent, false)
                return MyViewHolder(binding)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val currentStep = steps[position]
        holder.bind(currentStep)
    }

    override fun getItemCount(): Int {
        return steps.size
    }

    fun setData(newSteps: List<Step>) {
        steps = newSteps
        notifyDataSetChanged()
    }
}