package com.thingspath.ui.screen.itemdetail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.thingspath.ui.component.ItemImagePlaceholder
import java.io.File

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MultiImageViewer(
    imagePaths: List<String>,
    itemName: String?,
    onImageClick: (index: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (imagePaths.isEmpty()) {
        ItemImagePlaceholder(
            name = itemName,
            modifier = modifier.clip(MaterialTheme.shapes.medium),
            shape = MaterialTheme.shapes.medium,
            maxLines = 2
        )
        return
    }

    if (imagePaths.size == 1) {
        Box(
            modifier = modifier
                .clip(MaterialTheme.shapes.medium)
                .clickable { onImageClick(0) }
        ) {
            RobustAsyncImage(
                model = resolveImageModel(imagePaths[0]),
                contentDescription = "物品图片",
                modifier = Modifier.fillMaxSize(),
                placeholderName = itemName
            )
        }
        return
    }

    val pagerState = rememberPagerState(pageCount = { imagePaths.size })
    Box(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            RobustAsyncImage(
                model = resolveImageModel(imagePaths[page]),
                contentDescription = "物品图片 ${page + 1}",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onImageClick(page) },
                placeholderName = itemName
            )
        }
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(imagePaths.size) { index ->
                Box(
                    modifier = Modifier
                        .size(if (pagerState.currentPage == index) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline
                        )
                )
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp),
            color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = "${pagerState.currentPage + 1}/${imagePaths.size}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
    }
}

@Composable
fun RobustAsyncImage(
    model: Any,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholderName: String? = null
) {
    var loadFailed by remember(model) { mutableStateOf(false) }
    if (!loadFailed) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale,
            onError = { loadFailed = true }
        )
    }
    if (loadFailed) {
        ItemImagePlaceholder(
            name = placeholderName,
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            maxLines = 2
        )
    }
}

fun resolveImageModel(path: String): Any {
    return if (path.startsWith("http://") || path.startsWith("https://")) path else File(path)
}
