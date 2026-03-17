package com.thingspath.data.remote.repository

import com.google.gson.Gson
import com.thingspath.data.local.datastore.SettingsRepository
import com.thingspath.data.remote.api.SiliconFlowApi
import com.thingspath.data.remote.model.AIExtractedItem
import com.thingspath.data.remote.model.ChatCompletionRequest
import com.thingspath.data.remote.model.Message
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SiliconFlowRepository @Inject constructor(
    private val api: SiliconFlowApi,
    private val settingsRepository: SettingsRepository
) {
    private val gson = Gson()

    suspend fun analyzeText(text: String): AIExtractedItem {
        val apiKey = settingsRepository.apiKey.first() 
            ?: throw IllegalStateException("API Key not set. Please set it in Settings.")
        
        if (apiKey.isBlank()) {
             throw IllegalStateException("API Key is empty. Please set it in Settings.")
        }

        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        val request = ChatCompletionRequest(
            messages = listOf(
                Message(
                    role = "system",
                    content = "你是一个信息抽取器。只输出 JSON，不要输出解释、不要 Markdown 代码块。"
                ),
                Message(
                    role = "user",
                    content = """
                        从文本中提取物品信息并输出 JSON（字段缺失用 null）。
                        今天日期：$today
                        规则：
                        - name 必填（无法识别则给出最可能的简短名称）
                        - price 为数字（例如 5000）
                        - date 输出 YYYY-MM-DD；若出现“昨天/今天/前天”等相对日期，用今天日期推算
                        - tags 输出字符串数组（可为空数组）
                        JSON 结构：
                        {"name":string,"price":number|null,"date":string|null,"location":string|null,"tags":[string],"note":string|null}
                        文本：$text
                    """.trimIndent()
                )
            )
        )

        val response = api.chatCompletions("Bearer $apiKey", request)
        val content = response.choices.firstOrNull()?.message?.content 
            ?: throw IllegalStateException("No response from AI")

        var jsonString = content.trim()
        if (jsonString.startsWith("```")) {
            val lines = jsonString.lines()
            if (lines.size > 2) {
                jsonString = lines.subList(1, lines.lastIndex).joinToString("\n")
            }
        }
        
        return try {
            gson.fromJson(jsonString, AIExtractedItem::class.java)
        } catch (e: Exception) {
            throw IllegalStateException("Failed to parse AI response: $content", e)
        }
    }
}
