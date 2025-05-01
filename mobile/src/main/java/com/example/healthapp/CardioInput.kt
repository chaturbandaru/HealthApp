package com.example.healthapp

data class CardioInput(
    val age: Int,
    val gender: Int,
    val ap_hi: Int,
    val ap_lo: Int,
    val smoke: Int,
    val alco: Int,
    val active: Int,
    val BMI: Double
)

data class PredictionResponse(
    val prediction: Int
)