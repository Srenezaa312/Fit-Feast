package com.example.fitfeast

import retrofit2.http.GET
import retrofit2.http.Query

interface NewsApiService {
    @GET("v2/everything")
    suspend fun getEverything(
        @Query("apiKey") apiKey: String,
        @Query("q") query: String,
        @Query("pageSize") pageSize: Int = 20,
        @Query("language") language: String = "en"
    ): NewsResponse
}

