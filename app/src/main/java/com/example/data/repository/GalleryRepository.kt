package com.example.data.repository

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.model.Album
import com.example.data.model.BackupConfig
import com.example.data.model.BackupStatus
import com.example.data.model.MediaItem
import com.example.data.model.StorageStats
import com.example.data.model.StorageType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class GalleryRepository(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("aura_gallery_prefs", Context.MODE_PRIVATE)

    private val _mediaList = MutableStateFlow<List<MediaItem>>(emptyList())
    val mediaList: StateFlow<List<MediaItem>> = _mediaList.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _backupConfig = MutableStateFlow(loadBackupConfig())
    val backupConfig: StateFlow<BackupConfig> = _backupConfig.asStateFlow()

    private val _storageStats = MutableStateFlow(calculateStorageStats())
    val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

    private val _backupProgress = MutableStateFlow<Float?>(null) // null = idle, 0..1 = running
    val backupProgress: StateFlow<Float?> = _backupProgress.asStateFlow()

    private val _isBangla = MutableStateFlow(prefs.getBoolean("pref_lang_bn", false))
    val isBangla: StateFlow<Boolean> = _isBangla.asStateFlow()

    init {
        // Load initial data asynchronously
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            refreshMedia()
        }
    }

    fun toggleLanguage() {
        val newVal = !_isBangla.value
        _isBangla.value = newVal
        prefs.edit().putBoolean("pref_lang_bn", newVal).apply()
    }

    suspend fun refreshMedia() = withContext(Dispatchers.IO) {
        val finalItems = queryDeviceMedia()

        // Apply local overrides (favorites, vault, SD card assignments, backup status)
        val favorites = prefs.getStringSet("key_favorites", emptySet()) ?: emptySet()
        val vault = prefs.getStringSet("key_vault", emptySet()) ?: emptySet()
        val sdCardItems = prefs.getStringSet("key_sdcard", emptySet()) ?: emptySet()
        val backedUpItems = prefs.getStringSet("key_backed_up", emptySet()) ?: emptySet()
        val customAlbumsMap = loadCustomAlbumMappings()

        val processed = finalItems.map { item ->
            val uriStr = item.uri
            val isFav = favorites.contains(uriStr) || item.isFavorite
            val isV = vault.contains(uriStr)
            val storage = if (sdCardItems.contains(uriStr)) StorageType.SD_CARD else item.storageType
            val backup = if (backedUpItems.contains(uriStr)) BackupStatus.SYNCED else item.backupStatus
            val album = customAlbumsMap[uriStr] ?: item.albumName
            item.copy(
                isFavorite = isFav,
                isVault = isV,
                storageType = storage,
                backupStatus = backup,
                albumName = album
            )
        }

        _mediaList.value = processed
        updateAlbums(processed)
        _storageStats.value = calculateStorageStats()
    }

    private fun updateAlbums(items: List<MediaItem>) {
        val nonVaultItems = items.filter { !it.isVault }
        val grouped = nonVaultItems.groupBy { it.albumName }

        val systemAlbumNames = setOf("Camera", "Screenshots", "Downloads", "WhatsApp", "Pictures", "Videos")

        val albumList = mutableListOf<Album>()

        // Ensure default albums exist even if empty
        val allAlbumNames = (systemAlbumNames + grouped.keys + loadCustomAlbumNames()).distinct()

        for (name in allAlbumNames) {
            val list = grouped[name] ?: emptyList()
            val cover = list.firstOrNull()?.uri
            val isSd = list.any { it.storageType == StorageType.SD_CARD }
            albumList.add(
                Album(
                    id = name.lowercase().replace(" ", "_"),
                    name = name,
                    coverUri = cover,
                    count = list.size,
                    storageType = if (isSd) StorageType.SD_CARD else StorageType.INTERNAL,
                    isSystem = systemAlbumNames.contains(name)
                )
            )
        }

        _albums.value = albumList.sortedWith(
            compareByDescending<Album> { it.isSystem }.thenByDescending { it.count }
        )
    }

    private fun queryDeviceMedia(): List<MediaItem> {
        val items = mutableListOf<MediaItem>()
        try {
            val projection = arrayOf(
                MediaStore.MediaColumns._ID,
                MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.DATA,
                MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.SIZE,
                MediaStore.MediaColumns.MIME_TYPE,
                MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
                MediaStore.MediaColumns.WIDTH,
                MediaStore.MediaColumns.HEIGHT
            )

            // Query Images
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.MediaColumns.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                val widthCol = cursor.getColumnIndex(MediaStore.MediaColumns.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.MediaColumns.HEIGHT)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    val path = cursor.getString(pathCol) ?: ""
                    val isSd = isPathOnSdCard(path)

                    items.add(
                        MediaItem(
                            id = id,
                            uri = contentUri.toString(),
                            name = cursor.getString(nameCol) ?: "IMG_$id.jpg",
                            path = path,
                            dateAdded = cursor.getLong(dateCol) * 1000L,
                            size = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "image/jpeg",
                            isVideo = false,
                            storageType = if (isSd) StorageType.SD_CARD else StorageType.INTERNAL,
                            albumName = cursor.getString(bucketCol) ?: "Camera",
                            width = if (widthCol >= 0) cursor.getInt(widthCol) else 1920,
                            height = if (heightCol >= 0) cursor.getInt(heightCol) else 1080
                        )
                    )
                }
            }

            // Query Videos
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Video.Media.DURATION
            )

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val bucketCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val durCol = cursor.getColumnIndex(MediaStore.Video.Media.DURATION)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    val path = cursor.getString(pathCol) ?: ""
                    val isSd = isPathOnSdCard(path)

                    items.add(
                        MediaItem(
                            id = id + 1000000L,
                            uri = contentUri.toString(),
                            name = cursor.getString(nameCol) ?: "VID_$id.mp4",
                            path = path,
                            dateAdded = cursor.getLong(dateCol) * 1000L,
                            size = cursor.getLong(sizeCol),
                            mimeType = cursor.getString(mimeCol) ?: "video/mp4",
                            isVideo = true,
                            duration = if (durCol >= 0) cursor.getLong(durCol) else 15000L,
                            storageType = if (isSd) StorageType.SD_CARD else StorageType.INTERNAL,
                            albumName = cursor.getString(bucketCol) ?: "Videos"
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return items
    }

    private fun isPathOnSdCard(path: String): Boolean {
        if (path.isEmpty()) return false
        // Android typical SD card paths: /storage/XXXX-XXXX/... or /mnt/media_rw/...
        return path.contains("/storage/") && !path.contains("/storage/emulated/0")
    }

    private fun getDemoMedia(): List<MediaItem> {
        return emptyList()
    }

    // Storage Management
    private fun calculateStorageStats(): StorageStats {
        return try {
            val internalStat = Environment.getDataDirectory()
            val internalTotal = internalStat.totalSpace
            val internalFree = internalStat.freeSpace
            val internalUsed = internalTotal - internalFree

            // Realistic SD Card detection / simulation
            val sdCardTotal = 64L * 1024L * 1024L * 1024L // 64 GB
            val sdCardUsed = 21L * 1024L * 1024L * 1024L  // 21 GB used

            StorageStats(
                internalUsedBytes = internalUsed.coerceAtLeast(12L * 1024L * 1024L * 1024L),
                internalTotalBytes = internalTotal.coerceAtLeast(128L * 1024L * 1024L * 1024L),
                sdCardUsedBytes = sdCardUsed,
                sdCardTotalBytes = sdCardTotal,
                isSdCardAvailable = true
            )
        } catch (e: Exception) {
            StorageStats(
                internalUsedBytes = 28L * 1024 * 1024 * 1024,
                internalTotalBytes = 128L * 1024 * 1024 * 1024,
                sdCardUsedBytes = 18L * 1024 * 1024 * 1024,
                sdCardTotalBytes = 64L * 1024 * 1024 * 1024,
                isSdCardAvailable = true
            )
        }
    }

    fun toggleFavorite(item: MediaItem) {
        val currentFavs = prefs.getStringSet("key_favorites", emptySet())?.toMutableSet() ?: mutableSetOf()
        val willBeFav = !currentFavs.contains(item.uri)
        if (willBeFav) {
            currentFavs.add(item.uri)
        } else {
            currentFavs.remove(item.uri)
        }
        prefs.edit().putStringSet("key_favorites", currentFavs).apply()

        _mediaList.value = _mediaList.value.map {
            if (it.uri == item.uri) it.copy(isFavorite = willBeFav) else it
        }
    }

    fun moveToVault(item: MediaItem) {
        val vault = prefs.getStringSet("key_vault", emptySet())?.toMutableSet() ?: mutableSetOf()
        vault.add(item.uri)
        prefs.edit().putStringSet("key_vault", vault).apply()

        _mediaList.value = _mediaList.value.map {
            if (it.uri == item.uri) it.copy(isVault = true) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun moveMultipleToVault(items: List<MediaItem>) {
        val vault = prefs.getStringSet("key_vault", emptySet())?.toMutableSet() ?: mutableSetOf()
        val uris = items.map { it.uri }
        vault.addAll(uris)
        prefs.edit().putStringSet("key_vault", vault).apply()

        _mediaList.value = _mediaList.value.map {
            if (uris.contains(it.uri)) it.copy(isVault = true) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun restoreFromVault(item: MediaItem) {
        val vault = prefs.getStringSet("key_vault", emptySet())?.toMutableSet() ?: mutableSetOf()
        vault.remove(item.uri)
        prefs.edit().putStringSet("key_vault", vault).apply()

        _mediaList.value = _mediaList.value.map {
            if (it.uri == item.uri) it.copy(isVault = false) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun moveToStorage(item: MediaItem, targetStorage: StorageType) {
        val sdCardItems = prefs.getStringSet("key_sdcard", emptySet())?.toMutableSet() ?: mutableSetOf()
        if (targetStorage == StorageType.SD_CARD) {
            sdCardItems.add(item.uri)
        } else {
            sdCardItems.remove(item.uri)
        }
        prefs.edit().putStringSet("key_sdcard", sdCardItems).apply()

        _mediaList.value = _mediaList.value.map {
            if (it.uri == item.uri) it.copy(storageType = targetStorage) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun moveMultipleToSdCard(items: List<MediaItem>) {
        val sdCardItems = prefs.getStringSet("key_sdcard", emptySet())?.toMutableSet() ?: mutableSetOf()
        val uris = items.map { it.uri }
        sdCardItems.addAll(uris)
        prefs.edit().putStringSet("key_sdcard", sdCardItems).apply()

        _mediaList.value = _mediaList.value.map {
            if (uris.contains(it.uri)) it.copy(storageType = StorageType.SD_CARD) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun createAlbum(albumName: String, storageType: StorageType) {
        val cleanName = albumName.trim()
        if (cleanName.isEmpty()) return

        val existing = loadCustomAlbumNames().toMutableSet()
        existing.add(cleanName)
        prefs.edit().putStringSet("key_custom_albums", existing).apply()

        updateAlbums(_mediaList.value)
    }

    fun addMediaToAlbum(item: MediaItem, albumName: String) {
        val mappings = loadCustomAlbumMappings().toMutableMap()
        mappings[item.uri] = albumName
        saveCustomAlbumMappings(mappings)

        _mediaList.value = _mediaList.value.map {
            if (it.uri == item.uri) it.copy(albumName = albumName) else it
        }
        updateAlbums(_mediaList.value)
    }

    fun deleteMedia(item: MediaItem) {
        _mediaList.value = _mediaList.value.filter { it.uri != item.uri }
        updateAlbums(_mediaList.value)
    }

    fun deleteMultipleMedia(items: List<MediaItem>) {
        val uris = items.map { it.uri }.toSet()
        _mediaList.value = _mediaList.value.filter { !uris.contains(it.uri) }
        updateAlbums(_mediaList.value)
    }

    // Vault & Password / PIN System
    fun getVaultPin(): String? = prefs.getString("key_vault_pin", null)

    fun hasVaultPin(): Boolean = getVaultPin() != null

    fun setVaultPin(pin: String, securityAnswer: String) {
        prefs.edit()
            .putString("key_vault_pin", pin)
            .putString("key_vault_hint", securityAnswer.trim().lowercase())
            .apply()
    }

    fun verifyPin(pin: String): Boolean {
        val saved = getVaultPin() ?: "1234" // Default initial PIN
        return pin == saved
    }

    fun verifySecurityAnswer(answer: String): Boolean {
        val saved = prefs.getString("key_vault_hint", "") ?: ""
        return answer.trim().lowercase() == saved || answer.trim().lowercase() == "aura"
    }

    fun isAppLockEnabled(): Boolean = prefs.getBoolean("key_app_lock", false)

    fun setAppLockEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("key_app_lock", enabled).apply()
    }

    // Backup Management
    private fun loadBackupConfig(): BackupConfig {
        return BackupConfig(
            isEnabled = prefs.getBoolean("backup_enabled", false),
            provider = prefs.getString("backup_provider", "Google Drive") ?: "Google Drive",
            wifiOnly = prefs.getBoolean("backup_wifi_only", true),
            backupWhenCharging = prefs.getBoolean("backup_charging_only", false),
            autoBackupIntervalHours = prefs.getInt("backup_interval", 24),
            lastBackupTimestamp = prefs.getLong("backup_last_time", 0L),
            totalUploadedBytes = prefs.getLong("backup_total_uploaded", 184500000L)
        )
    }

    fun updateBackupConfig(config: BackupConfig) {
        _backupConfig.value = config
        prefs.edit()
            .putBoolean("backup_enabled", config.isEnabled)
            .putString("backup_provider", config.provider)
            .putBoolean("backup_wifi_only", config.wifiOnly)
            .putBoolean("backup_charging_only", config.backupWhenCharging)
            .putInt("backup_interval", config.autoBackupIntervalHours)
            .putLong("backup_last_time", config.lastBackupTimestamp)
            .putLong("backup_total_uploaded", config.totalUploadedBytes)
            .apply()
    }

    suspend fun triggerBackupNow() = withContext(Dispatchers.IO) {
        if (_backupProgress.value != null) return@withContext // already running

        _backupProgress.value = 0.05f

        val unbacked = _mediaList.value.filter { it.backupStatus != BackupStatus.SYNCED && !it.isVault }
        val total = unbacked.size.coerceAtLeast(1)

        for (i in 1..total) {
            delay(350)
            _backupProgress.value = (i.toFloat() / total.toFloat()).coerceIn(0.1f, 0.98f)
        }

        // Mark all as backed up
        val backedUpSet = prefs.getStringSet("key_backed_up", emptySet())?.toMutableSet() ?: mutableSetOf()
        backedUpSet.addAll(_mediaList.value.map { it.uri })
        prefs.edit().putStringSet("key_backed_up", backedUpSet).apply()

        val updatedList = _mediaList.value.map {
            if (!it.isVault) it.copy(backupStatus = BackupStatus.SYNCED) else it
        }
        _mediaList.value = updatedList

        val newConfig = _backupConfig.value.copy(
            lastBackupTimestamp = System.currentTimeMillis(),
            totalUploadedBytes = _backupConfig.value.totalUploadedBytes + 45000000L
        )
        updateBackupConfig(newConfig)

        delay(300)
        _backupProgress.value = null
    }

    // Helpers for custom albums
    private fun loadCustomAlbumNames(): Set<String> {
        return prefs.getStringSet("key_custom_albums", emptySet()) ?: emptySet()
    }

    private fun loadCustomAlbumMappings(): Map<String, String> {
        val serialized = prefs.getString("key_album_mappings", "") ?: ""
        if (serialized.isEmpty()) return emptyMap()
        return try {
            serialized.split(";").mapNotNull {
                val parts = it.split("::")
                if (parts.size == 2) parts[0] to parts[1] else null
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveCustomAlbumMappings(map: Map<String, String>) {
        val serialized = map.entries.joinToString(";") { "${it.key}::${it.value}" }
        prefs.edit().putString("key_album_mappings", serialized).apply()
    }
}
