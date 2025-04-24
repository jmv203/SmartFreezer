package com.example.smartfreezer.api

import com.example.smartfreezer.models.FoodRecipe
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface SpoonacularApi {

    @GET("/recipes/complexSearch")
    suspend fun getRecipes(
        @QueryMap queries: Map<String, String>
    ): Response<FoodRecipe>

    @GET("recipes/complexSearch")
    suspend fun getRecipesByType(
        @Query("type") type: String,
        @Query("apiKey") apiKey: String = "e605087d81d8481cabf5cb2bd36516dc"
    ): Response<RecipeSearchResponse>
}
