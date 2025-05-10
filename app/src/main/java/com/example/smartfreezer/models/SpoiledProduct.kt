package com.example.smartfreezer.models

data class SpoiledProduct(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val condition: String = "",
    val quantity: Int = 0,
    val location: String = ""
)