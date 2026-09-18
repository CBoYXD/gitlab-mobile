package com.example.data.api

import com.example.data.model.GitLabInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object GitLabApiClient {

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  fun createService(instance: GitLabInstance): GitLabApiService {
    // Normalize Base URL to end with slash
    var baseUrl = instance.url.trim()
    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      baseUrl = "https://$baseUrl"
    }
    if (!baseUrl.endsWith("/")) {
      baseUrl = "$baseUrl/"
    }

    val authInterceptor = Interceptor { chain ->
      val original = chain.request()
      val requestBuilder = original.newBuilder()
        .header("User-Agent", "GitLabMobile-Android/1.0")
        .header("Accept", "application/json")

      if (instance.token.isNotBlank()) {
        val cleanToken = instance.token.trim()
        // GitLab Personal Access Token or OAuth Bearer token
        if (cleanToken.startsWith("glpat-") || cleanToken.length == 20 || cleanToken.length == 26) {
          requestBuilder.header("PRIVATE-TOKEN", cleanToken)
        } else {
          requestBuilder.header("Authorization", "Bearer $cleanToken")
        }
      }

      chain.proceed(requestBuilder.build())
    }

    val loggingInterceptor = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val okHttpClient = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .addInterceptor(loggingInterceptor)
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(20, TimeUnit.SECONDS)
      .build()

    val retrofit = Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(okHttpClient)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()

    return retrofit.create(GitLabApiService::class.java)
  }
}
