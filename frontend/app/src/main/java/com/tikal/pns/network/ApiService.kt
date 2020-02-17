package com.tikal.pns.network


import retrofit2.Call

import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    //I preferred hard coded "api key" instead injected it from outside
    @POST("publish")
    fun sendToServer(@Body request: Request): Call<GeneralResponse>

}