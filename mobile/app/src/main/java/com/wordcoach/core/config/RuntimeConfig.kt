package com.wordcoach.core.config

import android.os.Build
import com.wordcoach.BuildConfig

object RuntimeConfig {
  val apiBaseUrl: String = normalizeBaseUrl(resolveApiBaseUrl())
  val clientToken: String = BuildConfig.CLIENT_TOKEN

  private fun resolveApiBaseUrl(): String {
    if (!BuildConfig.DEBUG) return BuildConfig.API_BASE_URL
    if (isProbablyEmulator()) return BuildConfig.API_BASE_URL
    // On physical devices, 10.0.2.2 is not routable; we rely on adb reverse -> localhost.
    return BuildConfig.API_BASE_URL.replace("10.0.2.2", "127.0.0.1")
  }

  private fun normalizeBaseUrl(url: String): String {
    return if (url.endsWith("/")) url else "$url/"
  }

  private fun isProbablyEmulator(): Boolean {
    return Build.FINGERPRINT.startsWith("generic") ||
      Build.FINGERPRINT.lowercase().contains("emulator") ||
      Build.MODEL.contains("Emulator") ||
      Build.MODEL.contains("Android SDK built for x86") ||
      Build.MANUFACTURER.contains("Genymotion") ||
      (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
      "google_sdk" == Build.PRODUCT
  }
}
