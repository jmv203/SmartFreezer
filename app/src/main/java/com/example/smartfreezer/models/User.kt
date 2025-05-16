package com.example.smartfreezer.models

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val vegan: Boolean = false,
    val vegetarian: Boolean = false,
    val glutenFree: Boolean = false,
    val dairyFree: Boolean = false
)