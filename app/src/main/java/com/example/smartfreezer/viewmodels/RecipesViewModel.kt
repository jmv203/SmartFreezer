package com.example.smartfreezer.viewmodels

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.*
import com.example.smartfreezer.api.FoodRecipesApi
import com.example.smartfreezer.models.FoodRecipe
import com.example.smartfreezer.models.Result
import com.example.smartfreezer.util.NetworkResult
import com.example.smartfreezer.util.Constants.Companion.API_KEY
import com.example.smartfreezer.util.Constants.Companion.QUERY_ADD_RECIPE_INFORMATION
import com.example.smartfreezer.util.Constants.Companion.QUERY_API_KEY
import com.example.smartfreezer.util.Constants.Companion.QUERY_DIET
import com.example.smartfreezer.util.Constants.Companion.QUERY_FILL_INGREDIENTS
import com.example.smartfreezer.util.Constants.Companion.QUERY_NUMBER
import com.example.smartfreezer.util.Constants.Companion.QUERY_TYPE
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
                    android.util.Log.d("API_REQUEST_URL", fullUrl)
                    val response = foodRecipesApi.getRecipes(queries)
                    _recipesResponse.value = handleResponse(response)
                } catch (e: Exception) {
                    _recipesResponse.value = NetworkResult.Error("Error: ${e.message}")
                }
            } else {
                _recipesResponse.value = NetworkResult.Error("No Internet Connection")
            }
        }
    }

    private fun handleResponse(response: Response<FoodRecipe>): NetworkResult<List<Result>> {
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
        queries[QUERY_ADD_RECIPE_INFORMATION] = "true"
        queries[QUERY_FILL_INGREDIENTS] = "true"
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
}
