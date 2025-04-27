package com.example.smartfreezer.models



import com.google.gson.annotations.SerializedName

data class Result(
    @SerializedName("dishTypes")
    val dishTypes: List<String>,
    @SerializedName("id")
    val id: Int,
    @SerializedName("image")
    val image: String,
    @SerializedName("readyInMinutes")
    val readyInMinutes: Int,
    @SerializedName("spoonacularScore")
    val spoonacularScore: Double,
    @SerializedName("title")
    val title: String,
)