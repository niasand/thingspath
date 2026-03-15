package com.thingspath.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun ItemAvatarPlaceholder(
    name: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.headlineMedium
) {
    val displayText = name.ifBlank { "?" }
    
    val backgroundColor = remember(name) {
        val colors = listOf(
            Color(0xFF4A90E2), Color(0xFF50E3C2), Color(0xFFF5A623),
            Color(0xFFD0021B), Color(0xFF8B572A), Color(0xFF7ED321),
            Color(0xFFBD10E0), Color(0xFF417505), Color(0xFF9013FE)
        )
        val index = if (name.isNotBlank()) {
            (name.hashCode().let { if (it < 0) -it else it } % colors.size)
        } else 0
        colors[index]
    }

    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    colors = listOf(backgroundColor, backgroundColor.copy(alpha = 0.7f))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            style = textStyle.copy(
                fontWeight = FontWeight.Bold,
                color = Color.White
            ),
            textAlign = TextAlign.Center,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis
        )
    }
}
