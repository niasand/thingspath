package com.thingspath.ui.screen.itemdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.thingspath.data.model.Item
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Composable
fun ViewModeContent(item: Item?, relatedItems: List<Item> = emptyList()) {
    item ?: return
    val usageDays = remember(item.purchaseDate) {
        item.purchaseDate?.let { calculateUsageDaysFromPurchaseDate(it) }
    } ?: item.usageDays

    DetailField(label = "名称", value = item.name)

    item.location?.let {
        DetailField(label = "位置", value = it)
    }

    item.purchaseDate?.let {
        DetailField(label = "购买日期", value = formatDate(it))
    }

    if (item.purchasePrice > 0) {
        DetailField(label = "价格", value = String.format("%.2f", item.purchasePrice))
    }

    usageDays?.let { days ->
        DetailField(label = "使用天数", value = "$days 天")

        if (item.purchasePrice > 0 && days > 0) {
            val dailyCost = item.purchasePrice / days
            DetailField(label = "每日成本", value = String.format("%.2f / 天", dailyCost))
        }
    }

    item.reminderDate?.let { reminderDate ->
        val status = reminderStatus(reminderDate)
        DetailField(
            label = item.reminderType ?: "提醒",
            value = buildString {
                append(formatDate(reminderDate))
                append(" · ")
                append(status)
                item.reminderNote?.takeIf { it.isNotBlank() }?.let { note ->
                    append("\n")
                    append(note)
                }
            }
        )
    }

    item.setName?.takeIf { it.isNotBlank() }?.let { setName ->
        DetailField(
            label = "套装",
            value = buildString {
                append(setName)
                item.setNote?.takeIf { it.isNotBlank() }?.let { note ->
                    append("\n")
                    append(note)
                }
            }
        )

        if (relatedItems.isNotEmpty()) {
            Column {
                Text(
                    text = "同套装物品",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                relatedItems.forEach { related ->
                    Text(
                        text = related.name,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }

    if (!item.note.isNullOrBlank()) {
        DetailField(label = "备注", value = item.note)
    }

    if (item.tags.isNotEmpty()) {
        Column {
            Text(
                text = "标签",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.tags.forEach { tag ->
                    SuggestionChip(
                        onClick = { },
                        label = { Text(tag) }
                    )
                }
            }
        }
    }

    DetailField(label = "添加日期", value = formatDate(item.createdAt))
}

private fun reminderStatus(reminderDateMillis: Long): String {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val days = TimeUnit.MILLISECONDS.toDays(reminderDateMillis - todayStart).toInt()
    return when {
        days < 0 -> "已逾期 ${-days} 天"
        days == 0 -> "今天到期"
        else -> "还有 $days 天"
    }
}

@Composable
fun DetailField(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun calculateUsageDaysFromPurchaseDate(purchaseDateMillis: Long): Int? {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val todayStart = cal.timeInMillis
    val diff = todayStart - purchaseDateMillis
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return if (days >= 0) days.toInt() else null
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("yyyy年M月d日", Locale.getDefault())
    return format.format(date)
}
