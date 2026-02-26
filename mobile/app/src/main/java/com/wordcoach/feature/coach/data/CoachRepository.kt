package com.wordcoach.feature.coach.data

import com.wordcoach.core.config.CoachPromptDefaults
import com.wordcoach.core.network.ApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class CoachRepositoryResult {
  data class Success(val response: CoachResponse) : CoachRepositoryResult()
  data class Failure(val message: String) : CoachRepositoryResult()
}

data class CoachRequestOverrides(
  val providerBaseUrl: String? = null,
  val providerModel: String? = null,
  val providerApiKey: String? = null,
  val systemPrompt: String? = null
)

class CoachRepository(
  private val apiService: ApiService,
  private val requestOverridesProvider: () -> CoachRequestOverrides = { CoachRequestOverrides() },
  private val directProviderClient: DirectProviderClient = OpenAiCompatibleDirectProviderClient()
) {
  suspend fun fetchCoaching(word: String): CoachRepositoryResult = withContext(Dispatchers.IO) {
    val overrides = requestOverridesProvider()
    val hasDirectProvider = !overrides.providerBaseUrl.isNullOrBlank() &&
      !overrides.providerModel.isNullOrBlank() &&
      !overrides.providerApiKey.isNullOrBlank()
    var directError: Throwable? = null

    if (hasDirectProvider) {
      val directResult = runCatching {
        directProviderClient.coach(
          word = word,
          config = DirectProviderConfig(
            baseUrl = overrides.providerBaseUrl ?: error("missing_provider_base_url"),
            model = overrides.providerModel ?: error("missing_provider_model"),
            apiKey = overrides.providerApiKey ?: error("missing_provider_api_key"),
            systemPrompt = overrides.systemPrompt ?: CoachPromptDefaults.defaultSystemPrompt()
          )
        )
      }
      if (directResult.isSuccess) {
        return@withContext CoachRepositoryResult.Success(directResult.getOrThrow())
      }
      directError = directResult.exceptionOrNull()
    }

    return@withContext runCatching {
      apiService.coach(
        CoachRequest(
          word = word,
          providerBaseUrl = overrides.providerBaseUrl,
          providerModel = overrides.providerModel,
          providerApiKey = overrides.providerApiKey,
          systemPrompt = overrides.systemPrompt
        )
      )
    }.fold(
      onSuccess = { CoachRepositoryResult.Success(it) },
      onFailure = {
        val directMsg = directError?.message
        val proxyMsg = it.message ?: "network_error"
        val finalMsg = if (!directMsg.isNullOrBlank()) {
          "direct_provider_failed: $directMsg; proxy_failed: $proxyMsg"
        } else {
          proxyMsg
        }
        CoachRepositoryResult.Failure(finalMsg)
      }
    )
  }
}
