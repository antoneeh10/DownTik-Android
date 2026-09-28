package com.example.data.network

import com.example.data.model.TikwmResponseJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    private const val BASE_URL = "https://www.tikwm.com/"
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Mobile; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36"

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val request = original.newBuilder()
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json, text/plain, */*")
            .header("Accept-Language", "en-US,en;q=0.9,id;q=0.8")
            .build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    val apiService: TikwmApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(TikwmApiService::class.java)
    }
}
