package com.example.smartfreezer.models

data class ShoppingItem(
    val id: String = "",
    val name: String = "",
    val icon: String = "",
    val category: String = "",
    var quantity: Int = 1
)
