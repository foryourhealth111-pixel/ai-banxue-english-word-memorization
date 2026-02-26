package com.wordcoach.feature.coach

import com.wordcoach.feature.coach.data.ChatCompletionRequest
import com.wordcoach.feature.coach.data.ChatMessage
import com.wordcoach.feature.coach.data.buildDirectProviderMoshi
import com.wordcoach.feature.coach.data.buildOpenAiChatCompletionsUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectProviderClientTest {
  @Test
  fun `buildOpenAiChatCompletionsUrl appends v1 for provider root url`() {
    val url = buildOpenAiChatCompletionsUrl("https://api.openai.com")
    assertEquals("https://api.openai.com/v1/chat/completions", url)
  }

  @Test
  fun `buildOpenAiChatCompletionsUrl does not duplicate v1 when already present`() {
    val url = buildOpenAiChatCompletionsUrl("https://api.openai.com/v1/")
    assertEquals("https://api.openai.com/v1/chat/completions", url)
  }

  @Test
  fun `buildDirectProviderMoshi serializes kotlin request model`() {
    val moshi = buildDirectProviderMoshi()
    val adapter = moshi.adapter(ChatCompletionRequest::class.java)
    val json = adapter.toJson(
      ChatCompletionRequest(
        model = "test-model",
        temperature = 0.4,
        messages = listOf(
          ChatMessage(role = "system", content = "system prompt"),
          ChatMessage(role = "user", content = "word: threshold")
        )
      )
    )
    assertTrue(json.contains("\"model\":\"test-model\""))
    assertTrue(json.contains("\"messages\""))
  }
}
