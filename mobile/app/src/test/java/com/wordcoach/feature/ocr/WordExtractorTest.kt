package com.wordcoach.feature.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordExtractorTest {
  @Test
  fun `extract picks largest meaningful english word`() {
    val extractor = WordExtractor()
    val result = extractor.extract(
      words = listOf(
        OcrWord("menu", 0, 0, 60, 20, 0.9f),
        OcrWord("abandon", 300, 300, 620, 460, 0.95f),
        OcrWord("ok", 40, 40, 80, 80, 0.9f)
      ),
      imageWidth = 1080,
      imageHeight = 1920
    )

    assertEquals("abandon", result.primary)
    assertTrue(!result.ambiguous)
  }

  @Test
  fun `extract returns null for empty word list`() {
    val extractor = WordExtractor()
    val result = extractor.extract(emptyList(), imageWidth = 1080, imageHeight = 1920)
    assertEquals(null, result.primary)
  }
}
