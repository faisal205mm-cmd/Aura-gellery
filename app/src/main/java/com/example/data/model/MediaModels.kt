package com.example.data.model

enum class StorageType {
    INTERNAL,
    SD_CARD
}

enum class BackupStatus {
    NOT_BACKED_UP,
    PENDING,
    SYNCED
}

data class MediaItem(
    val id: Long,
    val uri: String,
    val name: String,
    val path: String,
    val dateAdded: Long,
    val size: Long,
    val mimeType: String,
    val isVideo: Boolean = false,
    val duration: Long = 0L,
    val isFavorite: Boolean = false,
    val isVault: Boolean = false,
    val storageType: StorageType = StorageType.INTERNAL,
    val backupStatus: BackupStatus = BackupStatus.NOT_BACKED_UP,
    val albumName: String = "Camera",
    val width: Int = 1920,
    val height: Int = 1080
)

data class Album(
    val id: String,
    val name: String,
    val coverUri: String?,
    val count: Int,
    val storageType: StorageType = StorageType.INTERNAL,
    val isSystem: Boolean = false
)

data class BackupConfig(
    val isEnabled: Boolean = false,
    val provider: String = "Google Drive",
    val wifiOnly: Boolean = true,
    val backupWhenCharging: Boolean = false,
    val autoBackupIntervalHours: Int = 24,
    val lastBackupTimestamp: Long = 0L,
    val totalUploadedBytes: Long = 0L
)

data class StorageStats(
    val internalUsedBytes: Long,
    val internalTotalBytes: Long,
    val sdCardUsedBytes: Long,
    val sdCardTotalBytes: Long,
    val isSdCardAvailable: Boolean = true
)
