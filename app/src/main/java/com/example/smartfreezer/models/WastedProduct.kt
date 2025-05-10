package com.example.smartfreezer.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class WastedProduct(
    @PropertyName("name") val name: String = "",
    @PropertyName("icon") val icon: String = "",
    @PropertyName("date") val date: Timestamp = Timestamp.now(),
    @PropertyName("category") val category: String = "",
    @PropertyName("original_product_id") val originalProductId: String = "" // Para evitar duplicados
)