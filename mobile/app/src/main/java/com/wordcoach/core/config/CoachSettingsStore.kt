package com.wordcoach.core.config

import android.content.Context
import com.wordcoach.feature.coach.data.CoachRequestOverrides

data class CoachApiSettings(
  val providerBaseUrl: String = "",
  val providerModel: String = "",
  val providerApiKey: String = "",
  val systemPrompt: String = CoachPromptDefaults.defaultSystemPrompt()
)

object CoachPromptDefaults {
  fun defaultSystemPrompt(): String {
    return """
      你是一个“英语词源记忆教练”，请只用中文回答，并按下面结构输出，避免空话和模板腔：
      1) 一句话核心：先用1句说清这个词最核心的意义。
      2) 起源故事：讲清词源、词根词缀或历史演变，解释“为什么会有这个意思”。
      3) 记忆技巧：
         - 词源核心法：基于词源给出可复述的记忆点；
         - 形象联想法：给出一个具体、可视化的画面。
      4) 生活场景：分别给出“字面/具体场景”和“抽象/引申场景”。
      5) 高频例句：至少2句英文例句，每句附自然中文翻译。
      6) 复习提示：给出24小时内的复习节奏（短、可执行）。
      输出要求：
      - 结合目标词本身，不要泛化；
      - 内容具体、可记忆；
      - 总长度控制在300~600字；
      - 不要输出与结构无关的前后缀说明。
    """.trimIndent()
  }
}

class CoachSettingsStore(context: Context) {
  private val appContext = context.applicationContext
  private val prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

  fun load(): CoachApiSettings {
    val defaultPrompt = CoachPromptDefaults.defaultSystemPrompt()
    return CoachApiSettings(
      providerBaseUrl = prefs.getString(KEY_PROVIDER_BASE_URL, "") ?: "",
      providerModel = prefs.getString(KEY_PROVIDER_MODEL, "") ?: "",
      providerApiKey = prefs.getString(KEY_PROVIDER_API_KEY, "") ?: "",
      systemPrompt = prefs.getString(KEY_SYSTEM_PROMPT, defaultPrompt) ?: defaultPrompt
    )
  }

  fun save(settings: CoachApiSettings) {
    prefs.edit()
      .putString(KEY_PROVIDER_BASE_URL, settings.providerBaseUrl.trim())
      .putString(KEY_PROVIDER_MODEL, settings.providerModel.trim())
      .putString(KEY_PROVIDER_API_KEY, settings.providerApiKey.trim())
      .putString(KEY_SYSTEM_PROMPT, settings.systemPrompt.trim())
      .apply()
  }

  fun buildRequestOverrides(): CoachRequestOverrides {
    val settings = load()
    return CoachRequestOverrides(
      providerBaseUrl = settings.providerBaseUrl.trim().takeIf { it.isNotBlank() },
      providerModel = settings.providerModel.trim().takeIf { it.isNotBlank() },
      providerApiKey = settings.providerApiKey.trim().takeIf { it.isNotBlank() },
      systemPrompt = settings.systemPrompt.trim().takeIf { it.isNotBlank() }
    )
  }

  companion object {
    private const val PREF_NAME = "word_coach_settings"
    private const val KEY_PROVIDER_BASE_URL = "provider_base_url"
    private const val KEY_PROVIDER_MODEL = "provider_model"
    private const val KEY_PROVIDER_API_KEY = "provider_api_key"
    private const val KEY_SYSTEM_PROMPT = "system_prompt"
  }
}
