package com.example.smartfreezer.models


import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.android.parcel.Parcelize


@Parcelize
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
): Parcelable