package com.example.smartfreezer.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.models.ShoppingItem

class ShoppingListAdapter(
    private val items: MutableList<ShoppingItem>,
    private val onIncrease: (ShoppingItem) -> Unit,
    private val onDecrease: (ShoppingItem) -> Unit
) : RecyclerView.Adapter<ShoppingListAdapter.ShoppingListViewHolder>() {

    inner class ShoppingListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val icon: ImageView = view.findViewById(R.id.ivProductIcon)
        val name: TextView = view.findViewById(R.id.tvProductName)
        val quantity: TextView = view.findViewById(R.id.tvQuantity)
        val btnIncrease: ImageView = view.findViewById(R.id.btnIncrease)
        val btnDecrease: ImageView = view.findViewById(R.id.btnDecrease)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShoppingListViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shopping_product, parent, false)
        return ShoppingListViewHolder(view)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ShoppingListViewHolder, position: Int) {
        val item = items[position]
        val context = holder.itemView.context

        holder.name.text = item.name.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase() else it.toString()
        }
        holder.quantity.text = item.quantity.toString()

        val iconRes = context.resources.getIdentifier(item.icon, "drawable", context.packageName)
        holder.icon.setImageResource(iconRes)

        // Cambia el icono dependiendo de la cantidad
        if (item.quantity > 1) {
            holder.btnDecrease.setImageResource(R.drawable.subtract) // icono de menos
        } else {
            holder.btnDecrease.setImageResource(R.drawable.delete) // icono de basura
        }

        holder.btnIncrease.setOnClickListener {
            onIncrease(item)
        }

        holder.btnDecrease.setOnClickListener {
            onDecrease(item)
        }
    }
}
