package com.example.smartfreezer.api

import com.example.smartfreezer.models.FoodRecipe
import com.example.smartfreezer.models.RecipeDetails
import com.example.smartfreezer.models.Result
import com.example.smartfreezer.util.Constants
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap


interface FoodRecipesApi {

    @GET("/recipes/complexSearch")
    suspend fun getRecipes(
        @QueryMap queries: Map<String, String>
    ): Response<FoodRecipe>

    @GET("/recipes/{id}/information")
    suspend fun getRecipeDetails(
        @Path("id") recipeId: Int,
        @Query("apiKey") apiKey: String,
        @Query("includeNutrition") includeNutrition: Boolean
    ): Response<RecipeDetails>

    @GET("/recipes/{id}/analyzedInstructions")
    suspend fun getAnalyzedRecipeInstructions(
        @Path("id") recipeId: Int,
        @Query("apiKey") apiKey: String,
        @Query("stepBreakdown") stepBreakdown: Boolean
    ): Response<List<RecipeDetails>>

    @GET("recipes/informationBulk")
    suspend fun getBulkRecipes(
        @Query("ids") ids: String,
        @Query("apiKey") apiKey: String = Constants.API_KEY
    ): Response<List<Result>>



}
