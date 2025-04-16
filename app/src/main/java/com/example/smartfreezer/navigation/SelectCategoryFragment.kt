package com.example.smartfreezer.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.CategoryAdapter
import com.example.smartfreezer.models.Category

class SelectCategoryFragment : Fragment(R.layout.fragment_select_category) {

    private lateinit var recyclerView: RecyclerView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerCategories)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val categories = listOf(
            Category("Fruta", R.drawable.ic_fruit),
            Category("Verdura", R.drawable.ic_vegetable)
        )

        recyclerView.adapter = CategoryAdapter(categories) { selected ->
            val action = SelectCategoryFragmentDirections
                .actionSelectCategoryFragmentToSelectProductFragment(selected.name)
            findNavController().navigate(action)
        }
    }
}
