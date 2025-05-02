package com.example.smartfreezer.models



import com.google.gson.annotations.SerializedName



data class Step(
    @SerializedName("equipment")
    val equipment:  List<Equipment?>?,
    @SerializedName("ingredients")
    val ingredients:  List<Any>?,
    @SerializedName("number")
    val number: Int?,
    @SerializedName("step")
    val step: String?
)