package com.thingspath.data.remote.model

import com.google.gson.annotations.SerializedName

data class ChatCompletionRequest(
    val model: String = "Pro/deepseek-ai/DeepSeek-V3.2",
    val messages: List<Message>,
    val stream: Boolean = false,
    @SerializedName("enable_thinking") val enableThinking: Boolean = false,
    @SerializedName("thinking_budget") val thinkingBudget: Int = 1024,
    val temperature: Double = 0.2,
    @SerializedName("max_tokens") val maxTokens: Int = 1024
)

data class Message(
    val role: String,
    val content: String,
    @SerializedName("reasoning_content") val reasoningContent: String? = null
)

data class ChatCompletionResponse(
    val id: String,
    val choices: List<Choice>,
    val created: Long,
    val model: String
)

data class Choice(
    val index: Int,
    val message: Message,
    @SerializedName("finish_reason") val finishReason: String
)

// Helper class for parsing AI extracted item data
data class AIExtractedItem(
    val name: String?,
    val price: Double?,
    val date: String?, // Format: YYYY-MM-DD
    val location: String?,
    val tags: List<String>?,
    val note: String?
)
