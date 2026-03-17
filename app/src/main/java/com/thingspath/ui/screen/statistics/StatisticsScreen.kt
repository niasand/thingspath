package com.thingspath.ui.screen.statistics

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
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
import kotlin.math.cos
import kotlin.math.sin

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistics") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Items", style = MaterialTheme.typography.labelLarge)
                        Text("${state.totalItems}", style = MaterialTheme.typography.headlineMedium)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Total Value", style = MaterialTheme.typography.labelLarge)
                        Text(String.format("%.2f", state.totalPrice), style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }

            // Tag Distribution Chart
            if (state.tagDistribution.isNotEmpty()) {
                Text("Top Tags", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                PieChart(
                    data = state.tagDistribution.map { Triple(it.percentage, it.color, "${it.tag} (${it.count})") }
                )
            }

            // Price Distribution Chart
            if (state.priceDistribution.isNotEmpty()) {
                Text("Price Range Distribution", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                PieChart(
                    data = state.priceDistribution.map { Triple(it.percentage, it.color, "${it.range} (${it.count})") }
                )
            }
        }
    }
}

@Composable
fun PieChart(
    data: List<Triple<Float, Color, String>>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                var startAngle = -90f
                val radius = size.minDimension / 2
                val strokeWidth = 40.dp.toPx()

                data.forEach { (percentage, color, _) ->
                    val sweepAngle = percentage * 360f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth),
                        size = Size(size.width - strokeWidth, size.height - strokeWidth),
                        topLeft = Offset(strokeWidth / 2, strokeWidth / 2)
                    )
                    startAngle += sweepAngle
                }
            }
        }

        Column(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            data.forEach { (_, color, label) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(12.dp),
                        color = color,
                        shape = MaterialTheme.shapes.extraSmall
                    ) {}
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
