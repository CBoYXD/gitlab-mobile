package com.cboyxd.gitlab.data.api

import com.cboyxd.gitlab.BuildConfig
import com.cboyxd.gitlab.data.auth.GitLabAuth
import com.cboyxd.gitlab.data.model.GitLabInstance
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.ConcurrentHashMap
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

object GitLabApiClient {

  val moshi: Moshi = Moshi.Builder()
    .add(NullsAsDefaultsFactory)
    .add(KotlinJsonAdapterFactory())
    .build()

  /** One connection pool and dispatcher for every server; per-instance clients only add auth. */
  val baseHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
      .connectTimeout(15, TimeUnit.SECONDS)
      .readTimeout(30, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .callTimeout(60, TimeUnit.SECONDS)
      .retryOnConnectionFailure(true)
      .apply {
        if (BuildConfig.DEBUG) {
          addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
      }
      .build()
  }

  /** Tests point this at a client that trusts MockWebServer's certificate. */
  @Volatile
  internal var clientOverride: OkHttpClient? = null

  internal fun clearCache() = services.clear()

  private data class Entry(val key: String, val service: GitLabApiService)

  /** Keyed by instance id so a refreshed token replaces the old service instead of piling up. */
  private val services = ConcurrentHashMap<String, Entry>()

  fun createService(instance: GitLabInstance): GitLabApiService {
    val baseUrl = normalizeBaseUrl(instance.url)
    val key = baseUrl + "\u0000" + instance.token.trim() + "\u0000" + instance.authKind
    val slot = instance.id.ifBlank { baseUrl }
    services[slot]?.takeIf { it.key == key }?.let { return it.service }
    val service = buildService(baseUrl, instance.token, instance.authKind)
    services[slot] = Entry(key, service)
    return service
  }

  fun forget(instanceId: String) {
    services.remove(instanceId)
  }

  private fun buildService(baseUrl: String, token: String, authKind: String): GitLabApiService {
    val authInterceptor = Interceptor { chain ->
      val request = chain.request().newBuilder()
        .header("User-Agent", "GitLabMobile/1.0 (Android)")
        .header("Accept", "application/json")
        .apply { applyToken(token, authKind) }
        .build()
      chain.proceed(request)
    }

    val client = (clientOverride ?: baseHttpClient).newBuilder()
      .addInterceptor(authInterceptor)
      .build()

    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(GitLabApiService::class.java)
  }
}

/**
 * GitLab sends explicit `null` for many fields the models declare non-null with a default
 * (`author`, `name`, `ref`…). Codegen adapters reject that and one bad element used to fail a
 * whole list. This drops null members before decoding so the declared defaults apply.
 */
private object NullsAsDefaultsFactory : JsonAdapter.Factory {
  override fun create(type: Type, annotations: Set<Annotation>, moshi: Moshi): JsonAdapter<*>? {
    val raw = Types.getRawType(type)
    if (!raw.isAnnotationPresent(JsonClass::class.java)) return null
    val delegate = moshi.nextAdapter<Any>(this, type, annotations)
    return object : JsonAdapter<Any>() {
      override fun fromJson(reader: JsonReader): Any? {
        val value = reader.readJsonValue()
        val cleaned = if (value is Map<*, *>) splitLabelDetails(value.filterValues { it != null }) else value
        return delegate.fromJsonValue(cleaned)
      }

      override fun toJson(writer: JsonWriter, value: Any?) = delegate.toJson(writer, value)
    }.nullSafe()
  }
}

/**
 * With `with_labels_details=true` GitLab sends labels as objects. Keep `labels` as the list of
 * names every screen uses and expose the objects (with colors) as `label_details`.
 */
private fun splitLabelDetails(map: Map<*, *>): Map<*, *> {
  val labels = map["labels"] as? List<*> ?: return map
  if (labels.none { it is Map<*, *> }) return map
  val names = labels.mapNotNull { (it as? Map<*, *>)?.get("name") as? String ?: it as? String }
  val details = labels.filterIsInstance<Map<*, *>>().map { m -> m.filterValues { it != null } }
  return map + mapOf("labels" to names, "label_details" to details)
}

private fun okhttp3.Request.Builder.applyToken(token: String, authKind: String) {
  val clean = token.trim()
  if (clean.isEmpty()) return
  if (authKind == GitLabAuth.KIND_OAUTH || clean.startsWith("Bearer ", ignoreCase = true)) {
    val value = if (clean.startsWith("Bearer ", ignoreCase = true)) clean.substringAfter(' ').trim() else clean
    header("Authorization", "Bearer $value")
  } else {
    header("PRIVATE-TOKEN", clean)
  }
}

fun <T> Response<T>.requireBody(): T {
  if (!isSuccessful) throw toGitLabException()
  return body() ?: throw GitLabApiException(code(), "The server sent an empty response.")
}

fun <T> Response<List<T>>.requireList(): List<T> {
  if (!isSuccessful) throw toGitLabException()
  return body().orEmpty()
}

fun Response<*>.toGitLabException(): GitLabApiException {
  val raw = try {
    errorBody()?.use { it.string().take(4_000) }
  } catch (_: Exception) {
    null
  }
  return GitLabApiException(code(), parseGitLabError(raw, code()))
}
