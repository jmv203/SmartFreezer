package com.example.smartfreezer.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.*
import com.example.smartfreezer.api.FoodRecipesApi
import com.example.smartfreezer.models.*
import com.example.smartfreezer.util.Constants.Companion.API_KEY
import com.example.smartfreezer.util.Constants.Companion.QUERY_ADD_NUTRITION
import com.example.smartfreezer.util.Constants.Companion.QUERY_ADD_RECIPE_INFORMATION
import com.example.smartfreezer.util.Constants.Companion.QUERY_API_KEY
import com.example.smartfreezer.util.Constants.Companion.QUERY_DIET
import com.example.smartfreezer.util.Constants.Companion.QUERY_FILL_INGREDIENTS
import com.example.smartfreezer.util.Constants.Companion.QUERY_INSTRUCTION_REQUIRED
import com.example.smartfreezer.util.Constants.Companion.QUERY_NUMBER
import com.example.smartfreezer.util.Constants.Companion.QUERY_TYPE
import com.example.smartfreezer.util.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class RecipesViewModel @Inject constructor(
    private val foodRecipesApi: FoodRecipesApi,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _recipesResponse = MutableLiveData<NetworkResult<List<Result>>>()
    val recipesResponse: LiveData<NetworkResult<List<Result>>> get() = _recipesResponse

    private val _recipeDetailsResponse = MutableLiveData<NetworkResult<RecipeDetails>>()
    val recipeDetailsResponse: LiveData<NetworkResult<RecipeDetails>> get() = _recipeDetailsResponse

    private val _recipeInstructionsResponse = MutableLiveData<NetworkResult<List<RecipeDetails>>>()
    val recipeInstructionsResponse: LiveData<NetworkResult<List<RecipeDetails>>> get() = _recipeInstructionsResponse

    var selectedType: String = ""
    var selectedDiet: String = ""
    var selectedRating: Int = 0
    var selectedIngredients: List<String> = emptyList()

    var offset: Int = 0

    fun getRecipes(queries: Map<String, String>) {
        _recipesResponse.value = NetworkResult.Loading()

        viewModelScope.launch {
            if (hasInternetConnection()) {
                try {
                    val queryString = queries.map { "${it.key}=${it.value}" }.joinToString("&")
                    val fullUrl = "https://api.spoonacular.com/recipes/complexSearch?$queryString"
                    Log.d("API_REQUEST_URL", fullUrl)
                    val response = foodRecipesApi.getRecipes(queries)
                    _recipesResponse.value = handleRecipesResponse(response)
                } catch (e: Exception) {
                    _recipesResponse.value = NetworkResult.Error("Error: ${e.message}")
                }
            } else {
                _recipesResponse.value = NetworkResult.Error("No Internet Connection")
            }
        }
    }

    private fun handleRecipesResponse(response: Response<FoodRecipe>): NetworkResult<List<Result>> {
        return if (response.isSuccessful) {
            val data = response.body()?.results
            if (data != null && data.isNotEmpty()) {
                NetworkResult.Success(data)
            } else {
                NetworkResult.Error("No recipes found")
            }
        } else {
            NetworkResult.Error("Error: ${response.message()}")
        }
    }

    fun getRecipeDetails(recipeId: Int) {
        _recipeDetailsResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            if (hasInternetConnection()) {
                try {
                    val response = foodRecipesApi.getRecipeDetails(recipeId, API_KEY, true) // Include nutrition
                    _recipeDetailsResponse.value = handleRecipeDetailsResponse(response)
                } catch (e: Exception) {
                    _recipeDetailsResponse.value = NetworkResult.Error("Error: ${e.message}")
                }
            } else {
                _recipeDetailsResponse.value = NetworkResult.Error("No Internet Connection")
            }
        }
    }

    private fun handleRecipeDetailsResponse(response: Response<RecipeDetails>): NetworkResult<RecipeDetails> {
        return if (response.isSuccessful) {
            response.body()?.let {
                NetworkResult.Success(it)
            } ?: NetworkResult.Error("No se encontraron detalles de la receta")
        } else {
            NetworkResult.Error("Error: ${response.message()}")
        }
    }

    fun getRecipeInstructions(recipeId: Int, stepBreakdown: Boolean = false) {
        _recipeInstructionsResponse.value = NetworkResult.Loading()
        viewModelScope.launch {
            if (hasInternetConnection()) {
                try {
                    val response = foodRecipesApi.getAnalyzedRecipeInstructions(recipeId, API_KEY, stepBreakdown)
                    _recipeInstructionsResponse.value = handleRecipeInstructionsResponse(response)
                } catch (e: Exception) {
                    _recipeInstructionsResponse.value = NetworkResult.Error("Error: ${e.message}")
                }
            } else {
                _recipeInstructionsResponse.value = NetworkResult.Error("No Internet Connection")
            }
        }
    }

    private fun handleRecipeInstructionsResponse(response: Response<List<RecipeDetails>>): NetworkResult<List<RecipeDetails>> {
        return if (response.isSuccessful) {
            response.body()?.let {
                NetworkResult.Success(it)
            } ?: NetworkResult.Error("No se encontraron instrucciones")
        } else {
            NetworkResult.Error("Error: ${response.message()}")
        }
    }

    private fun hasInternetConnection(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    fun applyQueries(): HashMap<String, String> {
        val queries = HashMap<String, String>()
        queries[QUERY_NUMBER] = "20"
        queries[QUERY_API_KEY] = API_KEY
        queries[QUERY_TYPE] = selectedType
        queries[QUERY_DIET] = selectedDiet
        queries[QUERY_INSTRUCTION_REQUIRED] = "true"
        queries[QUERY_ADD_RECIPE_INFORMATION] = "true"
        queries[QUERY_FILL_INGREDIENTS] = "true"
        queries[QUERY_ADD_NUTRITION] = "true"
        queries["offset"] = offset.toString()

        if (selectedIngredients.isNotEmpty()) {
            queries["includeIngredients"] = selectedIngredients.joinToString(",")
        }

        return queries
    }

    fun updateFilters(type: String, diet: String, rating: Int, ingredients: List<String>) {
        selectedType = type
        selectedDiet = diet
        selectedRating = rating
        selectedIngredients = ingredients
        offset = 0
    }

    fun resetOffset() {
        offset = 0
    }

    fun increaseOffset() {
        offset += 20
    }

    fun resetFilters() {
        selectedType = ""
        selectedDiet = ""
        selectedRating = 0
        selectedIngredients = emptyList()
    }
}