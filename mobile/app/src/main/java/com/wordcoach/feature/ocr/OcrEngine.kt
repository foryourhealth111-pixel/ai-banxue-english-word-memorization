package com.wordcoach.feature.ocr

import android.graphics.Bitmap
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

data class OcrWord(
  val text: String,
  val left: Int,
  val top: Int,
  val right: Int,
  val bottom: Int,
  val confidence: Float?
) {
  val width: Int get() = right - left
  val height: Int get() = bottom - top
  val area: Int get() = width * height
  val centerX: Float get() = left + width / 2f
  val centerY: Float get() = top + height / 2f
}

open class OcrEngine {
  private val recognizer by lazy { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }

  open fun recognize(bitmap: Bitmap): List<OcrWord> {
    val image = InputImage.fromBitmap(bitmap, 0)
    val result = Tasks.await(recognizer.process(image))
    val words = mutableListOf<OcrWord>()
    for (block in result.textBlocks) {
      for (line in block.lines) {
        for (element in line.elements) {
          val box = element.boundingBox ?: continue
          words.add(
            OcrWord(
              text = element.text,
              left = box.left,
              top = box.top,
              right = box.right,
              bottom = box.bottom,
              confidence = element.confidence
            )
          )
        }
      }
    }
    return words
  }
}
