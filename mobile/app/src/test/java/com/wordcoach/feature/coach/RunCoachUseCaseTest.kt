package com.wordcoach.feature.coach

import android.graphics.Bitmap
import com.wordcoach.core.storage.CoachCacheDao
import com.wordcoach.core.network.ApiService
import com.wordcoach.feature.capture.CaptureCoordinator
import com.wordcoach.feature.coach.data.CoachRepository
import com.wordcoach.feature.coach.data.CoachRequest
import com.wordcoach.feature.coach.data.CoachResponse
import com.wordcoach.feature.coach.domain.RunCoachResult
import com.wordcoach.feature.coach.domain.RunCoachUseCase
import com.wordcoach.feature.ocr.OcrEngine
import com.wordcoach.feature.ocr.OcrWord
import com.wordcoach.feature.ocr.WordExtractor
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class RunCoachUseCaseTest {
  @Test
  fun `returns success when capture and network pass`() = runTest {
    val capture = object : CaptureCoordinator() {
      override suspend fun capture(): Result<Bitmap> {
        return Result.success(mockk(relaxed = true))
      }
    }
    val ocr = object : OcrEngine() {
      override fun recognize(bitmap: Bitmap): List<OcrWord> {
        return listOf(OcrWord("abandon", 100, 100, 320, 220, 0.95f))
      }
    }
    val api = object : ApiService {
      override suspend fun coach(request: CoachRequest): CoachResponse {
        return CoachResponse(request.word, "讲解", "gemini-proxy")
      }
    }
    val useCase = RunCoachUseCase(
      captureCoordinator = capture,
      ocrEngine = ocr,
      wordExtractor = WordExtractor(),
      coachRepository = CoachRepository(api),
      cacheDao = CoachCacheDao()
    )
    val result = useCase.execute()
    assertTrue(result is RunCoachResult.Success)
  }
}
