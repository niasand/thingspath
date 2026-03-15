package com.thingspath.ui.screen.aiadd

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIAddScreen(
    viewModel: AIAddViewModel,
    onBack: () -> Unit,
    onResult: (String, String?, String?, Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    // Premium Light Background Gradient (Vibrant but Light/Day mode)
    val backgroundBrush = Brush.verticalGradient(
        colors = listOf(
            Color(0xFFF0F4FF), // Very Light Blue/White
            Color(0xFFFFFFFF),
            Color(0xFFE8F0FE)
        )
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .imePadding()
    ) {
        // Back Button
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF1A1A2E))
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // AI Icon
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier
                    .size(80.dp)
                    .padding(8.dp),
                tint = Color(0xFF4A90E2) // Vibrant Blue for Light Mode
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "AI 智能添加",
                style = MaterialTheme.typography.headlineMedium.copy(
                    color = Color(0xFF1A1A2E),
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                ),
                textAlign = TextAlign.Center
            )

            Text(
                text = "ThingsPath AI Engine (Day Mode)",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF1A1A2E).copy(alpha = 0.6f),
                    fontWeight = FontWeight.Medium
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Premium Card for Light Mode
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)),
                color = Color.White,
                shadowElevation = 8.dp,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4A90E2).copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "请描述您的物品：",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color(0xFF1A1A2E),
                            fontWeight = FontWeight.Medium
                        ),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedTextField(
                        value = state.inputText,
                        onValueChange = { viewModel.onInputTextChange(it) },
                        placeholder = { 
                            Text(
                                "例如：昨天在小米买了200块的充电宝", 
                                color = Color.Gray.copy(alpha = 0.6f)
                            ) 
                        },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 4,
                        maxLines = 6,
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF1A1A2E),
                            unfocusedTextColor = Color(0xFF1A1A2E),
                            cursorColor = Color(0xFF4A90E2),
                            focusedBorderColor = Color(0xFF4A90E2),
                            unfocusedBorderColor = Color.Gray.copy(alpha = 0.3f),
                        )
                    )

                    if (state.error != null) {
                        Text(
                            text = state.error!!,
                            color = Color(0xFFFF4D4D),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.extractInfo { info ->
                                onResult(
                                    info.name ?: "",
                                    info.purchaseDate,
                                    info.location,
                                    info.purchasePrice
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = !state.isLoading && state.inputText.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4A90E2),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFF4A90E2).copy(alpha = 0.5f)
                        )
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "开始引擎解析", 
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Text(
                text = "AI 将识别名称、价格、日期和位置并自动保存",
                style = MaterialTheme.typography.bodySmall.copy(color = Color.Gray),
                textAlign = TextAlign.Center
            )
        }
    }
}
