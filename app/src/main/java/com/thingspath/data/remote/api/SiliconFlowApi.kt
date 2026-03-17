package com.thingspath.data.remote.api

import com.thingspath.data.remote.model.ChatCompletionRequest
import com.thingspath.data.remote.model.ChatCompletionResponse
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

interface SiliconFlowApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun chatCompletions(
        @Header("Authorization") authorization: String,
        @Body request: ChatCompletionRequest
    ): ChatCompletionResponse
}
