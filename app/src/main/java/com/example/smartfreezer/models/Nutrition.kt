package com.example.smartfreezer.models



import com.google.gson.annotations.SerializedName



data class Nutrition(
    @SerializedName("caloricBreakdown")
    val caloricBreakdown: CaloricBreakdown?,
    @SerializedName("flavanoids")
    val flavanoids: List<Flavanoid?>?,
    @SerializedName("ingredients")
    val ingredients: List<Ingredient?>?,
    @SerializedName("nutrients")
    val nutrients: List<Nutrient?>?,
    @SerializedName("properties")
    val properties: List<Property?>?,
    @SerializedName("weightPerServing")
    val weightPerServing: WeightPerServing?
)