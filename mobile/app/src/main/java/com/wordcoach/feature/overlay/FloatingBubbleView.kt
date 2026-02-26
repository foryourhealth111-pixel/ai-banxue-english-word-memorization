package com.wordcoach.feature.overlay

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import kotlin.math.min

class FloatingBubbleView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : View(context, attrs) {

  enum class BubbleStatus {
    IDLE,
    CAPTURING,
    OCR,
    REQUEST,
    CACHE_HIT,
    SUCCESS,
    ERROR
  }

  private val bubbleSizePx = dpToPxInt(70f)
  private val ringStrokePx = dpToPxF(4f)
  private val ringRect = RectF()

  private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG)
  private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.argb(88, 255, 255, 255)
  }
  private val innerStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = dpToPxF(1.2f)
    color = Color.argb(42, 255, 250, 245)
  }
  private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    strokeWidth = ringStrokePx
    strokeCap = Paint.Cap.ROUND
    color = Color.parseColor("#BFA26D")
  }
  private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    color = Color.WHITE
    textSize = spToPx(20f)
    textAlign = Paint.Align.CENTER
    typeface = Typeface.create("Times New Roman", Typeface.BOLD)
  }

  private var status: BubbleStatus = BubbleStatus.IDLE
  private var ringRotation = 0f
  private var ringSweep = 360f

  private var spinAnimator: ValueAnimator? = null
  private var breathAnimator: ValueAnimator? = null

  private val backToIdleRunnable = Runnable { setStatus(BubbleStatus.IDLE) }

  init {
    isClickable = true
    isFocusable = true
    setLayerType(LAYER_TYPE_HARDWARE, null)
    startBreathing()
  }

  fun setStatus(newStatus: BubbleStatus) {
    if (status == newStatus) return
    status = newStatus
    removeCallbacks(backToIdleRunnable)

    when (newStatus) {
      BubbleStatus.IDLE -> {
        ringPaint.color = Color.parseColor("#BFA26D")
        ringSweep = 360f
        stopSpin()
        startBreathing()
      }
      BubbleStatus.CAPTURING -> startWorkingStyle(Color.parseColor("#4C8DDB"))
      BubbleStatus.OCR -> startWorkingStyle(Color.parseColor("#3E9E93"))
      BubbleStatus.REQUEST -> startWorkingStyle(Color.parseColor("#C9873A"))
      BubbleStatus.CACHE_HIT -> {
        ringPaint.color = Color.parseColor("#4B9D63")
        ringSweep = 360f
        stopSpin()
        pulseOnce()
        postDelayed(backToIdleRunnable, 900L)
      }
      BubbleStatus.SUCCESS -> {
        ringPaint.color = Color.parseColor("#5AA66A")
        ringSweep = 360f
        stopSpin()
        pulseOnce()
        postDelayed(backToIdleRunnable, 900L)
      }
      BubbleStatus.ERROR -> {
        ringPaint.color = Color.parseColor("#D25050")
        ringSweep = 360f
        stopSpin()
        shakeOnce()
        postDelayed(backToIdleRunnable, 1200L)
      }
    }
    invalidate()
  }

  override fun onDetachedFromWindow() {
    super.onDetachedFromWindow()
    removeCallbacks(backToIdleRunnable)
    stopSpin()
    stopBreathing()
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    setMeasuredDimension(bubbleSizePx, bubbleSizePx)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val cx = width / 2f
    val cy = height / 2f
    val radius = min(cx, cy) - dpToPxF(1.5f)
    val coreRadius = radius - ringStrokePx - dpToPxF(2f)

    corePaint.shader = RadialGradient(
      cx - coreRadius * 0.28f,
      cy - coreRadius * 0.35f,
      coreRadius * 1.18f,
      intArrayOf(
        Color.parseColor("#D3A77A"),
        Color.parseColor("#8A5A35"),
        Color.parseColor("#5B3924")
      ),
      floatArrayOf(0f, 0.6f, 1f),
      Shader.TileMode.CLAMP
    )

    canvas.drawCircle(cx, cy, coreRadius, corePaint)
    canvas.drawCircle(cx - coreRadius * 0.35f, cy - coreRadius * 0.38f, coreRadius * 0.28f, highlightPaint)
    canvas.drawCircle(cx, cy, coreRadius - dpToPxF(0.4f), innerStrokePaint)

    ringRect.set(
      cx - radius + ringStrokePx * 0.65f,
      cy - radius + ringStrokePx * 0.65f,
      cx + radius - ringStrokePx * 0.65f,
      cy + radius - ringStrokePx * 0.65f
    )
    if (ringSweep < 360f) {
      canvas.save()
      canvas.rotate(ringRotation, cx, cy)
      canvas.drawArc(ringRect, -90f, ringSweep, false, ringPaint)
      canvas.restore()
    } else {
      canvas.drawArc(ringRect, -90f, 360f, false, ringPaint)
    }

    val baseline = cy - (labelPaint.descent() + labelPaint.ascent()) / 2f
    canvas.drawText("WC", cx, baseline, labelPaint)
  }

  private fun startWorkingStyle(color: Int) {
    ringPaint.color = color
    ringSweep = 300f
    stopBreathing()
    startSpin()
  }

  private fun startSpin() {
    if (spinAnimator?.isRunning == true) return
    spinAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
      duration = 900L
      repeatCount = ValueAnimator.INFINITE
      interpolator = LinearInterpolator()
      addUpdateListener {
        ringRotation = it.animatedValue as Float
        invalidate()
      }
      start()
    }
  }

  private fun stopSpin() {
    spinAnimator?.cancel()
    spinAnimator = null
    ringRotation = 0f
  }

  private fun startBreathing() {
    if (breathAnimator?.isRunning == true) return
    breathAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
      duration = 1600L
      repeatCount = ValueAnimator.INFINITE
      repeatMode = ValueAnimator.REVERSE
      interpolator = AccelerateDecelerateInterpolator()
      addUpdateListener {
        val value = it.animatedValue as Float
        val scale = 1f + value * 0.035f
        scaleX = scale
        scaleY = scale
      }
      start()
    }
  }

  private fun stopBreathing() {
    breathAnimator?.cancel()
    breathAnimator = null
    scaleX = 1f
    scaleY = 1f
  }

  private fun pulseOnce() {
    ObjectAnimator.ofFloat(this, View.SCALE_X, 1f, 1.08f, 1f).apply {
      duration = 320L
      interpolator = AccelerateDecelerateInterpolator()
      start()
    }
    ObjectAnimator.ofFloat(this, View.SCALE_Y, 1f, 1.08f, 1f).apply {
      duration = 320L
      interpolator = AccelerateDecelerateInterpolator()
      start()
    }
  }

  private fun shakeOnce() {
    ObjectAnimator.ofFloat(this, View.TRANSLATION_X, 0f, -8f, 8f, -5f, 5f, 0f).apply {
      duration = 420L
      interpolator = AccelerateDecelerateInterpolator()
      start()
    }
  }

  private fun dpToPxF(dp: Float): Float {
    return dp * resources.displayMetrics.density
  }

  private fun dpToPxInt(dp: Float): Int {
    return (dp * resources.displayMetrics.density).toInt()
  }

  private fun spToPx(sp: Float): Float {
    return sp * resources.displayMetrics.scaledDensity
  }
}
