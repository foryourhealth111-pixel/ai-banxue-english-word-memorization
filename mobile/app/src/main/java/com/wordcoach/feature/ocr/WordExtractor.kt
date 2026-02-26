package com.wordcoach.feature.ocr

import kotlin.math.abs

data class ExtractionResult(
  val primary: String?,
  val candidates: List<String>,
  val ambiguous: Boolean
)

class WordExtractor {
  private val englishRegex = Regex("^[A-Za-z-]{2,25}$")
  private val stopWords = setOf("ok", "menu", "vip", "next", "skip", "home", "back")

  fun extract(words: List<OcrWord>, imageWidth: Int, imageHeight: Int): ExtractionResult {
    val filtered = words
      .map { it.copy(text = it.text.trim()) }
      .filter { it.text.matches(englishRegex) }
      .filter { it.text.length >= 3 }
      .filter { it.text.lowercase() !in stopWords }

    if (filtered.isEmpty()) {
      return ExtractionResult(primary = null, candidates = emptyList(), ambiguous = false)
    }

    val maxArea = filtered.maxOf { it.area }.toFloat().coerceAtLeast(1f)
    val scores = filtered.map {
      val areaScore = it.area / maxArea
      val centerScore = centerProximityScore(it.centerX, it.centerY, imageWidth, imageHeight)
      val vocabScore = vocabLikelihood(it.text)
      val confidenceScore = (it.confidence ?: 0.6f).coerceIn(0f, 1f)
      val total = 0.5f * areaScore + 0.2f * centerScore + 0.2f * vocabScore + 0.1f * confidenceScore
      WordScore(it.text, total)
    }.sortedByDescending { it.score }

    val top = scores.first()
    val second = scores.getOrNull(1)
    val ambiguous = second != null && (top.score - second.score) < 0.08f
    val candidates = if (ambiguous) listOf(top.word, second!!.word) else listOf(top.word)
    return ExtractionResult(primary = top.word, candidates = candidates, ambiguous = ambiguous)
  }

  private fun centerProximityScore(x: Float, y: Float, width: Int, height: Int): Float {
    val cx = width / 2f
    val cy = height / 2f
    val dx = abs(x - cx) / cx.coerceAtLeast(1f)
    val dy = abs(y - cy) / cy.coerceAtLeast(1f)
    return (1f - ((dx + dy) / 2f)).coerceIn(0f, 1f)
  }

  private fun vocabLikelihood(word: String): Float {
    val len = word.length
    return when {
      len in 4..10 -> 1f
      len in 3..12 -> 0.8f
      else -> 0.6f
    }
  }
}
