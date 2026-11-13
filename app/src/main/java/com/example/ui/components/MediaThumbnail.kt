package com.example.ui.components

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.Size
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFramePercent
import com.example.data.model.BackupStatus
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.SoftEmerald
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MediaThumbnail(
    item: MediaItem,
    isSelected: Boolean,
    isMultiSelectMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var coilLoadFailed by remember(item.uri, item.path) { mutableStateOf(false) }
    var coilLoaded by remember(item.uri, item.path) { mutableStateOf(false) }
    var fallbackBitmap by remember(item.uri, item.path) { mutableStateOf<Bitmap?>(null) }

    // System thumbnail fallback for videos if Coil's VideoFrameDecoder fails on any codec/URI
    LaunchedEffect(item.uri, item.path, item.isVideo, coilLoadFailed) {
        if (item.isVideo && coilLoadFailed && fallbackBitmap == null) {
            fallbackBitmap = withContext(Dispatchers.IO) {
                loadVideoThumbnailFallback(context, item)
            }
        }
    }

    val modelRequest = remember(item.uri, item.path, item.isVideo) {
        val dataSource: Any = if (item.uri.isNotBlank()) item.uri else File(item.path)
        ImageRequest.Builder(context)
            .data(dataSource)
            .apply {
                if (item.isVideo) {
                    decoderFactory(VideoFrameDecoder.Factory())
                    videoFramePercent(0.1)
                }
            }
            .crossfade(true)
            .build()
    }

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("media_thumbnail_${item.id}")
    ) {
        // Video placeholder icon while loading or if frame is unavailable
        if (item.isVideo && !coilLoaded && fallbackBitmap == null) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Default.VideoLibrary,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (fallbackBitmap != null) {
            Image(
                bitmap = fallbackBitmap!!.asImageBitmap(),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // Thumbnail Image (supports photos & video frames via VideoFrameDecoder)
            AsyncImage(
                model = modelRequest,
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                onSuccess = {
                    coilLoaded = true
                    coilLoadFailed = false
                },
                onError = {
                    coilLoadFailed = true
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        // Subtle gradient overlay at bottom for legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = if (item.isVideo) 0.75f else 0.55f)
                        )
                    )
                )
        )

        // Centered subtle Play icon badge on videos so video items stand out clearly
        if (item.isVideo && !isMultiSelectMode) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.45f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.7f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Video Title & Duration Overlay (identifies each video at a glance)
        if (item.isVideo) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 5.dp, vertical = 5.dp)
            ) {
                val displayTitle = item.name.substringBeforeLast('.').ifBlank { item.name }
                Text(
                    text = displayTitle,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.size(11.dp)
                    )
                    val durationSec = (item.duration / 1000).coerceAtLeast(1)
                    val mins = durationSec / 60
                    val secs = durationSec % 60
                    Text(
                        text = String.format(Locale.US, "%d:%02d", mins, secs),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(start = 1.dp)
                    )
                }
            }
        }

        // Top-left Badges (SD Card & Cloud status)
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // SD Card badge
            if (item.storageType == StorageType.SD_CARD) {
                Box(
                    modifier = Modifier
                        .padding(end = 4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GoldenVault.copy(alpha = 0.9f))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SdCard,
                            contentDescription = "SD Card",
                            tint = Color.Black,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            text = "SD",
                            fontSize = 9.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }
                }
            }

            // Cloud backup status badge
            when (item.backupStatus) {
                BackupStatus.SYNCED -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Synced to Cloud",
                            tint = SoftEmerald,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                BackupStatus.PENDING -> {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                            .padding(2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Pending Cloud Backup",
                            tint = GoldenVault,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                BackupStatus.NOT_BACKED_UP -> { /* Silent */ }
            }
        }

        // Multi-select Checkmark
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(6.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.45f)
                    )
                    .border(
                        1.5.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                        CircleShape
                    )
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Selected",
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

private fun loadVideoThumbnailFallback(context: android.content.Context, item: MediaItem): Bitmap? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && item.uri.startsWith("content://")) {
        try {
            val bitmap = context.contentResolver.loadThumbnail(
                Uri.parse(item.uri),
                Size(480, 480),
                null
            )
            if (bitmap != null) return bitmap
        } catch (_: Exception) {
        }
    }

    val retriever = MediaMetadataRetriever()
    return try {
        if (item.uri.startsWith("content://")) {
            retriever.setDataSource(context, Uri.parse(item.uri))
        } else if (item.path.isNotBlank()) {
            retriever.setDataSource(item.path)
        } else {
            return null
        }
        retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            ?: retriever.getFrameAtTime(-1L)
    } catch (_: Exception) {
        null
    } finally {
        try {
            retriever.release()
        } catch (_: Exception) {
        }
    }
}

