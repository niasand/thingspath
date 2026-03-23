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
    val priceDistribution: List<PriceRangeStat> = emptyList()
)

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

                // Price Distribution
                val priceRanges = items.groupBy { item ->
                    when {
                        item.purchasePrice < 100 -> "< 100"
                        item.purchasePrice < 500 -> "100 - 500"
                        item.purchasePrice < 1000 -> "500 - 1000"
                        item.purchasePrice < 5000 -> "1000 - 5000"
                        else -> "> 5000"
                    }
                }

                val priceStats = listOf("< 100", "100 - 500", "500 - 1000", "1000 - 5000", "> 5000")
                    .mapIndexed { index, range ->
                        val count = priceRanges[range]?.size ?: 0
                        PriceRangeStat(
                            range = range,
                            count = count,
                            percentage = if (totalCount > 0) count.toFloat() / totalCount else 0f,
                            color = getChartColor(index)
                        )
                    }.filter { it.count > 0 }

                _state.update {
                    it.copy(
                        totalItems = totalCount,
                        totalPrice = totalPrice,
                        tagDistribution = tagStats,
                        priceDistribution = priceStats
                    )
                }
            }
        }
    }

    private fun getChartColor(index: Int): Color {
        val colors = listOf(
            Color(0xFF6750A4), Color(0xFFB58392), Color(0xFF7D5260),
            Color(0xFF625B71), Color(0xFFCCC2DC), Color(0xFFE8DEF8)
        )
        return colors[index % colors.size]
    }
}
