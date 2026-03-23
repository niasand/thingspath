package com.thingspath.ui.screen.statistics

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.thingspath.domain.usecase.GetItemsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StatisticsState(
    val totalItems: Int = 0,
    val totalPrice: Double = 0.0,
    val tagDistribution: List<TagStat> = emptyList(),
    val priceDistribution: List<PriceRangeStat> = emptyList(),
    val locationDistribution: List<LocationStat> = emptyList()
)

data class LocationStat(val location: String, val count: Int, val percentage: Float, val color: Color)

data class TagStat(val tag: String, val count: Int, val percentage: Float, val color: Color)
data class PriceRangeStat(val range: String, val count: Int, val percentage: Float, val color: Color)

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val getItemsUseCase: GetItemsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(StatisticsState())
    val state: StateFlow<StatisticsState> = _state.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            getItemsUseCase("").collectLatest { items ->
                val totalCount = items.size
                val totalPrice = items.sumOf { it.purchasePrice }

                // Tag Distribution
                val tagCounts = items.flatMap { it.tags }
                    .groupingBy { it }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(5) // Top 5 tags

                val totalTags = tagCounts.sumOf { it.second }
                val tagStats = tagCounts.mapIndexed { index, (tag, count) ->
                    TagStat(
                        tag = tag,
                        count = count,
                        percentage = if (totalTags > 0) count.toFloat() / totalTags else 0f,
                        color = getChartColor(index)
                    )
                }

                // Price Distribution - 3 tiers: <1000, 1000-3000, >3000
                val priceRanges = items.groupBy { item ->
                    when {
                        item.purchasePrice < 1000 -> "< 1000"
                        item.purchasePrice <= 3000 -> "1000 - 3000"
                        else -> "> 3000"
                    }
                }

                val priceStats = listOf("< 1000", "1000 - 3000", "> 3000")
                    .mapIndexed { index, range ->
                        val count = priceRanges[range]?.size ?: 0
                        PriceRangeStat(
                            range = range,
                            count = count,
                            percentage = if (totalCount > 0) count.toFloat() / totalCount else 0f,
                            color = getPriceColor(index)
                        )
                    }

                // Location Distribution
                val locationGroups = items
                    .groupingBy { it.location?.takeIf { it.isNotBlank() } ?: "Unknown" }
                    .eachCount()
                    .toList()
                    .sortedByDescending { it.second }
                    .take(6) // Top 6 locations

                val totalLocations = locationGroups.sumOf { it.second }
                val locationStats = locationGroups.mapIndexed { index, (location, count) ->
                    LocationStat(
                        location = location,
                        count = count,
                        percentage = if (totalLocations > 0) count.toFloat() / totalLocations else 0f,
                        color = getLocationColor(index)
                    )
                }

                _state.update {
                    it.copy(
                        totalItems = totalCount,
                        totalPrice = totalPrice,
                        tagDistribution = tagStats,
                        priceDistribution = priceStats,
                        locationDistribution = locationStats
                    )
                }
            }
        }
    }

    private fun getChartColor(index: Int): Color {
        // 赤橙黄绿青蓝紫 - 彩虹混搭
        val colors = listOf(
            Color(0xFFF44336), // Red
            Color(0xFFFF9800), // Orange
            Color(0xFFFFEB3B), // Yellow
            Color(0xFF4CAF50), // Green
            Color(0xFF00BCD4), // Cyan
            Color(0xFF2196F3), // Blue
            Color(0xFF9C27B0)  // Purple
        )
        return colors[index % colors.size]
    }

    private fun getPriceColor(index: Int): Color {
        // 3档价格用差异明显的颜色
        val colors = listOf(
            Color(0xFF4CAF50), // Green - 低价
            Color(0xFFFF9800), // Orange - 中价
            Color(0xFFF44336)  // Red - 高价
        )
        return colors[index % colors.size]
    }

    private fun getLocationColor(index: Int): Color {
        // 赤橙黄绿青蓝紫 - 彩虹混搭
        val colors = listOf(
            Color(0xFFE91E63), // Pink
            Color(0xFF9C27B0), // Purple
            Color(0xFF3F51B5), // Indigo
            Color(0xFF03A9F4), // Light Blue
            Color(0xFF00BCD4), // Cyan
            Color(0xFF4CAF50), // Green
            Color(0xFF8BC34A), // Light Green
            Color(0xFFFFEB3B), // Yellow
            Color(0xFFFF9800), // Orange
            Color(0xFFF44336)  // Red
        )
        return colors[index % colors.size]
    }
}
