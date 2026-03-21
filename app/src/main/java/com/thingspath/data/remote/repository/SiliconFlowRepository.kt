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
                        - location 是购买地点或存放位置
                        - note 是额外备注信息
                        - tags 必须根据物品名称智能推断分类标签（单标签优先，选最准确的一个）
                        标签分类规则（严格按此分类，只使用以下标签）：
                        - 家用电器：电视、空调、冰箱、洗衣机、油烟机、燃气灶、洗碗机、消毒柜、热水器、集成灶、电饭煲、破壁机、空气炸锅、微波炉、咖啡机、饮水机、净水器、吸尘器、扫地机器人、挂烫机、吹风机、电动牙刷、按摩椅、足浴盆、净化器、加湿器、除湿机、电风扇、取暖器
                        - 手机通讯：手机、老人机、折叠屏手机、手机壳、贴膜、充电器、移动电源、耳机、数据线
                        - 电脑办公：笔记本、台式机、一体机、平板电脑、显示器、键盘、鼠标、摄像头、音箱、打印机、投影仪、文具、本册、文件收纳
                        - 数码产品：相机、微单、运动相机、三脚架、蓝牙耳机、头戴耳机、蓝牙音箱、家庭影院、智能手表、手环、智能音箱、电子书阅读器、无人机、录音笔、存储卡、U盘、移动硬盘
                        - 家具家居：沙发、茶几、电视柜、床、床垫、衣柜、餐桌、餐椅、四件套、被子、枕头、蚊帐、窗帘、地毯、收纳箱、置物架、衣架、垃圾桶、摆件、挂画、香薰、钟表、炒锅、汤锅、餐具、刀具、菜板、保鲜盒、厨房小工具
                        - 服装内衣：男装(T恤/衬衫/卫衣/夹克/羽绒服/牛仔裤/休闲裤)、女装(T恤/针织衫/连衣裙/半身裙/风衣/大衣/羽绒服)、童装(婴幼连体衣/儿童外套/裤子/家居服)、内衣(文胸/内裤/保暖内衣/秋衣秋裤/睡衣/家居服)、配饰(围巾/帽子/手套/袜子/皮带)
                        - 鞋包珠宝：男鞋(皮鞋/休闲鞋/运动鞋)、女鞋(高跟鞋/单鞋/小白鞋/运动鞋)、双肩包、手提包、斜挎包、行李箱、钱包、卡包、手表、挂钟、闹钟、黄金首饰、银饰、玉石、珍珠、项链、戒指、手链、耳钉
                        - 运动户外：运动衣、速干衣、运动裤、瑜伽裤、运动鞋、运动包、瑜伽垫、哑铃、拉力器、仰卧板、跑步机、动感单车、篮球、足球、羽毛球、乒乓球、跳绳、帐篷、睡袋、登山包、渔具
                        - 美妆个护：洁面、爽肤水、精华、面霜、面膜、眼霜、身体乳、粉底液、口红、眼影、腮红、香水、化妆刷、洗发水、护发素、沐浴露、牙膏、漱口水、冲牙器、洗衣液、洗洁精、油污净、洁厕灵、抽纸、卷纸、湿巾
                        - 宠物用品：狗粮、猫粮、宠物零食、猫砂、宠物窝、宠物玩具、宠物沐浴露
                        - 食品生鲜：坚果、饼干、巧克力、糖果、糕点、肉干、海味零食、大米、面粉、食用油、酱油、醋、调料、火锅底料、牛奶、酸奶、饮料、咖啡、麦片、蜂蜜、白酒、葡萄酒、啤酒、洋酒、黄酒、水果、蔬菜、海鲜、鱼虾、肉类、鸡蛋、熟食、冰淇淋
                        - 母婴用品：奶粉、纸尿裤、拉拉裤、湿巾、米粉、果泥、辅食、儿童零食、奶瓶、奶嘴、吸奶器、温奶器、婴儿餐具、婴儿洗护、润肤霜、护臀膏、婴儿洗衣液、婴儿车、婴儿床、餐椅、安全座椅、积木、毛绒玩具、早教益智玩具
                        - 医药保健：感冒药、肠胃药、创可贴、口罩、碘伏棉签、维生素、钙片、蛋白粉、鱼油、阿胶、人参、枸杞、避孕套、验孕试纸、血压计、血糖仪、体温计、制氧机、轮椅、护腰、护膝、隐形眼镜、护理液、太阳镜
                        - 无法确定：返回 []
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
            try {
                val singleItem = gson.fromJson(jsonString, AIExtractedItem::class.java)
                listOf(singleItem)
            } catch (_: Exception) {
                throw IllegalStateException("Failed to parse AI response: $content", e)
            }
        }
    }
}
