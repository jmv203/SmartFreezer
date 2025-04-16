package com.example.smartfreezer.models
import java.util.Date

data class UserProduct(
    val idProduct: String = "",
    val name: String = "",
    val icon: String = "", // nombre del recurso (por ejemplo: "banana_icon")
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val expirationDate: Date? = null,
    val purchaseDate: Date? = null,
    val idUser: String = "",
    val nutritionFacts: Map<String, String> = emptyMap(),
    val startSeason: Int = 0,
    val lastSeason: Int = 0,
    var iconDrawableRes: Int = 0 // ← esto es para guardar el ID del recurso drawable
)

