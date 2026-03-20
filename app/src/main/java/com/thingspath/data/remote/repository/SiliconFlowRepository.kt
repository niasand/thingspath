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

    suspend fun analyzeText(text: String): List<AIExtractedItem> {
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
                        从文本中提取所有物品信息并输出 JSON 数组（字段缺失用 null）。
                        今天日期：$today
                        规则：
                        - 如果文本包含多个物品，返回数组包含所有识别出的物品
                        - 如果只有一个物品，返回单元素数组
                        - name 必填（无法识别则给出最可能的简短名称）
                        - price 为数字（例如 5000）
                        - date 输出 YYYY-MM-DD；若出现"昨天/今天/前天"等相对日期，用今天日期推算
                        - tags 输出字符串数组，必须根据物品名称自动推断分类标签
                        标签规则（根据物品名称智能分类）：
                        - 水果类：苹果、香蕉、橘子、葡萄、西瓜等 -> ["水果"]
                        - 蔬菜/食品类：萝卜、白菜、辣椒酱、大米、面条等 -> ["食品"]
                        - 家具类：桌子、椅子、床、沙发、衣柜等 -> ["家具"]
                        - 电器类：电视、冰箱、洗衣机、空调、电脑等 -> ["电器"]
                        - 数码类：手机、耳机、充电器、数据线等 -> ["数码"]
                        - 服装类：衣服、裤子、鞋子、帽子等 -> ["服装"]
                        - 化妆品类：口红、面霜、洗面奶等 -> ["化妆品"]
                        - 药品类：感冒药、止痛药、维生素等 -> ["药品"]
                        - 文具类：笔、本子、文件夹等 -> ["文具"]
                        - 无法分类的 -> []（空数组）
                        JSON 结构：
                        [{"name":string,"price":number|null,"date":string|null,"location":string|null,"tags":[string],"note":string|null}]
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
            val listType = object : com.google.gson.reflect.TypeToken<List<AIExtractedItem>>() {}.type
            gson.fromJson(jsonString, listType)
        } catch (e: Exception) {
            // 尝试解析为单个对象并包装为列表（兼容旧格式）
            try {
                val singleItem = gson.fromJson(jsonString, AIExtractedItem::class.java)
                listOf(singleItem)
            } catch (_: Exception) {
                throw IllegalStateException("Failed to parse AI response: $content", e)
            }
        }
    }
}
