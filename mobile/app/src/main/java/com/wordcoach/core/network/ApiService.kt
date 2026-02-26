package com.wordcoach.core.network

import com.wordcoach.feature.coach.data.CoachRequest
import com.wordcoach.feature.coach.data.CoachResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
  @POST("/v1/coach")
  suspend fun coach(@Body request: CoachRequest): CoachResponse
}
