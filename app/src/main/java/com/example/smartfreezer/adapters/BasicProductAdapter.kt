package com.example.smartfreezer.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.models.BasicProduct

class BasicProductAdapter(
    private var productList: List<BasicProduct>,
    private val onItemClick: (BasicProduct) -> Unit // Función lambda para manejar clics
) : RecyclerView.Adapter<BasicProductAdapter.BasicProductViewHolder>() { // Corregido aquí

    // ViewHolder para los productos
    class BasicProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val icon: ImageView = itemView.findViewById(R.id.productIconAdd)
        val name: TextView = itemView.findViewById(R.id.productNameAdd)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BasicProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return BasicProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: BasicProductViewHolder, position: Int) {
        val item = productList[position]

        // Cargar imagen desde drawable
        val context = holder.itemView.context
        val iconResId = context.resources.getIdentifier(item.icon, "drawable", context.packageName)

        holder.icon.setImageResource(iconResId)

        holder.name.text = item.name

        // Configurar el clic en el elemento
        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = productList.size

    fun updateData(newList: List<BasicProduct>) {
        productList = newList
        notifyDataSetChanged()
    }
}
