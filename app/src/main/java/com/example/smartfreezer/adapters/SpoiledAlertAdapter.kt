package com.example.smartfreezer.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.models.SpoiledProduct

class SpoiledAlertAdapter(
    private val onDeleteClick: (SpoiledProduct) -> Unit
) : RecyclerView.Adapter<SpoiledAlertAdapter.ViewHolder>() {

    private val products = mutableListOf<SpoiledProduct>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvLocation: TextView = itemView.findViewById(R.id.tvLocation)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val ivProductIcon: ImageView = itemView.findViewById(R.id.ivProductIcon)
        private val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
        private val context = itemView.context

        @SuppressLint("StringFormatInvalid", "StringFormatMatches")
        fun bind(product: SpoiledProduct) {
            tvProductName.text = product.name.replaceFirstChar { it.uppercase() }
            tvLocation.text = context.getString(
                R.string.ubicaci_n_spoiled,
                product.location.replaceFirstChar { it.uppercase() })
            tvQuantity.text = context.getString(R.string.cantidad_spoiled, product.quantity)

            val resId = itemView.context.resources.getIdentifier(product.icon, "drawable", itemView.context.packageName)
            ivProductIcon.setImageResource(resId)

            ivDelete.setOnClickListener { onDeleteClick(product) }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_spoiled_alert, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<SpoiledProduct>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}