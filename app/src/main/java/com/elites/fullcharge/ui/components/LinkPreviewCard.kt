package com.elites.fullcharge.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.elites.fullcharge.data.LinkPreview
import com.elites.fullcharge.ui.theme.*
import com.elites.fullcharge.util.LinkPreviewFetcher
import kotlinx.coroutines.launch

@Composable
fun LinkPreviewCard(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var linkPreview by remember { mutableStateOf<LinkPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        scope.launch {
            linkPreview = LinkPreviewFetcher.fetchPreview(url)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TossBlue,
                strokeWidth = 2.dp
            )
        }
    } else {
        linkPreview?.let { preview ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color.Black.copy(alpha = 0.15f))
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preview.url))
                        context.startActivity(intent)
                    }
            ) {
                Column {
                    // 썸네일 이미지
                    preview.imageUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Link preview image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 텍스트 정보
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // 사이트 이름
                        preview.siteName?.let { siteName ->
                            Text(
                                text = siteName,
                                fontSize = 11.sp,
                                color = TossBlue,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 제목
                        preview.title?.let { title ->
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 설명
                        preview.description?.let { description ->
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 제목/설명 없으면 URL 표시
                        if (preview.title == null && preview.description == null) {
                            Text(
                                text = preview.url,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LinkPreviewCardLight(
    url: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var linkPreview by remember { mutableStateOf<LinkPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(url) {
        scope.launch {
            linkPreview = LinkPreviewFetcher.fetchPreview(url)
            isLoading = false
        }
    }

    if (isLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BackgroundGray),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TossBlue,
                strokeWidth = 2.dp
            )
        }
    } else {
        linkPreview?.let { preview ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BackgroundGray)
                    .clickable {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(preview.url))
                        context.startActivity(intent)
                    }
            ) {
                Column {
                    // 썸네일 이미지
                    preview.imageUrl?.let { imageUrl ->
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Link preview image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 텍스트 정보
                    Column(
                        modifier = Modifier.padding(12.dp)
                    ) {
                        // 사이트 이름
                        preview.siteName?.let { siteName ->
                            Text(
                                text = siteName,
                                fontSize = 11.sp,
                                color = TossBlue,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 제목
                        preview.title?.let { title ->
                            Text(
                                text = title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                        }

                        // 설명
                        preview.description?.let { description ->
                            Text(
                                text = description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // 제목/설명 없으면 URL 표시
                        if (preview.title == null && preview.description == null) {
                            Text(
                                text = preview.url,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
