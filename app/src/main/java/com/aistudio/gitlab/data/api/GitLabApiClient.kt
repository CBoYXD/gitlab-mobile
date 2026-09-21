package com.aistudio.gitlab.data.api

import com.aistudio.gitlab.BuildConfig
import com.aistudio.gitlab.data.model.GitLabInstance
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

object GitLabApiClient {

  private val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  private val services = ConcurrentHashMap<String, GitLabApiService>()

  fun createService(instance: GitLabInstance): GitLabApiService {
    val baseUrl = normalizeBaseUrl(instance.url)
    val cacheKey = baseUrl + "\u0000" + instance.token.trim()
    return services.getOrPut(cacheKey) { buildService(baseUrl, instance.token) }
  }

  private fun buildService(baseUrl: String, token: String): GitLabApiService {
    val authInterceptor = Interceptor { chain ->
      val request = chain.request().newBuilder()
        .header("User-Agent", "GitLabMobile/1.0 (Android)")
        .header("Accept", "application/json")
        .apply { applyToken(token) }
        .build()
      chain.proceed(request)
    }

    val clientBuilder = OkHttpClient.Builder()
      .addInterceptor(authInterceptor)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)

    if (BuildConfig.DEBUG) {
      val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
      clientBuilder.addInterceptor(logging)
    }

    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(clientBuilder.build())
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GitLabApiService::class.java)
  }
}

private fun okhttp3.Request.Builder.applyToken(token: String) {
  val clean = token.trim()
  if (clean.isEmpty()) return
  if (clean.startsWith("Bearer ", ignoreCase = true)) {
    header("Authorization", "Bearer " + clean.substringAfter(' ').trim())
  } else {
    header("PRIVATE-TOKEN", clean)
  }
}

fun <T> Response<T>.requireBody(): T {
  if (!isSuccessful) throw toGitLabException()
  return body() ?: throw GitLabApiException(code(), "Empty response")
}

fun <T> Response<List<T>>.requireList(): List<T> {
  if (!isSuccessful) throw toGitLabException()
  return body().orEmpty()
}

fun Response<*>.toGitLabException(): GitLabApiException {
  val raw = try {
    errorBody()?.string()
  } catch (_: Exception) {
    null
  }
  return GitLabApiException(code(), parseGitLabError(raw, code()))
}
