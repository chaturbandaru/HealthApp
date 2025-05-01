package com.example.healthapp

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("predict")
    fun predict(@Body input: CardioInput): Call<PredictionResponse>
}
