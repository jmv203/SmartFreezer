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
    private var productList: List<UserProduct>
) : RecyclerView.Adapter<UserProductAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val iconImageView: ImageView = view.findViewById(R.id.ivProductIconInventory)
        val nameTextView: TextView = view.findViewById(R.id.tvProductNameInventory)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product_inventory, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val product = productList[position]

        holder.nameTextView.text = product.name
        holder.iconImageView.setImageResource(product.iconDrawableRes)
    }

    override fun getItemCount() = productList.size

    fun updateData(newList: List<UserProduct>) {
        productList = newList
        notifyDataSetChanged()
    }
}
