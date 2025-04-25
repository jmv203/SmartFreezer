package com.example.smartfreezer.navigation

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.smartfreezer.R
import com.example.smartfreezer.adapters.RecipesAdapter
import com.example.smartfreezer.databinding.FragmentRecipesBinding
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.viewmodels.RecipesViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RecipesFragment : Fragment(R.layout.fragment_recipes) {

    private lateinit var binding: FragmentRecipesBinding
    private val recipesAdapter by lazy { RecipesAdapter() }

    // ViewModel con Hilt
    private val recipesViewModel: RecipesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding = FragmentRecipesBinding.bind(view)

        setupRecyclerView()
        requestApiData()
    }

    private fun setupRecyclerView() {
        binding.recipesRecyclerView.apply {
            adapter = recipesAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    private fun requestApiData() {
        recipesViewModel.getRecipes(recipesViewModel.applyQueries())

        recipesViewModel.recipesResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is NetworkResult.Success -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    recipesAdapter.setData(response.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    binding.shimmerLayout.stopShimmer()
                    binding.shimmerLayout.visibility = View.GONE
                    // Puedes mostrar un error con Snackbar o Toast aquí
                }
                is NetworkResult.Loading -> {
                    binding.shimmerLayout.startShimmer()
                    binding.shimmerLayout.visibility = View.VISIBLE
                }
            }
        }
    }
}
