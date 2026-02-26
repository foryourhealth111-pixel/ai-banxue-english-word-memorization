package com.wordcoach.feature.coach.domain

import android.graphics.Bitmap
import com.wordcoach.core.error.ErrorCode
import com.wordcoach.core.storage.CoachCacheDao
import com.wordcoach.feature.capture.CaptureCoordinator
import com.wordcoach.feature.coach.data.CoachRepository
import com.wordcoach.feature.coach.data.CoachRepositoryResult
import com.wordcoach.feature.ocr.OcrEngine
import com.wordcoach.feature.ocr.WordExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class RunCoachResult {
  data class Success(val word: String, val explanation: String, val fromCache: Boolean) : RunCoachResult()
  data class Ambiguous(val candidates: List<String>) : RunCoachResult()
  data class Failure(val code: ErrorCode, val message: String) : RunCoachResult()
}

enum class CoachStage {
  OCR,
  REQUEST_REMOTE,
  CACHE_HIT
}

class RunCoachUseCase(
  private val captureCoordinator: CaptureCoordinator,
  private val ocrEngine: OcrEngine,
  private val wordExtractor: WordExtractor,
  private val coachRepository: CoachRepository,
  private val cacheDao: CoachCacheDao
) {
  suspend fun execute(): RunCoachResult = withContext(Dispatchers.Default) {
    val bitmap = captureCoordinator.capture().getOrElse {
      return@withContext RunCoachResult.Failure(ErrorCode.CAPTURE_FAILED, it.message ?: "capture_failed")
    }
    return@withContext executeFromCapturedBitmapInternal(bitmap) {}
  }

  suspend fun executeFromCapturedBitmap(
    bitmap: Bitmap,
    onStage: (CoachStage) -> Unit = {}
  ): RunCoachResult = withContext(Dispatchers.Default) {
    return@withContext executeFromCapturedBitmapInternal(bitmap, onStage)
  }

  private suspend fun executeFromCapturedBitmapInternal(
    bitmap: Bitmap,
    onStage: (CoachStage) -> Unit
  ): RunCoachResult {
    onStage(CoachStage.OCR)
    val recognized = runCatching { ocrEngine.recognize(bitmap) }.getOrElse {
      return RunCoachResult.Failure(ErrorCode.OCR_EMPTY, it.message ?: "ocr_failed")
    }
    val extraction = wordExtractor.extract(recognized, bitmap.width, bitmap.height)
    if (extraction.primary == null) {
      return RunCoachResult.Failure(ErrorCode.OCR_EMPTY, "no_word_detected")
    }

    if (extraction.ambiguous) {
      return RunCoachResult.Ambiguous(extraction.candidates)
    }

    val word = extraction.primary.lowercase()
    return fetchByWordInternal(word, onStage)
  }

  suspend fun executeWithWord(word: String): RunCoachResult = withContext(Dispatchers.Default) {
    return@withContext fetchByWordInternal(word.lowercase()) {}
  }

  private suspend fun fetchByWordInternal(
    word: String,
    onStage: (CoachStage) -> Unit
  ): RunCoachResult {
    val cached = cacheDao.find(word)
    if (cached != null) {
      onStage(CoachStage.CACHE_HIT)
      return RunCoachResult.Success(word, cached.explanation, fromCache = true)
    }

    onStage(CoachStage.REQUEST_REMOTE)
    return when (val repoResult = coachRepository.fetchCoaching(word)) {
      is CoachRepositoryResult.Success -> {
        cacheDao.upsert(word, repoResult.response.explanation)
        RunCoachResult.Success(word, repoResult.response.explanation, fromCache = false)
      }
      is CoachRepositoryResult.Failure -> {
        RunCoachResult.Failure(ErrorCode.NETWORK_TIMEOUT, repoResult.message)
      }
    }
  }
}
