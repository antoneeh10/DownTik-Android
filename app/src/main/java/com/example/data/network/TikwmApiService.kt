package com.example.data.network

import com.example.data.model.TikwmResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface TikwmApiService {

    /**
     * Endpoint: https://www.tikwm.com/api/?url={VIDEO_URL}&hd=1
     * Retrofit with encoded=false will safely URL-encode the url parameter,
     * ensuring any '?', '&', '=', or other query parameters inside the video link
     * do not corrupt the request.
     */
    @GET("api/")
    suspend fun getVideoInfo(
        @Query("url", encoded = false) url: String,
        @Query("hd") hd: Int = 1
    ): TikwmResponse
}
