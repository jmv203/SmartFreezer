package com.example.smartfreezer.models

import android.graphics.RectF

data class Detection(val label: String, val confidence: Float, val rect: RectF)