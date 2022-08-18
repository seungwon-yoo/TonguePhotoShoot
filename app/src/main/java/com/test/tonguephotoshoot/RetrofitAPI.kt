package com.test.tonguephotoshoot

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.*

data class ResponseDC(var result: String? = null)

interface RetrofitAPI {
    @Multipart
    @POST("/upload")
    fun uploadImage(
        @Query("id") id: Int,
        @Part image: MultipartBody.Part
    ): Call<ResponseDC>
}