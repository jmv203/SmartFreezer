package com.example.smartfreezer.adapters


import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.models.SeasonProduct

class SeasonAlertAdapter(
    private val onAddToListClick: (SeasonProduct) -> Unit
) : RecyclerView.Adapter<SeasonAlertAdapter.ViewHolder>() {

    private val products = mutableListOf<SeasonProduct>()

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        private val tvSeasonInfo: TextView = itemView.findViewById(R.id.tvSeasonInfo)
        private val ivProductIcon: ImageView = itemView.findViewById(R.id.ivProductIcon)
        private val btnAddToList: Button = itemView.findViewById(R.id.btnAddToList)
        private val context = itemView.context

        fun bind(product: SeasonProduct) {
            tvProductName.text = product.name.replaceFirstChar { it.uppercase() }

            val startMonthName = getMonthName(product.startSeason)
            val lastMonthName = getMonthName(product.lastSeason)
            tvSeasonInfo.text = context.getString(
                R.string.season_info,
                startMonthName,
                lastMonthName
            )

            val resId = context.resources.getIdentifier(
                product.icon,
                "drawable",
                context.packageName
            )
            ivProductIcon.setImageResource(resId)

            btnAddToList.setOnClickListener { onAddToListClick(product) }
        }

        private fun getMonthName(month: Int): String {
            val monthResId = when (month) {
                1 -> R.string.month_january
                2 -> R.string.month_february
                3 -> R.string.month_march
                4 -> R.string.month_april
                5 -> R.string.month_may
                6 -> R.string.month_june
                7 -> R.string.month_july
                8 -> R.string.month_august
                9 -> R.string.month_september
                10 -> R.string.month_october
                11 -> R.string.month_november
                12 -> R.string.month_december
                else -> R.string.month_unknown
            }
            return context.getString(monthResId)
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_season_alert, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(products[position])
    }

    override fun getItemCount() = products.size

    fun updateData(newProducts: List<SeasonProduct>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}