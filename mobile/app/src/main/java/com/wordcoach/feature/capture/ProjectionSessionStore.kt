package com.wordcoach.feature.capture

import android.content.Intent

object ProjectionSessionStore {
  @Volatile
  private var resultCode: Int? = null
  @Volatile
  private var resultData: Intent? = null

  fun save(resultCode: Int, data: Intent?) {
    this.resultCode = resultCode
    this.resultData = data?.let { Intent(it) }
  }

  fun clear() {
    resultCode = null
    resultData = null
  }

  fun hasSession(): Boolean = resultCode != null && resultData != null

  fun readSession(): Pair<Int, Intent>? {
    val code = resultCode ?: return null
    val data = resultData ?: return null
    return code to data
  }
}
