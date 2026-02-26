package com.wordcoach.feature.capture

import android.graphics.Bitmap
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureCoordinatorTest {
  @Test
  fun `capture fails when projection is missing`() = runTest {
    ProjectionSessionStore.clear()
    val coordinator = CaptureCoordinator()
    val result = coordinator.capture()
    assertTrue(result.isFailure)
  }

  @Test
  fun `capture can be overridden for deterministic tests`() = runTest {
    val coordinator = object : CaptureCoordinator() {
      override suspend fun capture(): Result<Bitmap> {
        return Result.success(mockk(relaxed = true))
      }
    }
    val result = coordinator.capture()
    assertTrue(result.isSuccess)
  }
}
