package com.cboyxd.gitlab

import android.app.Application
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.SvgDecoder
import coil.disk.DiskCache
import kotlinx.coroutines.flow.MutableStateFlow
import okhttp3.OkHttpClient

/** One shared image cache and connectivity observer for all avatar requests. */
class GitLabApplication : Application(), ImageLoaderFactory {
  val networkChanges = MutableStateFlow(0L)
  private val connectivity by lazy { getSystemService(ConnectivityManager::class.java) }

  val isOnline: Boolean
    get() = connectivity.getNetworkCapabilities(connectivity.activeNetwork)
      ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

  override fun onCreate() {
    super.onCreate()
    connectivity.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
      override fun onAvailable(network: Network) {
        networkChanges.value += 1
      }

      override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)) {
          networkChanges.value += 1
        }
      }
    })
  }

  override fun newImageLoader(): ImageLoader = ImageLoader.Builder(this)
    .components { add(SvgDecoder.Factory()) }
    .diskCache {
      DiskCache.Builder().directory(cacheDir.resolve("images")).maxSizeBytes(64L * 1024 * 1024).build()
    }
    .okHttpClient {
      OkHttpClient.Builder().addNetworkInterceptor { chain ->
        val original = chain.call().request().url
        val request = chain.request()
        val target = request.url
        val sameOrigin = original.scheme == target.scheme && original.host == target.host && original.port == target.port
        // A redirected avatar must never forward a GitLab token to another server.
        chain.proceed(if (sameOrigin) request else request.newBuilder()
          .removeHeader("PRIVATE-TOKEN").removeHeader("Authorization").build())
      }.build()
    }
    .build()
}
