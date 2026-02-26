package com.wordcoach.feature.coach.data

data class CoachRequest(
  val word: String,
  val locale: String = "zh-CN",
  val providerBaseUrl: String? = null,
  val providerModel: String? = null,
  val providerApiKey: String? = null,
  val systemPrompt: String? = null
)

data class CoachResponse(
  val word: String,
  val explanation: String,
  val source: String
)
