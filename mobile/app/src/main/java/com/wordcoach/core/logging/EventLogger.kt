package com.wordcoach.core.logging

import android.util.Log

class EventLogger(private val tag: String = "WordCoach") {
  fun info(event: String, attrs: Map<String, Any?> = emptyMap()) {
    Log.i(tag, format(event, attrs))
  }

  fun warn(event: String, attrs: Map<String, Any?> = emptyMap()) {
    Log.w(tag, format(event, attrs))
  }

  fun error(event: String, throwable: Throwable? = null, attrs: Map<String, Any?> = emptyMap()) {
    Log.e(tag, format(event, attrs), throwable)
  }

  private fun format(event: String, attrs: Map<String, Any?>): String {
    val body = attrs.entries.joinToString(",") { "${it.key}=${it.value}" }
    return "event=$event attrs={$body}"
  }
}
