package com.example.ui.components

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BackupStatus
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.SoftEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MediaViewerDialog(
    item: MediaItem,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onToggleFavorite: (MediaItem) -> Unit,
    onMoveToStorage: (MediaItem, StorageType) -> Unit,
    onMoveToVault: (MediaItem) -> Unit,
    onRestoreFromVault: (MediaItem) -> Unit,
    onAddToAlbum: (MediaItem) -> Unit,
    onDelete: (MediaItem) -> Unit,
    onPlayVideoInMxPlayer: ((MediaItem) -> Unit)? = null
) {
    val context = LocalContext.current
    var showInfoSheet by remember { mutableStateOf(false) }

    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Interactive Zoom/Pan Image
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.8f, 5f)
                                if (scale > 1f) {
                                    offsetX += pan.x
                                    offsetY += pan.y
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(item.uri)
                            .crossfade(true)
                            .build(),
                        contentDescription = item.name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offsetX,
                                translationY = offsetY
                            )
                    )

                    // Video Play Icon Overlay (Launches MX Player)
                    if (item.isVideo) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .clickable {
                                    onPlayVideoInMxPlayer?.invoke(item)
                                }
                                .testTag("viewer_center_play_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video in MX Player",
                                tint = Color.White,
                                modifier = Modifier.size(46.dp)
                            )
                        }
                    }
                }

                // Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("viewer_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = item.name,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val storageLabel = if (item.storageType == StorageType.SD_CARD) {
                                if (isBangla) "এসডি কার্ড" else "SD Card"
                            } else {
                                if (isBangla) "ইন্টারনাল মেমোরি" else "Internal Storage"
                            }
                            Text(
                                text = "$storageLabel • ${item.albumName}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Info button
                    IconButton(onClick = { showInfoSheet = !showInfoSheet }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Details",
                            tint = if (showInfoSheet) GoldenVault else Color.White
                        )
                    }

                    // Share button
                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = item.mimeType
                                putExtra(Intent.EXTRA_STREAM, android.net.Uri.parse(item.uri))
                                putExtra(Intent.EXTRA_TEXT, item.name)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = Color.White
                        )
                    }
                }

                // Bottom Action Bar with clear icon and text labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.82f))
                        .navigationBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ViewerActionItem(
                        icon = if (item.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        label = if (isBangla) "পছন্দ" else "Favorite",
                        tint = if (item.isFavorite) Color(0xFFEF4444) else Color.White,
                        tag = "viewer_favorite_btn",
                        onClick = { onToggleFavorite(item) }
                    )

                    ViewerActionItem(
                        icon = if (item.storageType == StorageType.SD_CARD) Icons.Default.Smartphone else Icons.Default.SdCard,
                        label = if (item.storageType == StorageType.SD_CARD) {
                            if (isBangla) "ইন্টারনাল" else "Internal"
                        } else {
                            if (isBangla) "এসডি কার্ড" else "SD Card"
                        },
                        tint = GoldenVault,
                        tag = "viewer_sdcard_btn",
                        onClick = {
                            val target = if (item.storageType == StorageType.SD_CARD) StorageType.INTERNAL else StorageType.SD_CARD
                            onMoveToStorage(item, target)
                        }
                    )

                    ViewerActionItem(
                        icon = Icons.Default.FolderSpecial,
                        label = if (isBangla) "অ্যালবাম" else "Album",
                        tint = Color.White,
                        tag = "viewer_add_album_btn",
                        onClick = { onAddToAlbum(item) }
                    )

                    ViewerActionItem(
                        icon = if (item.isVault) Icons.Default.LockOpen else Icons.Default.Lock,
                        label = if (item.isVault) {
                            if (isBangla) "আনলক" else "Restore"
                        } else {
                            if (isBangla) "ভল্টে পাঠান" else "Lock Vault"
                        },
                        tint = if (item.isVault) SoftEmerald else GoldenVault,
                        tag = "viewer_vault_btn",
                        isHighlight = !item.isVault,
                        onClick = {
                            if (item.isVault) {
                                onRestoreFromVault(item)
                            } else {
                                onMoveToVault(item)
                            }
                        }
                    )

                    ViewerActionItem(
                        icon = Icons.Default.Delete,
                        label = if (isBangla) "মুছুন" else "Delete",
                        tint = Color(0xFFF87171),
                        tag = "viewer_delete_btn",
                        onClick = { onDelete(item) }
                    )
                }

                // Slide-up Info Sheet
                AnimatedVisibility(
                    visible = showInfoSheet,
                    enter = slideInVertically { it },
                    exit = slideOutVertically { it },
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Card(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 56.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = if (isBangla) "মিডিয়া তথ্য" else "Media Details",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                IconButton(onClick = { showInfoSheet = false }) {
                                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = "Close", tint = MaterialTheme.colorScheme.primary)
                                }
                            }

                            HorizontalDivider(
                                color = DividerDefaults.color.copy(alpha = 0.3f),
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            InfoRow(
                                label = if (isBangla) "ফাইলের নাম" else "File Name",
                                value = item.name
                            )
                            val sizeMb = item.size / (1024f * 1024f)
                            InfoRow(
                                label = if (isBangla) "সাইজ" else "Size",
                                value = String.format(Locale.US, "%.2f MB", sizeMb)
                            )
                            InfoRow(
                                label = if (isBangla) "স্টোরেজ অবস্থান" else "Storage Location",
                                value = if (item.storageType == StorageType.SD_CARD) {
                                    if (isBangla) "মাইক্রো এসডি কার্ড (রিমুভেবল)" else "Micro SD Card (Removable)"
                                } else {
                                    if (isBangla) "ইন্টারনাল ফোন মেমোরি" else "Internal Phone Storage"
                                },
                                highlightColor = if (item.storageType == StorageType.SD_CARD) GoldenVault else null
                            )
                            val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                            InfoRow(
                                label = if (isBangla) "তারিখ ও সময়" else "Date Added",
                                value = dateFormat.format(Date(item.dateAdded))
                            )
                            InfoRow(
                                label = if (isBangla) "অ্যালবাম" else "Album",
                                value = item.albumName
                            )
                            InfoRow(
                                label = if (isBangla) "ক্লাউড স্ট্যাটাস" else "Cloud Sync",
                                value = when (item.backupStatus) {
                                    BackupStatus.SYNCED -> if (isBangla) "ব্যাকআপ সম্পন্ন (Google Drive)" else "Backed Up (Google Drive)"
                                    BackupStatus.PENDING -> if (isBangla) "ব্যাকআপ পেন্ডিং" else "Pending Sync"
                                    BackupStatus.NOT_BACKED_UP -> if (isBangla) "শুধুমাত্র অফলাইনে" else "Offline Only"
                                },
                                highlightColor = if (item.backupStatus == BackupStatus.SYNCED) SoftEmerald else null
                            )
                            InfoRow(
                                label = if (isBangla) "ফাইলের পাথ" else "Path",
                                value = item.path.ifEmpty { item.uri }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    highlightColor: Color? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = highlightColor ?: MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ViewerActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color,
    tag: String,
    isHighlight: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHighlight) GoldenVault.copy(alpha = 0.15f) else Color.Transparent)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Text(
            text = label,
            color = if (isHighlight) GoldenVault else Color.White,
            fontSize = 10.sp,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
