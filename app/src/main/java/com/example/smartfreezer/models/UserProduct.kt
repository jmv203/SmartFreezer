package com.example.smartfreezer.models

data class UserProduct(
    val idProduct: String = "",
    val name: String = "",
    val icon: String = "", // nombre del recurso (por ejemplo: "banana_icon")
    val category: String = "",
    val condition: String = "",
    val location: String = "",
    val idUser: String = "",
    val startSeason: Int = 0,
    val lastSeason: Int = 0,
    var iconDrawableRes: Int = 0, // ← esto es para guardar el ID del recurso drawable
    val quantity: Int
)

