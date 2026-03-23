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
import kotlin.math.cos
import kotlin.math.sin

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
