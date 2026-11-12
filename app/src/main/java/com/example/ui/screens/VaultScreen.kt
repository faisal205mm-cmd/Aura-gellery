package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.MediaItem
import com.example.ui.components.MediaThumbnail
import com.example.ui.components.PinLockView
import com.example.ui.theme.GoldenVault

@Composable
fun VaultScreen(
    isVaultUnlocked: Boolean,
    vaultItems: List<MediaItem>,
    isBangla: Boolean,
    onVerifyPin: (String) -> Boolean,
    onUnlockSuccess: () -> Unit,
    onLockVault: () -> Unit,
    onResetPinWithAnswer: (String, String) -> Boolean,
    onOpenPinSetup: () -> Unit,
    onAddPhotosClick: () -> Unit,
    onMediaClick: (MediaItem) -> Unit,
    onMediaLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVaultUnlocked) {
        PinLockView(
            isBangla = isBangla,
            onPinSuccess = onUnlockSuccess,
            onVerifyPin = onVerifyPin,
            onResetPinWithAnswer = onResetPinWithAnswer,
            onOpenPinSetup = onOpenPinSetup,
            modifier = modifier
        )
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Golden Security Header
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = GoldenVault.copy(alpha = 0.12f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp)
                    .border(1.dp, GoldenVault.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(GoldenVault.copy(alpha = 0.2f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = GoldenVault,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = if (isBangla) "গোপন ভল্ট আনলকড" else "Private Vault Unlocked",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = GoldenVault
                            )
                            Text(
                                text = if (isBangla) "${vaultItems.size} টি সুরক্ষিত ফাইল" else "${vaultItems.size} protected items",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onAddPhotosClick,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("vault_add_photos_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isBangla) "যোগ করুন" else "Add",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }

                        Button(
                            onClick = onLockVault,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenVault),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.testTag("lock_vault_now_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isBangla) "লক" else "Lock",
                                color = Color.Black,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }
                }
            }

            if (vaultItems.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = GoldenVault.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (isBangla) "ভল্ট খালি আছে" else "Your Vault is Empty",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isBangla) "ছবি ভল্টে পাঠানোর ২টি সহজ উপায়:\n১. যেকোনো ছবি ওপেন করে নিচের 'ভল্টে পাঠান' বাটনে চাপুন।\n২. অথবা ছবি চেপে ধরে নির্বাচন করে উপরে 'ভল্ট' বাটনে চাপুন।"
                            else "2 easy ways to lock photos in vault:\n1. Open any photo and tap 'Lock in Vault' at the bottom.\n2. Long-press to select photos in gallery and tap 'Vault' above.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 8.dp),
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = onAddPhotosClick,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldenVault),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.testTag("vault_empty_add_photos_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = Color.Black)
                            Text(
                                text = if (isBangla) "গ্যালারি থেকে ছবি ভল্টে আনুন" else "Select Photos to Hide",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 80.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(vaultItems, key = { it.uri }) { item ->
                        MediaThumbnail(
                            item = item,
                            isSelected = false,
                            isMultiSelectMode = false,
                            onClick = { onMediaClick(item) },
                            onLongClick = { onMediaLongClick(item) }
                        )
                    }
                }
            }
        }
    }
}
