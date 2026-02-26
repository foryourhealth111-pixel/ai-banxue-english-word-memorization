package com.wordcoach.core.storage

data class CoachCacheEntry(
  val word: String,
  val explanation: String,
  val updatedAtMillis: Long
)

class CoachCacheDao {
  private val cache = linkedMapOf<String, CoachCacheEntry>()
  private val ttlMillis = 7L * 24 * 60 * 60 * 1000

  @Synchronized
  fun upsert(word: String, explanation: String, now: Long = System.currentTimeMillis()) {
    cache[word.lowercase()] = CoachCacheEntry(word.lowercase(), explanation, now)
  }

  @Synchronized
  fun find(word: String, now: Long = System.currentTimeMillis()): CoachCacheEntry? {
    val key = word.lowercase()
    val entry = cache[key] ?: return null
    if (now - entry.updatedAtMillis > ttlMillis) {
      cache.remove(key)
      return null
    }
    return entry
  }
}
