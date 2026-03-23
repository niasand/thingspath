package com.thingspath.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thingspath.data.model.Item
import java.text.SimpleDateFormat
import java.util.*

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.combinedClickable
import java.util.concurrent.TimeUnit

// Purple theme colors from the design
private val PurplePrimary = Color(0xFF6B4EFF)
private val PurpleLight = Color(0xFFE8E5FF)
private val PinkLight = Color(0xFFFFE8EC)
private val PinkPrimary = Color(0xFFFF6B8A)
private val GrayText = Color(0xFF9B9B9B)
private val GrayLight = Color(0xFFF5F5F7)

// Compute usage days dynamically from purchase date
private fun computeUsageDays(purchaseDate: Long?): Int? {
    if (purchaseDate == null) return null
    val todayStart = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    val diff = todayStart - purchaseDate
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return if (days >= 0) days.toInt() else null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemCard(
    item: Item,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp),
        border = if (isSelected) BorderStroke(2.dp, PurplePrimary) else null,
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail image - rounded square with light background
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(GrayLight),
                contentAlignment = Alignment.Center
            ) {
                if (item.imagePath != null) {
                    AsyncImage(
                        model = item.imagePath,
                        contentDescription = "Item image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    ItemImagePlaceholder(
                        name = item.name,
                        modifier = Modifier.fillMaxSize(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 2,
                        backgroundColor = GrayLight,
                        textColor = PurplePrimary
                    )
                }
            }

            // Content column
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // First row: name (left) + price (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )

                    // Price tag - purple light background with $ symbol
                    if (item.purchasePrice > 0) {
                        Surface(
                            color = PurpleLight,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = "¥${String.format("%.2f", item.purchasePrice)}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = PurplePrimary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                // Second row: location + date (left)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (item.location != null) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = GrayText
                            )
                            Text(
                                text = item.location,
                                style = MaterialTheme.typography.labelSmall,
                                color = GrayText
                            )
                        }
                        if (item.purchaseDate != null) {
                            Text(
                                text = "${if (item.location != null) "· " else ""}${formatDate(item.purchaseDate)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = GrayText
                            )
                        }
                    }
                }

                // Compute dynamic usage days for display
                val dynamicUsageDays = computeUsageDays(item.purchaseDate)

                // Third row: daily cost + usage (left) + tags (right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left side: daily cost + usage
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Daily cost - pink light background (use dynamic usage days)
                        if (item.purchasePrice > 0 && dynamicUsageDays != null && dynamicUsageDays > 0) {
                            val dailyCost = item.purchasePrice / dynamicUsageDays
                            Surface(
                                color = PinkLight,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text(
                                    text = "${String.format("%.1f", dailyCost)} / Day",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = PinkPrimary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        // Usage days - "已使用" in gray, "Xd" in purple (use dynamic value)
                        if (dynamicUsageDays != null) {
                            Row {
                                Text(
                                    text = "已使用 ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GrayText,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "${dynamicUsageDays}d",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = PurplePrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }

                    // Right side: tags
                    if (item.tags.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            item.tags.take(2).forEach { tag ->
                                Surface(
                                    color = Color(0xFFF0F0F0),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = "#$tag",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = GrayText,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Selection checkbox (when in selection mode)
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = null,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(date)
}
