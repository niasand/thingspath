package com.thingspath.ui.screen.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun StatisticsHeader(
    totalCount: Int,
    totalPrice: Double,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "物品总数",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$totalCount",
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            VerticalDivider(
                modifier = Modifier.height(40.dp)
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "总价值",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = String.format("%.2f", totalPrice),
                    style = MaterialTheme.typography.headlineMedium
                )
            }

            VerticalDivider(
                modifier = Modifier.height(40.dp)
            )

            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = "查看统计",
                tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
            )
        }
    }
}
