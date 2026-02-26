package com.wordcoach.ocr

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.wordcoach.feature.ocr.OcrWord
import com.wordcoach.feature.ocr.WordExtractor
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OcrInstrumentationTest {
  @Test
  fun extractor_works_on_device_runtime() {
    val extractor = WordExtractor()
    val result = extractor.extract(
      words = listOf(
        OcrWord("abandon", 200, 200, 600, 460, 0.95f)
      ),
      imageWidth = 1080,
      imageHeight = 1920
    )
    assertEquals("abandon", result.primary)
  }
}
