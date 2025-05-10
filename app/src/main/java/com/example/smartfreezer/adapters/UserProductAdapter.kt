package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.models.UserProduct

class UserProductAdapter(
    private var productList: List<UserProduct>,
    private val onItemClick: (UserProduct) -> Unit,
    private val onItemLongClick: (UserProduct) -> Unit
) : RecyclerView.Adapter<UserProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.ivProductIconInventory)
        val nameTextView: TextView = view.findViewById(R.id.tvProductNameInventory)

        fun bind(product: UserProduct) {
            nameTextView.text = product.name.replaceFirstChar { it.uppercase() }
            iconImageView.setImageResource(product.iconDrawableRes)

            // Click normal - abrir detalles
            itemView.setOnClickListener {
                onItemClick(product)
            }

            // Long click - mostrar diálogo de eliminación
            itemView.setOnLongClickListener {
                onItemLongClick(product)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount() = productList.size

    fun updateData(newList: List<UserProduct>) {
        productList = newList
        notifyDataSetChanged()
    }
}