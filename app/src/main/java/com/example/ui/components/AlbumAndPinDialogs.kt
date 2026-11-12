package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Album
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.ui.theme.GoldenVault

@Composable
fun PinSetupDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onSavePin: (pin: String, answer: String) -> Unit
) {
    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var recoveryAnswer by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldenVault) },
        title = {
            Text(if (isBangla) "ভল্ট পিন পরিবর্তন করুন" else "Set Secret Vault PIN")
        },
        text = {
            Column {
                Text(
                    text = if (isBangla) "৪-সংখ্যার গোপন পিন নম্বর এবং রিকভারি উত্তর সেট করুন" else "Set 4-digit PIN and security answer for emergency recovery",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = newPin,
                    onValueChange = { if (it.length <= 4) newPin = it },
                    label = { Text(if (isBangla) "নতুন ৪-সংখ্যার পিন" else "New 4-Digit PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_new_pin_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 4) confirmPin = it },
                    label = { Text(if (isBangla) "পিন নিশ্চিত করুন" else "Confirm PIN") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_pin_input")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = recoveryAnswer,
                    onValueChange = { recoveryAnswer = it },
                    label = { Text(if (isBangla) "সিক্রেট রিকভারি শব্দ (যেমন: aura)" else "Security Secret Word (e.g. aura)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_recovery_answer_input")
                )
                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (newPin.length != 4) {
                        errorMessage = if (isBangla) "পিন অবশ্যই ৪ সংখ্যার হতে হবে" else "PIN must be 4 digits"
                        return@Button
                    }
                    if (newPin != confirmPin) {
                        errorMessage = if (isBangla) "উভয় পিন মিলছে না" else "PINs do not match"
                        return@Button
                    }
                    if (recoveryAnswer.isBlank()) {
                        errorMessage = if (isBangla) "একটি রিকভারি উত্তর লিখুন" else "Please provide security answer"
                        return@Button
                    }
                    onSavePin(newPin, recoveryAnswer)
                },
                colors = ButtonDefaults.buttonColors(containerColor = GoldenVault),
                modifier = Modifier.testTag("save_pin_btn")
            ) {
                Text(if (isBangla) "সংরক্ষণ করুন" else "Save PIN", color = Color.Black)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
fun CreateAlbumDialog(
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onCreateAlbum: (name: String, storageType: StorageType) -> Unit
) {
    var albumName by remember { mutableStateOf("") }
    var selectedStorage by remember { mutableStateOf(StorageType.INTERNAL) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CreateNewFolder, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = {
            Text(if (isBangla) "নতুন অ্যালবাম তৈরি করুন" else "Create New Album")
        },
        text = {
            Column {
                OutlinedTextField(
                    value = albumName,
                    onValueChange = { albumName = it },
                    label = { Text(if (isBangla) "অ্যালবামের নাম" else "Album Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("album_name_input")
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isBangla) "স্টোরেজ লোকেশন নির্বাচন করুন:" else "Target Storage Location:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FilterChip(
                        selected = selectedStorage == StorageType.INTERNAL,
                        onClick = { selectedStorage = StorageType.INTERNAL },
                        label = { Text(if (isBangla) "ইন্টারনাল" else "Internal") },
                        leadingIcon = { Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors()
                    )

                    FilterChip(
                        selected = selectedStorage == StorageType.SD_CARD,
                        onClick = { selectedStorage = StorageType.SD_CARD },
                        label = { Text(if (isBangla) "এসডি কার্ড" else "SD Card") },
                        leadingIcon = { Icon(Icons.Default.SdCard, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = GoldenVault.copy(alpha = 0.25f),
                            selectedLabelColor = GoldenVault
                        )
                    )
                }

                if (errorText != null) {
                    Text(
                        text = errorText!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (albumName.trim().isEmpty()) {
                        errorText = if (isBangla) "অ্যালবামের একটি নাম দিন" else "Please enter album name"
                        return@Button
                    }
                    onCreateAlbum(albumName.trim(), selectedStorage)
                },
                modifier = Modifier.testTag("create_album_confirm_btn")
            ) {
                Text(if (isBangla) "তৈরি করুন" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বাতিল" else "Cancel")
            }
        }
    )
}

@Composable
fun AddToAlbumDialog(
    item: MediaItem,
    albums: List<Album>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onAlbumSelected: (albumName: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isBangla) "অ্যালবামে যোগ করুন" else "Add to Album")
        },
        text = {
            Column {
                Text(
                    text = if (isBangla) "কোন অ্যালবামে ছবিটি রাখতে চান নির্বাচন করুন:" else "Select target album for this photo:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.height(200.dp)
                ) {
                    items(albums) { album ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onAlbumSelected(album.name) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Folder,
                                    contentDescription = null,
                                    tint = if (album.storageType == StorageType.SD_CARD) GoldenVault else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(
                                        text = album.name,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = if (album.storageType == StorageType.SD_CARD) "SD Card" else "Internal",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বন্ধ করুন" else "Close")
            }
        }
    )
}

@Composable
fun ImportToVaultDialog(
    availableMedia: List<MediaItem>,
    isBangla: Boolean,
    onDismiss: () -> Unit,
    onMediaSelected: (MediaItem) -> Unit
) {
    val nonVaultItems = availableMedia.filter { !it.isVault }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldenVault) },
        title = {
            Text(if (isBangla) "ভল্টে ছবি লুকান" else "Hide Photos in Vault")
        },
        text = {
            Column {
                Text(
                    text = if (isBangla) "যে ছবিটি ভল্টে গোপন রাখতে চান তার উপর ট্যাপ করুন:" else "Tap a photo to securely lock and hide it:",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))

                if (nonVaultItems.isEmpty()) {
                    Text(
                        text = if (isBangla) "গ্যালারিতে কোনো অতিরিক্ত ছবি নেই" else "No photos available to hide",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.height(260.dp)
                    ) {
                        items(nonVaultItems) { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onMediaSelected(item) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = null,
                                        tint = GoldenVault,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column(modifier = Modifier.padding(start = 10.dp)) {
                                        Text(
                                            text = item.name,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "${item.albumName} • ${if (item.storageType == StorageType.SD_CARD) "SD Card" else "Internal"}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isBangla) "বন্ধ করুন" else "Close")
            }
        }
    )
}
