package com.thingspath.data.remote

import com.google.gson.Gson
import com.thingspath.BuildConfig
import com.thingspath.data.model.ExtractedItemInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit

class SiliconFlowClient {

    private val gson = Gson()
    
    // Use the API key from BuildConfig (populated from local.properties)
    private val apiKey = BuildConfig.SILICON_FLOW_API_KEY
    private val apiUrl = "https://api.siliconflow.cn/v1/chat/completions"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    suspend fun extractItemInfo(text: String): Result<ExtractedItemInfo> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "YOUR_API_KEY_HERE") {
            return@withContext Result.failure(Exception("SiliconFlow API Key not configured. Please add SILICON_FLOW_API_KEY to local.properties"))
        }

        val prompt = """
            你是一个帮我记录物品的智能助手。请从下面这段用户输入的文字中，提取物品的相关信息。
            必须提取以下 4 个字段的信息（如果没有提供则留空或传 null）：
            1. name (物品名称，必须有)
            2. purchaseDate (购买日期，必须格式化为 YYYY-MM-DD，只要日期，不要时间)
            3. location (存放位置)
            4. purchasePrice (购买价格，只要数字，不需要单位，保留两位小数)
            
            只能且必须返回纯净的 JSON 字符串，不要包含任何 json 代码块标记 (如 ```json) 或多余的说明文字，严格满足以下格式：
            {
              "name": "xxx",
              "purchaseDate": "2024-03-10",
              "location": "xxx",
              "purchasePrice": 199.00
            }

            用户输入文本：
            ${text}
        """.trimIndent()

        val requestBodyMap = mutableMapOf(
            "model" to "Qwen/Qwen2-7B-Instruct",
            "messages" to listOf(
                mapOf("role" to "system", "content" to "你是一个专门从文本中提取物品信息的助手。你必须始终返回一个合法的 JSON 对象。"),
                mapOf("role" to "user", "content" to prompt)
            ),
            "response_format" to mapOf("type" to "json_object"),
            "temperature" to 0.0,
            "max_tokens" to 512
        )

        val jsonBody = gson.toJson(requestBodyMap)

        val request = Request.Builder()
            .url(apiUrl)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(jsonBody.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("API request failed with code ${response.code}: ${response.message}"))
            }

            val bodyString = response.body?.string() ?: ""
            
            // The API returns a standard OpenAI chat completion JSON structure
            val responseMap = gson.fromJson(bodyString, Map::class.java)
            val choices = responseMap["choices"] as? List<Map<String, Any>>
            var content = (choices?.firstOrNull()?.get("message") as? Map<String, Any>)?.get("content") as? String ?: ""

            // Clean up the output in case the model wraps it in Markdown json blocks
            content = content.trim()
            if (content.startsWith("```json")) {
                content = content.substringAfter("```json").substringBeforeLast("```").trim()
            } else if (content.startsWith("```")) {
                content = content.substringAfter("```").substringBeforeLast("```").trim()
            }

            val extractedInfo = gson.fromJson(content, ExtractedItemInfo::class.java)
            if (extractedInfo.name.isNullOrBlank()) {
                 return@withContext Result.failure(Exception("Failed to extract item name from text."))
            }
            Result.success(extractedInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
