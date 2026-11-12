package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BackupConfig
import com.example.data.model.MediaItem
import com.example.data.model.StorageStats
import com.example.data.model.StorageType
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.SoftEmerald
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageBackupScreen(
    storageStats: StorageStats,
    backupConfig: BackupConfig,
    backupProgress: Float?,
    allMedia: List<MediaItem>,
    isBangla: Boolean,
    onUpdateBackupConfig: (BackupConfig) -> Unit,
    onTriggerBackupNow: () -> Unit,
    onMoveAllToSdCard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val internalMediaCount = allMedia.count { it.storageType == StorageType.INTERNAL && !it.isVault }
    val sdCardMediaCount = allMedia.count { it.storageType == StorageType.SD_CARD && !it.isVault }

    var expandedProviderDropdown by remember { mutableStateOf(false) }
    val cloudProviders = listOf("Google Drive", "Microsoft OneDrive", "NextCloud / WebDAV", "Aura Secure Vault Cloud")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .padding(bottom = 80.dp)
    ) {
        // SECTION 1: SD CARD & INTERNAL STORAGE
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.SdCard,
                contentDescription = null,
                tint = GoldenVault,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isBangla) "এসডি কার্ড ও মেমোরি স্টোরেজ" else "SD Card & Storage Management",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = if (isBangla) "আপনার ফটো ও ভিডিওগুলো ইন্টারনাল ফোন মেমোরি অথবা এক্সটারনাল এসডি কার্ডে সংরক্ষণ করুন।" else "Manage your media across Internal device storage and Removable SD card.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Internal Storage Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = if (isBangla) "ইন্টারনাল ফোন মেমোরি" else "Internal Phone Storage",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isBangla) "$internalMediaCount টি মিডিয়া সংরক্ষিত" else "$internalMediaCount items stored",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%.1f GB / %.1f GB", storageStats.internalUsedBytes / 1e9, storageStats.internalTotalBytes / 1e9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                val internalFraction = (storageStats.internalUsedBytes.toFloat() / storageStats.internalTotalBytes.toFloat()).coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { internalFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }

        // SD Card Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(GoldenVault.copy(alpha = 0.18f))
                        ) {
                            Icon(Icons.Default.SdCard, contentDescription = null, tint = GoldenVault)
                        }
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (isBangla) "মাইক্রো এসডি কার্ড" else "Micro SD Card",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(start = 8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SoftEmerald.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isBangla) "সংযুক্ত" else "Mounted",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SoftEmerald
                                    )
                                }
                            }
                            Text(
                                text = if (isBangla) "$sdCardMediaCount টি মিডিয়া সংরক্ষিত" else "$sdCardMediaCount items stored",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Text(
                        text = String.format(Locale.US, "%.1f GB / %.1f GB", storageStats.sdCardUsedBytes / 1e9, storageStats.sdCardTotalBytes / 1e9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = GoldenVault
                    )
                }

                val sdFraction = (storageStats.sdCardUsedBytes.toFloat() / storageStats.sdCardTotalBytes.toFloat()).coerceIn(0f, 1f)
                Spacer(modifier = Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { sdFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = GoldenVault,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action to move all internal media to SD card
                Button(
                    onClick = onMoveAllToSdCard,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldenVault),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transfer_all_sdcard_btn")
                ) {
                    Icon(Icons.Default.DriveFileMove, contentDescription = null, tint = Color.Black)
                    Text(
                        text = if (isBangla) "সব ছবি এসডি কার্ডে স্থানান্তর করুন" else "Transfer Photos to SD Card",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // SECTION 2: AUTOMATED CLOUD BACKUP (OPTIONAL)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Cloud,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = if (isBangla) "স্বয়ংক্রিয় ক্লাউড ব্যাকআপ (ঐচ্ছিক)" else "Automated Cloud Backup (Optional)",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Text(
            text = if (isBangla) "অফলাইন-ফার্স্ট গ্যালারি। আপনার মেমোরি কার্ড ছাড়াও ক্লাউডে স্বয়ংক্রিয় নিরাপদ ব্যাকআপ রাখতে এটি ঐচ্ছিক সুবিধা হিসেবে চালু করতে পারেন।"
            else "Offline-first gallery. Keep an optional automated backup to cloud storage providers for extra safety.",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        // Master Toggle Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isBangla) "ক্লাউড ব্যাকআপ সক্রিয় করুন" else "Enable Cloud Backup",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = if (backupConfig.isEnabled) {
                                if (isBangla) "স্বয়ংক্রিয় ব্যাকআপ সক্রিয় রয়েছে" else "Automated sync is active"
                            } else {
                                if (isBangla) "নিষ্ক্রিয় (গ্যালারি সম্পূর্ণ অফলাইনে কাজ করছে)" else "Disabled (Operating 100% offline)"
                            },
                            fontSize = 12.sp,
                            color = if (backupConfig.isEnabled) SoftEmerald else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = backupConfig.isEnabled,
                        onCheckedChange = { onUpdateBackupConfig(backupConfig.copy(isEnabled = it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = SoftEmerald
                        ),
                        modifier = Modifier.testTag("cloud_backup_master_switch")
                    )
                }

                // Expanded Cloud Settings
                AnimatedVisibility(visible = backupConfig.isEnabled) {
                    Column(modifier = Modifier.padding(top = 16.dp)) {
                        // Cloud Provider Selector
                        Text(
                            text = if (isBangla) "ক্লাউড সেবা প্রদানকারী নির্বাচন করুন:" else "Select Cloud Service:",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        ExposedDropdownMenuBox(
                            expanded = expandedProviderDropdown,
                            onExpandedChange = { expandedProviderDropdown = it }
                        ) {
                            OutlinedTextField(
                                value = backupConfig.provider,
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedProviderDropdown) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            )
                            ExposedDropdownMenu(
                                expanded = expandedProviderDropdown,
                                onDismissRequest = { expandedProviderDropdown = false }
                            ) {
                                cloudProviders.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider) },
                                        onClick = {
                                            onUpdateBackupConfig(backupConfig.copy(provider = provider))
                                            expandedProviderDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Wi-Fi Only Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Wifi, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(
                                        text = if (isBangla) "শুধুমাত্র ওয়াইফাই (Wi-Fi Only)" else "Wi-Fi Only",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isBangla) "মোবাইল ডেটা খরচ বাঁচাবে" else "Save mobile cellular data",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = backupConfig.wifiOnly,
                                onCheckedChange = { onUpdateBackupConfig(backupConfig.copy(wifiOnly = it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Charging only Toggle
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp))
                                Column(modifier = Modifier.padding(start = 10.dp)) {
                                    Text(
                                        text = if (isBangla) "চার্জে থাকা অবস্থায় ব্যাকআপ" else "Backup When Charging",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (isBangla) "ব্যাটারির স্বাস্থ্য রক্ষা করতে" else "Preserve battery life",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = backupConfig.backupWhenCharging,
                                onCheckedChange = { onUpdateBackupConfig(backupConfig.copy(backupWhenCharging = it)) }
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Backup Progress Bar if actively running
                        if (backupProgress != null) {
                            Column(modifier = Modifier.padding(vertical = 8.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = if (isBangla) "ক্লাউডে ফাইল আপলোড হচ্ছে..." else "Uploading to ${backupConfig.provider}...",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${(backupProgress * 100).toInt()}%",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { backupProgress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Last backup stats
                        if (backupConfig.lastBackupTimestamp > 0) {
                            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
                            Text(
                                text = if (isBangla) "সর্বশেষ সফল ব্যাকআপ: ${sdf.format(Date(backupConfig.lastBackupTimestamp))}"
                                else "Last backup: ${sdf.format(Date(backupConfig.lastBackupTimestamp))}",
                                fontSize = 12.sp,
                                color = SoftEmerald,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Trigger Backup Button
                        Button(
                            onClick = onTriggerBackupNow,
                            enabled = backupProgress == null,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("backup_now_btn")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, tint = Color.White)
                            Text(
                                text = if (isBangla) "এখনই ক্লাউডে ব্যাকআপ নিন" else "Backup Now",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
