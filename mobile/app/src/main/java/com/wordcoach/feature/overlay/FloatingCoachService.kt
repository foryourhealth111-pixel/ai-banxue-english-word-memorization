package com.wordcoach.feature.overlay

import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.provider.Settings
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.style.MetricAffectingSpan
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.facebook.shimmer.ShimmerFrameLayout
import com.wordcoach.R
import com.wordcoach.core.config.CoachSettingsStore
import com.wordcoach.core.logging.EventLogger
import com.wordcoach.core.network.NetworkModule
import com.wordcoach.core.storage.CoachDatabase
import com.wordcoach.feature.capture.CaptureCoordinator
import com.wordcoach.feature.coach.data.CoachRepository
import com.wordcoach.feature.coach.domain.CoachStage
import com.wordcoach.feature.coach.domain.RunCoachResult
import com.wordcoach.feature.coach.domain.RunCoachUseCase
import com.wordcoach.feature.ocr.OcrEngine
import com.wordcoach.feature.ocr.WordExtractor
import io.noties.markwon.Markwon
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FloatingCoachService : Service() {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
  private val logger = EventLogger()

  private lateinit var windowManager: WindowManager
  private lateinit var bubbleView: FloatingBubbleView
  private lateinit var cardView: LinearLayout
  private lateinit var candidateRow: LinearLayout
  private lateinit var copyButton: Button
  private lateinit var closeButton: Button
  private lateinit var exitButton: Button
  private lateinit var candidateBtn1: Button
  private lateinit var candidateBtn2: Button
  private lateinit var loadingShimmer: ShimmerFrameLayout
  private lateinit var loadingContainer: LinearLayout
  private lateinit var loadingTextView: TextView
  private lateinit var resultScrollView: ScrollView
  private lateinit var resultTextView: TextView
  private var cardVisible = false
  private var loadingVisible = false
  private var lastCopiedText: String = ""
  private var captureInProgress = false
  private var overlayHiddenForCapture = false

  private lateinit var runCoachUseCase: RunCoachUseCase
  private lateinit var captureCoordinator: CaptureCoordinator
  private lateinit var bubbleParams: WindowManager.LayoutParams
  private lateinit var cardParams: WindowManager.LayoutParams
  private val markwon by lazy { Markwon.create(this) }
  private val chineseTypeface by lazy { Typeface.create("Source Han Serif SC", Typeface.NORMAL) }
  private val englishTypeface by lazy { Typeface.create("Times New Roman", Typeface.NORMAL) }

  override fun onBind(intent: Intent?): IBinder? = null

  override fun onCreate() {
    super.onCreate()
    windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
    initUseCase()
    initViews()
    logger.info("overlay_service_created")
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    OverlayNotification.ensureChannel(this)
    startForeground(OverlayNotification.NOTIFICATION_ID, OverlayNotification.build(this))

    if (!Settings.canDrawOverlays(this)) {
      logger.warn("overlay_permission_missing")
      stopSelf()
      return START_NOT_STICKY
    }

    logger.info("overlay_service_started")
    attachBubbleIfNeeded()
    return START_STICKY
  }

  override fun onDestroy() {
    super.onDestroy()
    logger.warn("overlay_service_destroyed")
    if (this::captureCoordinator.isInitialized) {
      captureCoordinator.release(clearPermission = true)
    }
    removeViewSafely(bubbleView)
    removeViewSafely(cardView)
    scope.cancel()
  }

  private fun initUseCase() {
    captureCoordinator = CaptureCoordinator(this)
    val ocrEngine = OcrEngine()
    val extractor = WordExtractor()
    val settingsStore = CoachSettingsStore(this)
    val repository = CoachRepository(
      apiService = NetworkModule.createApiService(),
      requestOverridesProvider = { settingsStore.buildRequestOverrides() }
    )
    val cacheDao = CoachDatabase().coachCacheDao
    runCoachUseCase = RunCoachUseCase(
      captureCoordinator = captureCoordinator,
      ocrEngine = ocrEngine,
      wordExtractor = extractor,
      coachRepository = repository,
      cacheDao = cacheDao
    )
  }

  private fun initViews() {
    val bubble = FloatingBubbleView(this)
    setupBubbleTouchBehavior(bubble)
    bubble.setStatus(FloatingBubbleView.BubbleStatus.IDLE)
    bubbleView = bubble

    resultTextView = TextView(this).apply {
      textSize = 16f
      setLineSpacing(dpToPx(6).toFloat(), 1f)
      typeface = chineseTypeface
      setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))
      setTextColor(Color.parseColor("#2B2B2B"))
      text = ""
    }
    setResultText(getString(R.string.overlay_idle_hint))
    resultScrollView = ScrollView(this).apply {
      isVerticalScrollBarEnabled = true
      isScrollbarFadingEnabled = false
      overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
      setBackgroundColor(Color.parseColor("#FFFDF8F2"))
      layoutParams = LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        0,
        1f
      )
      addView(
        resultTextView,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        )
      )
    }
    loadingTextView = TextView(this).apply {
      textSize = 16f
      typeface = chineseTypeface
      setTextColor(Color.parseColor("#5B4636"))
      text = getString(R.string.overlay_loading_preparing)
    }
    loadingContainer = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      setPadding(dpToPx(12), dpToPx(16), dpToPx(12), dpToPx(16))
      addView(ProgressBar(this@FloatingCoachService).apply {
        isIndeterminate = true
      })
      addView(
        loadingTextView,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.WRAP_CONTENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
          marginStart = dpToPx(12)
        }
      )
    }
    loadingShimmer = ShimmerFrameLayout(this).apply {
      visibility = View.GONE
      setShimmer(
        com.facebook.shimmer.Shimmer.AlphaHighlightBuilder()
          .setDuration(900L)
          .setBaseAlpha(0.75f)
          .setHighlightAlpha(1f)
          .setDirection(com.facebook.shimmer.Shimmer.Direction.LEFT_TO_RIGHT)
          .build()
      )
      addView(
        loadingContainer,
        LinearLayout.LayoutParams(
          LinearLayout.LayoutParams.MATCH_PARENT,
          LinearLayout.LayoutParams.WRAP_CONTENT
        )
      )
    }

    copyButton = Button(this).apply {
      text = getString(R.string.overlay_action_copy)
      setOnClickListener { copyLatestText() }
    }
    closeButton = Button(this).apply {
      text = getString(R.string.overlay_action_close)
      setOnClickListener { hideCard() }
    }
    exitButton = Button(this).apply {
      text = getString(R.string.overlay_action_exit)
      setOnClickListener { stopAssistant() }
    }
    attachPressFeedback(copyButton)
    attachPressFeedback(closeButton)
    attachPressFeedback(exitButton)

    val actionRow = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      setPadding(dpToPx(4), dpToPx(8), dpToPx(4), dpToPx(4))
      addView(copyButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
      addView(closeButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
      addView(exitButton, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
    }

    candidateBtn1 = Button(this).apply {
      text = getString(R.string.overlay_candidate_placeholder)
      visibility = View.GONE
      setOnClickListener { chooseCandidate(text.toString()) }
    }
    candidateBtn2 = Button(this).apply {
      text = getString(R.string.overlay_candidate_placeholder)
      visibility = View.GONE
      setOnClickListener { chooseCandidate(text.toString()) }
    }
    candidateRow = LinearLayout(this).apply {
      orientation = LinearLayout.HORIZONTAL
      visibility = View.GONE
      addView(candidateBtn1)
      addView(candidateBtn2)
    }

    cardView = LinearLayout(this).apply {
      orientation = LinearLayout.VERTICAL
      background = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = dpToPx(18).toFloat()
        setColor(Color.parseColor("#FFFFFCF6"))
        setStroke(dpToPx(1), Color.parseColor("#1A8B5E3C"))
      }
      setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8))
      elevation = dpToPx(14).toFloat()
      addView(loadingShimmer)
      addView(resultScrollView)
      addView(candidateRow)
      addView(actionRow)
      visibility = View.GONE
    }
  }

  private fun attachBubbleIfNeeded() {
    if (!this::bubbleParams.isInitialized) {
      bubbleParams = bubbleLayoutParams()
    }
    if (!this::cardParams.isInitialized) {
      cardParams = cardLayoutParams()
    }
    if (bubbleView.parent == null) {
      windowManager.addView(bubbleView, bubbleParams)
    }
    if (cardView.parent == null) {
      windowManager.addView(cardView, cardParams)
    }
  }

  private fun runCoach() {
    if (captureInProgress) {
      return
    }
    captureInProgress = true
    bubbleView.setStatus(FloatingBubbleView.BubbleStatus.CAPTURING)
    hideCandidateButtons()
    showLoadingState(getString(R.string.overlay_loading_capture))
    scope.launch {
      try {
        setOverlayHiddenForCapture(hidden = true)
        delay(120)
        val captureResult = withContext(Dispatchers.Default) { captureCoordinator.capture() }
        setOverlayHiddenForCapture(hidden = false)
        val bitmap = captureResult.getOrElse {
          bubbleView.setStatus(FloatingBubbleView.BubbleStatus.ERROR)
          val text = getString(
            R.string.overlay_error_capture_format,
            it.message ?: "capture_failed"
          )
          lastCopiedText = text
          showResultCard(text)
          logger.error("coach_capture_failed", throwable = it)
          return@launch
        }

        showLoadingState(getString(R.string.overlay_loading_ocr))
        when (
          val result = runCoachUseCase.executeFromCapturedBitmap(bitmap) { stage ->
            scope.launch {
              showLoadingState(statusTextByStage(stage))
              bubbleView.setStatus(statusBubbleByStage(stage))
            }
          }
        ) {
          is RunCoachResult.Success -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.SUCCESS)
            hideCandidateButtons()
            val cachedTag = if (result.fromCache) getString(R.string.overlay_cache_tag) else ""
            val markdownText = getString(
              R.string.overlay_markdown_result_template,
              result.word,
              cachedTag,
              normalizeMarkdownForDisplay(result.explanation)
            )
            val plainText = getString(
              R.string.overlay_plain_result_template,
              result.word,
              cachedTag,
              sanitizeMarkdown(result.explanation)
            )
            lastCopiedText = plainText
            showResultCard(markdownText, renderMarkdown = true)
            logger.info("coach_success", mapOf("word" to result.word, "fromCache" to result.fromCache))
          }
          is RunCoachResult.Ambiguous -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.SUCCESS)
            showCandidateButtons(result.candidates)
            val text = getString(
              R.string.overlay_candidate_ambiguous_format,
              result.candidates.joinToString(" / ")
            )
            lastCopiedText = text
            showResultCard(text)
            logger.warn("coach_ambiguous", mapOf("candidates" to result.candidates.joinToString(",")))
          }
          is RunCoachResult.Failure -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.ERROR)
            hideCandidateButtons()
            val text = getString(
              R.string.overlay_error_generic_format,
              result.code.name,
              sanitizeMarkdown(result.message)
            )
            lastCopiedText = text
            showResultCard(text, renderMarkdown = false)
            logger.error("coach_failure", attrs = mapOf("code" to result.code.name))
          }
        }
      } catch (t: Throwable) {
        bubbleView.setStatus(FloatingBubbleView.BubbleStatus.ERROR)
        hideCandidateButtons()
        val text = getString(
          R.string.overlay_error_unexpected_format,
          t.message ?: "unknown_error"
        )
        lastCopiedText = text
        showResultCard(text, renderMarkdown = false)
        logger.error("coach_unexpected_exception", throwable = t)
      } finally {
        setOverlayHiddenForCapture(hidden = false)
        hideLoadingState()
        captureInProgress = false
      }
    }
  }

  private fun chooseCandidate(word: String) {
    if (word == getString(R.string.overlay_candidate_placeholder) || word.isBlank()) return
    bubbleView.setStatus(FloatingBubbleView.BubbleStatus.REQUEST)
    showCard()
    showLoadingState(getString(R.string.overlay_loading_request))
    scope.launch {
      try {
        when (
          val result = runCoachUseCase.executeWithWord(word)
        ) {
          is RunCoachResult.Success -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.SUCCESS)
            hideCandidateButtons()
            val cachedTag = if (result.fromCache) getString(R.string.overlay_cache_tag) else ""
            val markdownText = getString(
              R.string.overlay_markdown_result_template,
              result.word,
              cachedTag,
              normalizeMarkdownForDisplay(result.explanation)
            )
            val plainText = getString(
              R.string.overlay_plain_result_template,
              result.word,
              cachedTag,
              sanitizeMarkdown(result.explanation)
            )
            lastCopiedText = plainText
            showResultCard(markdownText, renderMarkdown = true)
          }
          is RunCoachResult.Failure -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.ERROR)
            hideCandidateButtons()
            val text = getString(
              R.string.overlay_error_generic_format,
              result.code.name,
              sanitizeMarkdown(result.message)
            )
            lastCopiedText = text
            showResultCard(text, renderMarkdown = false)
          }
          is RunCoachResult.Ambiguous -> {
            bubbleView.setStatus(FloatingBubbleView.BubbleStatus.IDLE)
            showCandidateButtons(result.candidates)
            val text = getString(R.string.overlay_ambiguous_continue)
            lastCopiedText = text
            showResultCard(text, renderMarkdown = false)
          }
        }
      } catch (t: Throwable) {
        bubbleView.setStatus(FloatingBubbleView.BubbleStatus.ERROR)
        hideCandidateButtons()
        val text = getString(
          R.string.overlay_error_unexpected_format,
          t.message ?: "unknown_error"
        )
        lastCopiedText = text
        showResultCard(text, renderMarkdown = false)
        logger.error("coach_choose_candidate_exception", throwable = t)
      } finally {
        hideLoadingState()
      }
    }
  }

  private fun copyLatestText() {
    if (lastCopiedText.isBlank()) return
    val manager = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    manager.setPrimaryClip(ClipData.newPlainText(getString(R.string.clipboard_label), lastCopiedText))
    copyButton.animate().scaleX(1.06f).scaleY(1.06f).setDuration(100L).withEndAction {
      copyButton.animate().scaleX(1f).scaleY(1f).setDuration(140L).start()
    }.start()
    val oldText = copyButton.text
    copyButton.text = getString(R.string.overlay_copy_done_button)
    copyButton.postDelayed({ copyButton.text = oldText }, 1200L)
    Toast.makeText(this, getString(R.string.copy_success), Toast.LENGTH_SHORT).show()
  }

  private fun toggleCard() {
    if (cardVisible) hideCard() else showCard()
  }

  private fun showCard() {
    cardVisible = true
    if (!overlayHiddenForCapture) {
      cardView.visibility = View.VISIBLE
    }
  }

  private fun hideCard() {
    cardVisible = false
    cardView.visibility = View.GONE
  }

  private fun showResultCard(text: String, renderMarkdown: Boolean = false) {
    hideLoadingState()
    if (renderMarkdown) {
      setResultMarkdown(text)
    } else {
      setResultText(text)
    }
    scrollResultToTop()
    cardVisible = true
    if (!overlayHiddenForCapture) {
      cardView.visibility = View.VISIBLE
      resultScrollView.alpha = 0f
      resultScrollView.animate().alpha(1f).setDuration(220L).start()
    }
  }

  private fun showLoadingState(message: String) {
    loadingVisible = true
    cardVisible = true
    loadingTextView.text = message
    loadingShimmer.visibility = View.VISIBLE
    loadingShimmer.startShimmer()
    resultScrollView.visibility = View.GONE
    hideCandidateButtons()
    if (!overlayHiddenForCapture) {
      cardView.visibility = View.VISIBLE
    }
  }

  private fun hideLoadingState() {
    loadingVisible = false
    if (this::loadingShimmer.isInitialized) {
      loadingShimmer.stopShimmer()
      loadingShimmer.visibility = View.GONE
    }
    if (this::resultScrollView.isInitialized) {
      resultScrollView.visibility = View.VISIBLE
    }
  }

  private fun scrollResultToTop() {
    if (this::resultScrollView.isInitialized) {
      resultScrollView.post { resultScrollView.scrollTo(0, 0) }
    }
  }

  private fun stopAssistant() {
    cardVisible = false
    hideCard()
    stopForeground(STOP_FOREGROUND_REMOVE)
    stopSelf()
  }

  private fun setOverlayHiddenForCapture(hidden: Boolean) {
    overlayHiddenForCapture = hidden
    bubbleView.visibility = if (hidden) View.GONE else View.VISIBLE
    cardView.visibility = if (!hidden && cardVisible) View.VISIBLE else View.GONE
  }

  private fun showCandidateButtons(candidates: List<String>) {
    candidateRow.visibility = View.VISIBLE
    val placeholder = getString(R.string.overlay_candidate_placeholder)
    candidateBtn1.text = candidates.getOrElse(0) { placeholder }
    candidateBtn2.text = candidates.getOrElse(1) { placeholder }
    candidateBtn1.visibility = if (candidates.size >= 1) View.VISIBLE else View.GONE
    candidateBtn2.visibility = if (candidates.size >= 2) View.VISIBLE else View.GONE
  }

  private fun hideCandidateButtons() {
    candidateRow.visibility = View.GONE
    candidateBtn1.visibility = View.GONE
    candidateBtn2.visibility = View.GONE
  }

  private fun removeViewSafely(view: View) {
    if (view.parent != null) {
      windowManager.removeView(view)
    }
  }

  private fun setupBubbleTouchBehavior(target: FloatingBubbleView) {
    val touchSlop = ViewConfiguration.get(this).scaledTouchSlop
    var downRawX = 0f
    var downRawY = 0f
    var downX = 0
    var downY = 0
    var moved = false
    var downTime = 0L

    target.setOnTouchListener { _, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          downRawX = event.rawX
          downRawY = event.rawY
          downX = bubbleParams.x
          downY = bubbleParams.y
          moved = false
          downTime = event.downTime
          true
        }
        MotionEvent.ACTION_MOVE -> {
          val dx = (event.rawX - downRawX).toInt()
          val dy = (event.rawY - downRawY).toInt()
          if (!moved && (abs(dx) > touchSlop || abs(dy) > touchSlop)) {
            moved = true
          }
          if (moved) {
            moveBubbleTo(target, downX + dx, downY + dy)
          }
          true
        }
        MotionEvent.ACTION_UP -> {
          val pressDuration = event.eventTime - downTime
          if (!moved) {
            if (pressDuration >= ViewConfiguration.getLongPressTimeout()) {
              toggleCard()
            } else {
              runCoach()
            }
          }
          true
        }
        MotionEvent.ACTION_CANCEL -> true
        else -> false
      }
    }
  }

  private fun moveBubbleTo(target: FloatingBubbleView, rawX: Int, rawY: Int) {
    val metrics = resources.displayMetrics
    val bubbleWidth = target.width.takeIf { it > 0 } ?: dpToPx(70)
    val bubbleHeight = target.height.takeIf { it > 0 } ?: dpToPx(70)
    val clampedX = rawX.coerceIn(0, max(0, metrics.widthPixels - bubbleWidth))
    val clampedY = rawY.coerceIn(0, max(0, metrics.heightPixels - bubbleHeight))
    bubbleParams.x = clampedX
    bubbleParams.y = clampedY
    windowManager.updateViewLayout(target, bubbleParams)

    if (cardView.parent != null) {
      val cardWidth = cardParams.width.takeIf { it > 0 } ?: cardWidthPx()
      val cardHeight = cardParams.height.takeIf { it > 0 } ?: cardHeightPx()
      val cardX = min(clampedX, max(0, metrics.widthPixels - cardWidth))
      val cardY = min(clampedY + bubbleHeight + dpToPx(8), max(0, metrics.heightPixels - cardHeight))
      cardParams.x = cardX
      cardParams.y = cardY
      windowManager.updateViewLayout(cardView, cardParams)
    }
  }

  private fun bubbleLayoutParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      WindowManager.LayoutParams.WRAP_CONTENT,
      WindowManager.LayoutParams.WRAP_CONTENT,
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = dpToPx(16)
      y = dpToPx(120)
    }
  }

  private fun cardLayoutParams(): WindowManager.LayoutParams {
    return WindowManager.LayoutParams(
      cardWidthPx(),
      cardHeightPx(),
      overlayType(),
      WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
      PixelFormat.TRANSLUCENT
    ).apply {
      gravity = Gravity.TOP or Gravity.START
      x = dpToPx(16)
      y = dpToPx(180)
    }
  }

  private fun cardWidthPx(): Int {
    val metrics = resources.displayMetrics
    val sidePadding = dpToPx(16) * 2
    val maxUsableWidth = max(dpToPx(280), metrics.widthPixels - sidePadding)
    return min(dpToPx(360), maxUsableWidth)
  }

  private fun cardHeightPx(): Int {
    val metrics = resources.displayMetrics
    val maxUsableHeight = max(dpToPx(280), metrics.heightPixels - dpToPx(120))
    return min(dpToPx(420), maxUsableHeight)
  }

  private fun statusTextByStage(stage: CoachStage): String {
    return when (stage) {
      CoachStage.OCR -> getString(R.string.overlay_loading_ocr)
      CoachStage.REQUEST_REMOTE -> getString(R.string.overlay_loading_request)
      CoachStage.CACHE_HIT -> getString(R.string.overlay_loading_cache_hit)
    }
  }

  private fun statusBubbleByStage(stage: CoachStage): FloatingBubbleView.BubbleStatus {
    return when (stage) {
      CoachStage.OCR -> FloatingBubbleView.BubbleStatus.OCR
      CoachStage.REQUEST_REMOTE -> FloatingBubbleView.BubbleStatus.REQUEST
      CoachStage.CACHE_HIT -> FloatingBubbleView.BubbleStatus.CACHE_HIT
    }
  }

  private fun setResultMarkdown(markdown: String) {
    runCatching {
      markwon.setMarkdown(resultTextView, markdown)
    }.onFailure {
      setResultText(sanitizeMarkdown(markdown))
    }
  }

  private fun setResultText(text: String) {
    resultTextView.text = applyBilingualTypeface(text)
  }

  private fun applyBilingualTypeface(text: String): SpannableString {
    val spannable = SpannableString(text)
    if (text.isEmpty()) return spannable
    var start = 0
    while (start < text.length) {
      val useEnglish = isAsciiSegment(text[start])
      var end = start + 1
      while (end < text.length && isAsciiSegment(text[end]) == useEnglish) {
        end++
      }
      spannable.setSpan(
        TypefaceSpanCompat(if (useEnglish) englishTypeface else chineseTypeface),
        start,
        end,
        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
      )
      start = end
    }
    return spannable
  }

  private fun isAsciiSegment(ch: Char): Boolean {
    return ch.code in 0x0000..0x007F
  }

  private fun sanitizeMarkdown(raw: String): String {
    var text = raw.replace("\r\n", "\n")
    text = text.replace(Regex("(?m)^\\s*```[a-zA-Z0-9_-]*\\s*$"), "")
    text = text.replace(Regex("`([^`]+)`"), "$1")
    text = text.replace(Regex("(?m)^\\s{0,3}#{1,6}\\s*"), "")
    text = text.replace(Regex("(?m)^\\s*>\\s?"), "")
    text = text.replace(Regex("\\*\\*([^*]+)\\*\\*"), "$1")
    text = text.replace(Regex("__([^_]+)__"), "$1")
    text = text.replace(Regex("~~([^~]+)~~"), "$1")
    text = text.replace(Regex("(?m)^\\s*[-*+]\\s+"), "• ")
    text = text.replace(Regex("(?m)^\\s*(\\d+)\\.\\s+"), "$1. ")
    text = text.replace(Regex("(?m)^\\s*[-=]{3,}\\s*$"), "")
    text = text.replace(Regex("\\n{3,}"), "\n\n")
    return text.trim()
  }

  private fun normalizeMarkdownForDisplay(raw: String): String {
    var text = raw.replace("\r\n", "\n")
    text = text.replace(Regex("(?m)^\\s*```\\s*$"), "")
    text = text.replace(Regex("\\n{3,}"), "\n\n")
    return text.trim()
  }

  private fun attachPressFeedback(button: Button) {
    button.setOnTouchListener { view, event ->
      when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
          view.animate().scaleX(0.97f).scaleY(0.97f).setDuration(80L).start()
        }
        MotionEvent.ACTION_CANCEL,
        MotionEvent.ACTION_UP -> {
          view.animate().scaleX(1f).scaleY(1f).setDuration(120L).start()
        }
      }
      false
    }
  }

  private fun dpToPx(dp: Int): Int {
    return (dp * resources.displayMetrics.density).toInt()
  }

  private fun overlayType(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
    } else {
      WindowManager.LayoutParams.TYPE_PHONE
    }
  }

  private class TypefaceSpanCompat(
    private val typeface: Typeface
  ) : MetricAffectingSpan() {
    override fun updateDrawState(tp: TextPaint) {
      tp.typeface = typeface
    }

    override fun updateMeasureState(tp: TextPaint) {
      tp.typeface = typeface
    }
  }
}
