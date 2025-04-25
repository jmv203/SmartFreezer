package com.example.smartfreezer.navigation

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.CategoryAdapter
import com.example.smartfreezer.models.Category

class SelectCategoryFragment : Fragment(R.layout.fragment_select_category) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var btnBack: ImageView // Añade esta línea

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        recyclerView = view.findViewById(R.id.recyclerCategories)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        btnBack = view.findViewById(R.id.btnBack) // Inicializa el botón de retroceso

        val categories = listOf(
            Category("Fruta", R.drawable.ic_fruit),
            Category("Verdura", R.drawable.ic_vegetable)
        )

        recyclerView.adapter = CategoryAdapter(categories) { selected ->
            val action = SelectCategoryFragmentDirections
                .actionSelectCategoryFragmentToSelectProductFragment(selected.name)
            findNavController().navigate(action)
        }

        // Lógica para la flecha de retroceso
        btnBack.setOnClickListener {
            findNavController().navigateUp() // Vuelve al Fragment anterior en la pila (InventoryFragment)
        }
    }
}