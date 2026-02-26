package com.wordcoach.core.network

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.wordcoach.core.config.RuntimeConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
  fun createApiService(): ApiService {
    val authInterceptor = Interceptor { chain ->
      val request = chain.request().newBuilder()
        .addHeader("X-Client-Token", RuntimeConfig.clientToken)
        .build()
      chain.proceed(request)
    }

    val httpClient = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
      .connectTimeout(8, TimeUnit.SECONDS)
      .readTimeout(8, TimeUnit.SECONDS)
      .writeTimeout(8, TimeUnit.SECONDS)
      .build()

    val moshi = Moshi.Builder()
      .add(KotlinJsonAdapterFactory())
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(RuntimeConfig.apiBaseUrl)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .client(httpClient)
      .build()

    return retrofit.create(ApiService::class.java)
  }
}
