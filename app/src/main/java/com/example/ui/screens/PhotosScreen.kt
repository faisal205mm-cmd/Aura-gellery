package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Collections
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.ui.components.MediaThumbnail
import com.example.ui.theme.GoldenVault
import com.example.ui.viewmodel.MediaFilter

@Composable
fun PhotosScreen(
    items: List<MediaItem>,
    isBangla: Boolean,
    activeFilter: MediaFilter,
    isMultiSelectMode: Boolean,
    selectedUris: Set<String>,
    onFilterChanged: (MediaFilter) -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    onMoveSelectedToSdCard: () -> Unit,
    onMoveSelectedToVault: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteSingleItem: (MediaItem) -> Unit,
    onMoveSingleToVault: (MediaItem) -> Unit,
    onMoveSingleToSdCard: (MediaItem) -> Unit,
    onClearMultiSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showBatchDeleteConfirm by remember { mutableStateOf(false) }
    var contextMenuItem by remember { mutableStateOf<MediaItem?>(null) }
    var singleItemToDelete by remember { mutableStateOf<MediaItem?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Multi-select Action Bar
        AnimatedVisibility(
            visible = isMultiSelectMode,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClearMultiSelect) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        Text(
                            text = if (isBangla) "${selectedUris.size} নির্বাচিত" else "${selectedUris.size} selected",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 2.dp)
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Batch Delete Button
                        Button(
                            onClick = { showBatchDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("batch_delete_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isBangla) "মুছুন" else "Delete",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Move to Vault Button
                        Button(
                            onClick = onMoveSelectedToVault,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenVault),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("batch_move_vault_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isBangla) "ভল্ট" else "Vault",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }

                        // Move to SD Card Button
                        Button(
                            onClick = onMoveSelectedToSdCard,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("batch_move_sdcard_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = if (isBangla) "এসডি" else "SD",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp)
                            )
                        }
                    }
                }
            }
        }

        // Filter Pills Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = activeFilter == MediaFilter.ALL,
                onClick = { onFilterChanged(MediaFilter.ALL) },
                label = { Text(if (isBangla) "সব মিডিয়া" else "All") },
                leadingIcon = { Icon(Icons.Default.Collections, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("filter_all")
            )
            FilterChip(
                selected = activeFilter == MediaFilter.PHOTOS_ONLY,
                onClick = { onFilterChanged(MediaFilter.PHOTOS_ONLY) },
                label = { Text(if (isBangla) "ছবি" else "Photos") },
                leadingIcon = { Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("filter_photos")
            )
            FilterChip(
                selected = activeFilter == MediaFilter.VIDEOS_ONLY,
                onClick = { onFilterChanged(MediaFilter.VIDEOS_ONLY) },
                label = { Text(if (isBangla) "ভিডিও" else "Videos") },
                leadingIcon = { Icon(Icons.Default.VideoLibrary, contentDescription = null, modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("filter_videos")
            )
            FilterChip(
                selected = activeFilter == MediaFilter.FAVORITES,
                onClick = { onFilterChanged(MediaFilter.FAVORITES) },
                label = { Text(if (isBangla) "পছন্দ" else "Favorites") },
                leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp)) },
                modifier = Modifier.testTag("filter_favorites")
            )
            FilterChip(
                selected = activeFilter == MediaFilter.SD_CARD,
                onClick = { onFilterChanged(MediaFilter.SD_CARD) },
                label = { Text(if (isBangla) "এসডি কার্ড" else "SD Card") },
                leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null, tint = GoldenVault, modifier = Modifier.size(16.dp)) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = GoldenVault.copy(alpha = 0.2f),
                    selectedLabelColor = GoldenVault
                ),
                modifier = Modifier.testTag("filter_sdcard")
            )
        }

        // Photos Grid or Empty State
        if (items.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Collections,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isBangla) "কোনো ছবি পাওয়া যায়নি" else "No photos found",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isBangla) "ফিল্টার পরিবর্তন করুন অথবা নতুন ছবি তুলুন" else "Change the filter or take new photos",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 4.dp, bottom = 80.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("photos_grid")
            ) {
                items(items, key = { it.uri }) { item ->
                    MediaThumbnail(
                        item = item,
                        isSelected = selectedUris.contains(item.uri),
                        isMultiSelectMode = isMultiSelectMode,
                        onClick = {
                            if (isMultiSelectMode) {
                                onItemLongClick(item)
                            } else {
                                onItemClick(item)
                            }
                        },
                        onLongClick = {
                            if (isMultiSelectMode) {
                                onItemLongClick(item)
                            } else {
                                // Open instant context menu on long press
                                contextMenuItem = item
                            }
                        }
                    )
                }
            }
        }
    }

    // Long Press Context Menu Dialog
    contextMenuItem?.let { item ->
        AlertDialog(
            onDismissRequest = { contextMenuItem = null },
            icon = {
                Icon(
                    imageVector = if (item.isVideo) Icons.Default.VideoLibrary else Icons.Default.Image,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = item.name,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Option 1: Delete
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                contextMenuItem = null
                                singleItemToDelete = item
                            }
                            .testTag("context_delete_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBangla) "ছবিটি মুছে ফেলুন (Delete)" else "Delete this media",
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // Option 2: Move to Vault
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = GoldenVault.copy(alpha = 0.15f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMoveSingleToVault(item)
                                contextMenuItem = null
                            }
                            .testTag("context_vault_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = GoldenVault,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBangla) "সিক্রেট ভল্টে লুকান (Move to Vault)" else "Hide in Secret Vault",
                                color = GoldenVault,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // Option 3: Move to SD Card / Internal
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onMoveSingleToSdCard(item)
                                contextMenuItem = null
                            }
                            .testTag("context_storage_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SdCard,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (item.storageType == StorageType.SD_CARD) {
                                    if (isBangla) "ফোন মেমোরিতে স্থানান্তর" else "Move to Internal Storage"
                                } else {
                                    if (isBangla) "এসডি কার্ডে স্থানান্তর (Move to SD)" else "Move to SD Card"
                                },
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // Option 4: Multi-select
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onItemLongClick(item)
                                contextMenuItem = null
                            }
                            .testTag("context_multiselect_option")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Checklist,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = if (isBangla) "একাধিক ছবি নির্বাচন করুন" else "Select multiple items",
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { contextMenuItem = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Single Item Delete Confirmation Dialog
    singleItemToDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { singleItemToDelete = null },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(if (isBangla) "ছবিটি মুছে ফেলতে চান?" else "Delete this media?")
            },
            text = {
                Text(
                    text = if (isBangla) "'${item.name}' ফাইলটি স্থায়ীভাবে মুছে ফেলা হবে। আপনি কি নিশ্চিত?"
                    else "File '${item.name}' will be permanently deleted. Are you sure?",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSingleItem(item)
                        singleItemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_single_delete_btn")
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { singleItemToDelete = null }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }

    // Batch Delete Confirmation Dialog
    if (showBatchDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showBatchDeleteConfirm = false },
            icon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = {
                Text(if (isBangla) "নির্বাচিত ফাইল মুছুন" else "Delete Selected Items")
            },
            text = {
                Text(
                    text = if (isBangla) "আপনি কি নিশ্চিতভাবে নির্বাচিত ${selectedUris.size} টি ফাইল মুছে ফেলতে চান? এটি আর ফিরিয়ে আনা যাবে না।"
                    else "Are you sure you want to permanently delete ${selectedUris.size} selected items? This cannot be undone.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteSelected()
                        showBatchDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_batch_delete_btn")
                ) {
                    Text(if (isBangla) "মুছে ফেলুন" else "Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showBatchDeleteConfirm = false }) {
                    Text(if (isBangla) "বাতিল" else "Cancel")
                }
            }
        )
    }
}
