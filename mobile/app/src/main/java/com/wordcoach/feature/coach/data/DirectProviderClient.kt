package com.wordcoach.feature.coach.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

data class DirectProviderConfig(
  val baseUrl: String,
  val model: String,
  val apiKey: String,
  val systemPrompt: String
)

interface DirectProviderClient {
  suspend fun coach(word: String, config: DirectProviderConfig): CoachResponse
}

internal fun buildOpenAiChatCompletionsUrl(baseUrl: String): String {
  val normalized = baseUrl.trim().trimEnd('/')
  return if (normalized.endsWith("/v1", ignoreCase = true)) {
    "$normalized/chat/completions"
  } else {
    "$normalized/v1/chat/completions"
  }
}

internal fun buildDirectProviderMoshi(): Moshi {
  return Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()
}

class OpenAiCompatibleDirectProviderClient(
  private val httpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(10, TimeUnit.SECONDS)
    .readTimeout(20, TimeUnit.SECONDS)
    .writeTimeout(20, TimeUnit.SECONDS)
    .build(),
  private val moshi: Moshi = buildDirectProviderMoshi()
) : DirectProviderClient {

  override suspend fun coach(word: String, config: DirectProviderConfig): CoachResponse {
    val body = ChatCompletionRequest(
      model = config.model,
      temperature = 0.4,
      messages = listOf(
        ChatMessage(role = "system", content = config.systemPrompt),
        ChatMessage(role = "user", content = buildUserPrompt(word))
      )
    )
    val adapter = moshi.adapter(ChatCompletionRequest::class.java)
    val payload = adapter.toJson(body)
    val request = Request.Builder()
      .url(buildOpenAiChatCompletionsUrl(config.baseUrl))
      .addHeader("Authorization", "Bearer ${config.apiKey}")
      .addHeader("Content-Type", "application/json")
      .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
      .build()

    httpClient.newCall(request).execute().use { response ->
      val raw = response.body?.string().orEmpty()
      if (!response.isSuccessful) {
        throw IllegalStateException("provider_error_${response.code}: ${raw.take(300)}")
      }
      val responseAdapter = moshi.adapter(ChatCompletionResponse::class.java)
      val parsed = responseAdapter.fromJson(raw)
      val content = parsed?.choices
        ?.firstOrNull()
        ?.message
        ?.content
        ?.trim()
      if (content.isNullOrBlank()) {
        throw IllegalStateException("provider_invalid_response")
      }
      return CoachResponse(
        word = word,
        explanation = content,
        source = "direct-provider"
      )
    }
  }

  private fun buildUserPrompt(word: String): String {
    return listOf(
      "识别图片中的最大的、用户正在记忆的单词（不要被其他信息误导）。",
      "目标词候选：$word",
      "若候选不准，请先纠正为最合理的英文单词，再进行讲解。"
    ).joinToString("\n")
  }
}

data class ChatCompletionRequest(
  val model: String,
  val temperature: Double,
  val messages: List<ChatMessage>
)

data class ChatMessage(
  val role: String,
  val content: String
)

data class ChatCompletionResponse(
  val choices: List<Choice>?
) {
  data class Choice(
    val message: ChoiceMessage?
  )

  data class ChoiceMessage(
    val content: String?
  )
}
