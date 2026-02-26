package com.wordcoach.feature.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

open class CaptureCoordinator(
  private val appContext: Context? = null
) {
  companion object {
    private val lock = Any()
    private var projection: MediaProjection? = null
    private var projectionCallback: MediaProjection.Callback? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
  }

  open suspend fun capture(): Result<Bitmap> = withContext(Dispatchers.Default) {
    runCatching {
      val context = appContext ?: throw IllegalStateException("context_missing")
      val reader = ensureCapturePipeline(context.applicationContext)
      var image: Image? = null
      try {
        // Wait briefly for the first frame to arrive.
        delay(120)
        image = reader.acquireLatestImage()
        if (image == null) {
          delay(120)
          image = reader.acquireLatestImage()
        }

        val capturedImage = image ?: throw IllegalStateException("projection_frame_unavailable")
        val width = capturedImage.width
        val height = capturedImage.height
        val plane = capturedImage.planes[0]
        val buffer = plane.buffer
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * width
        val paddedBitmap = Bitmap.createBitmap(
          width + rowPadding / pixelStride,
          height,
          Bitmap.Config.ARGB_8888
        )
        paddedBitmap.copyPixelsFromBuffer(buffer)
        return@runCatching Bitmap.createBitmap(paddedBitmap, 0, 0, width, height)
      } finally {
        image?.close()
      }
    }.onFailure {
      if (shouldInvalidateProjectionSession(it)) {
        release(clearPermission = true)
      }
    }
  }

  open fun release(clearPermission: Boolean = false) {
    synchronized(lock) {
      releaseLocked(clearPermission = clearPermission, stopProjection = true)
    }
  }

  private fun ensureCapturePipeline(context: Context): ImageReader {
    synchronized(lock) {
      val currentReader = imageReader
      if (projection != null && virtualDisplay != null && currentReader != null) {
        return currentReader
      }

      val session = ProjectionSessionStore.readSession()
        ?: throw IllegalStateException("projection_permission_missing")
      val manager = context.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
      val newProjection = manager.getMediaProjection(session.first, session.second)
        ?: throw IllegalStateException("projection_creation_failed")

      val callback = object : MediaProjection.Callback() {
        override fun onStop() {
          synchronized(lock) {
            releaseLocked(clearPermission = true, stopProjection = false)
          }
        }
      }
      newProjection.registerCallback(callback, Handler(Looper.getMainLooper()))

      val metrics = context.resources.displayMetrics
      val width = metrics.widthPixels
      val height = metrics.heightPixels
      val density = metrics.densityDpi
      val newImageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
      val newVirtualDisplay = newProjection.createVirtualDisplay(
        "word-coach-capture",
        width,
        height,
        density,
        DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
        newImageReader.surface,
        null,
        null
      )

      projection = newProjection
      projectionCallback = callback
      imageReader = newImageReader
      virtualDisplay = newVirtualDisplay
      return newImageReader
    }
  }

  private fun releaseLocked(clearPermission: Boolean, stopProjection: Boolean) {
    runCatching { virtualDisplay?.release() }
    runCatching { imageReader?.close() }
    val currentProjection = projection
    val callback = projectionCallback
    runCatching {
      if (currentProjection != null && callback != null) {
        currentProjection.unregisterCallback(callback)
      }
    }
    if (stopProjection) {
      runCatching { currentProjection?.stop() }
    }
    virtualDisplay = null
    imageReader = null
    projectionCallback = null
    projection = null
    if (clearPermission) {
      ProjectionSessionStore.clear()
    }
  }

  private fun shouldInvalidateProjectionSession(throwable: Throwable): Boolean {
    if (throwable is SecurityException) return true
    val msg = throwable.message?.lowercase() ?: return false
    return msg.contains("don't re-use the resultdata") ||
      msg.contains("token that has timed out") ||
      msg.contains("createvirtualdisplay") ||
      msg.contains("media projection") ||
      msg.contains("projection")
  }
}
