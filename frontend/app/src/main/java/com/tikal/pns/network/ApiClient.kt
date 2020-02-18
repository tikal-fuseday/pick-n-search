package com.tikal.pns.network

import retrofit2.Retrofit

import retrofit2.converter.gson.GsonConverterFactory


class ApiClient {


    companion object {
        fun create(): ApiService {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://us-central1-pick-n-search.cloudfunctions.net/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return retrofit.create(ApiService::class.java)
        }
    }
}