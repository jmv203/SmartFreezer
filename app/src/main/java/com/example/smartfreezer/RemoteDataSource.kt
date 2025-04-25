package com.example.smartfreezer

import com.example.smartfreezer.api.FoodRecipesApi
import com.example.smartfreezer.models.FoodRecipe
import javax.inject.Inject
import retrofit2.Response

class RemoteDataSource @Inject constructor(
    private val foodRecipesApi: FoodRecipesApi
) {
    suspend fun getRecipes(queries: Map<String, String>): Response<FoodRecipe> {
        return  foodRecipesApi.getRecipes(queries)
    }
}