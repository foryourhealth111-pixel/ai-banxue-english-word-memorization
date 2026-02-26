package com.wordcoach.feature.coach

import com.wordcoach.core.network.ApiService
import com.wordcoach.feature.coach.data.CoachRepository
import com.wordcoach.feature.coach.data.CoachRepositoryResult
import com.wordcoach.feature.coach.data.CoachRequest
import com.wordcoach.feature.coach.data.CoachRequestOverrides
import com.wordcoach.feature.coach.data.CoachResponse
import com.wordcoach.feature.coach.data.DirectProviderClient
import com.wordcoach.feature.coach.data.DirectProviderConfig
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CoachRepositoryTest {
  @Test
  fun `fetchCoaching returns success when api succeeds`() = runTest {
    val api = object : ApiService {
      override suspend fun coach(request: CoachRequest): CoachResponse {
        return CoachResponse(request.word, "讲解内容", "gemini-proxy")
      }
    }

    val repository = CoachRepository(api)
    val result = repository.fetchCoaching("abandon")
    assertTrue(result is CoachRepositoryResult.Success)
    val success = result as CoachRepositoryResult.Success
    assertEquals("abandon", success.response.word)
  }

  @Test
  fun `fetchCoaching returns failure when api fails`() = runTest {
    val api = object : ApiService {
      override suspend fun coach(request: CoachRequest): CoachResponse {
        error("network down")
      }
    }

    val repository = CoachRepository(api)
    val result = repository.fetchCoaching("abandon")
    assertTrue(result is CoachRepositoryResult.Failure)
  }

  @Test
  fun `fetchCoaching uses direct provider when full override config exists`() = runTest {
    val api = object : ApiService {
      override suspend fun coach(request: CoachRequest): CoachResponse {
        error("proxy should not be called")
      }
    }
    val directClient = object : DirectProviderClient {
      override suspend fun coach(word: String, config: DirectProviderConfig): CoachResponse {
        return CoachResponse(word, "直连讲解", "direct-provider")
      }
    }

    val repository = CoachRepository(
      apiService = api,
      requestOverridesProvider = {
        CoachRequestOverrides(
          providerBaseUrl = "https://example.com",
          providerModel = "custom-model",
          providerApiKey = "api-key-1234567890"
        )
      },
      directProviderClient = directClient
    )

    val result = repository.fetchCoaching("threshold")
    assertTrue(result is CoachRepositoryResult.Success)
    val success = result as CoachRepositoryResult.Success
    assertEquals("direct-provider", success.response.source)
  }

  @Test
  fun `fetchCoaching falls back to proxy when direct provider fails`() = runTest {
    val api = object : ApiService {
      override suspend fun coach(request: CoachRequest): CoachResponse {
        return CoachResponse(request.word, "代理讲解", "gemini-proxy")
      }
    }
    val directClient = object : DirectProviderClient {
      override suspend fun coach(word: String, config: DirectProviderConfig): CoachResponse {
        error("direct down")
      }
    }

    val repository = CoachRepository(
      apiService = api,
      requestOverridesProvider = {
        CoachRequestOverrides(
          providerBaseUrl = "https://example.com",
          providerModel = "custom-model",
          providerApiKey = "api-key-1234567890"
        )
      },
      directProviderClient = directClient
    )

    val result = repository.fetchCoaching("threshold")
    assertTrue(result is CoachRepositoryResult.Success)
    val success = result as CoachRepositoryResult.Success
    assertEquals("gemini-proxy", success.response.source)
  }
}
