package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.Album
import com.example.data.model.BackupConfig
import com.example.data.model.MediaItem
import com.example.data.model.StorageStats
import com.example.data.model.StorageType
import com.example.data.repository.GalleryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GalleryTab {
    PHOTOS,
    ALBUMS,
    VAULT,
    STORAGE_BACKUP
}

enum class MediaFilter {
    ALL,
    PHOTOS_ONLY,
    VIDEOS_ONLY,
    FAVORITES,
    SD_CARD
}

class GalleryViewModel(application: Application) : AndroidViewModel(application) {

    val repository = GalleryRepository(application)

    private val _currentTab = MutableStateFlow(GalleryTab.PHOTOS)
    val currentTab: StateFlow<GalleryTab> = _currentTab.asStateFlow()

    private val _mediaFilter = MutableStateFlow(MediaFilter.ALL)
    val mediaFilter: StateFlow<MediaFilter> = _mediaFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedItemForDetail = MutableStateFlow<MediaItem?>(null)
    val selectedItemForDetail: StateFlow<MediaItem?> = _selectedItemForDetail.asStateFlow()

    private val _selectedAlbum = MutableStateFlow<Album?>(null)
    val selectedAlbum: StateFlow<Album?> = _selectedAlbum.asStateFlow()

    private val _multiSelectMode = MutableStateFlow(false)
    val multiSelectMode: StateFlow<Boolean> = _multiSelectMode.asStateFlow()

    private val _selectedMediaUris = MutableStateFlow<Set<String>>(emptySet())
    val selectedMediaUris: StateFlow<Set<String>> = _selectedMediaUris.asStateFlow()

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    private val _showPinSetupDialog = MutableStateFlow(false)
    val showPinSetupDialog: StateFlow<Boolean> = _showPinSetupDialog.asStateFlow()

    private val _showCreateAlbumDialog = MutableStateFlow(false)
    val showCreateAlbumDialog: StateFlow<Boolean> = _showCreateAlbumDialog.asStateFlow()

    private val _showAddToAlbumDialog = MutableStateFlow<MediaItem?>(null)
    val showAddToAlbumDialog: StateFlow<MediaItem?> = _showAddToAlbumDialog.asStateFlow()

    private val _showImportToVaultDialog = MutableStateFlow(false)
    val showImportToVaultDialog: StateFlow<Boolean> = _showImportToVaultDialog.asStateFlow()

    private val _selectedVideoForMxPlayer = MutableStateFlow<MediaItem?>(null)
    val selectedVideoForMxPlayer: StateFlow<MediaItem?> = _selectedVideoForMxPlayer.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    val isBangla: StateFlow<Boolean> = repository.isBangla
    val backupConfig: StateFlow<BackupConfig> = repository.backupConfig
    val storageStats: StateFlow<StorageStats> = repository.storageStats
    val backupProgress: StateFlow<Float?> = repository.backupProgress
    val albums: StateFlow<List<Album>> = repository.albums

    // Filtered Timeline Media (excludes items in Vault)
    val displayedTimelineMedia: StateFlow<List<MediaItem>> = combine(
        repository.mediaList,
        _mediaFilter,
        _searchQuery
    ) { allItems, filter, query ->
        allItems
            .filter { !it.isVault }
            .filter { item ->
                when (filter) {
                    MediaFilter.ALL -> true
                    MediaFilter.PHOTOS_ONLY -> !item.isVideo
                    MediaFilter.VIDEOS_ONLY -> item.isVideo
                    MediaFilter.FAVORITES -> item.isFavorite
                    MediaFilter.SD_CARD -> item.storageType == StorageType.SD_CARD
                }
            }
            .filter { item ->
                if (query.isBlank()) true
                else item.name.contains(query, ignoreCase = true) || item.albumName.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Media in Vault
    val vaultMedia: StateFlow<List<MediaItem>> = repository.mediaList.combine(_searchQuery) { all, query ->
        all.filter { it.isVault }
            .filter { item ->
                if (query.isBlank()) true else item.name.contains(query, ignoreCase = true)
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Media for the currently opened album
    val currentAlbumMedia: StateFlow<List<MediaItem>> = combine(
        repository.mediaList,
        _selectedAlbum
    ) { all, album ->
        if (album == null) emptyList()
        else all.filter { !it.isVault && it.albumName.equals(album.name, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setTab(tab: GalleryTab) {
        _currentTab.value = tab
        _selectedAlbum.value = null
        if (tab != GalleryTab.VAULT) {
            // lock vault when navigating away for maximum security
            _isVaultUnlocked.value = false
        }
    }

    fun setFilter(filter: MediaFilter) {
        _mediaFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectItemForDetail(item: MediaItem?) {
        _selectedItemForDetail.value = item
    }

    fun playVideoInMxPlayer(item: MediaItem?) {
        _selectedVideoForMxPlayer.value = item
    }

    fun handleMediaClick(item: MediaItem) {
        if (item.isVideo) {
            _selectedVideoForMxPlayer.value = item
        } else {
            _selectedItemForDetail.value = item
        }
    }

    fun openAlbum(album: Album) {
        _selectedAlbum.value = album
    }

    fun closeAlbum() {
        _selectedAlbum.value = null
    }

    fun toggleLanguage() {
        repository.toggleLanguage()
    }

    fun toggleMultiSelect(item: MediaItem) {
        val current = _selectedMediaUris.value.toMutableSet()
        if (current.contains(item.uri)) {
            current.remove(item.uri)
            if (current.isEmpty()) _multiSelectMode.value = false
        } else {
            current.add(item.uri)
            _multiSelectMode.value = true
        }
        _selectedMediaUris.value = current
    }

    fun clearMultiSelect() {
        _multiSelectMode.value = false
        _selectedMediaUris.value = emptySet()
    }

    fun toggleFavorite(item: MediaItem) {
        repository.toggleFavorite(item)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = _selectedItemForDetail.value?.copy(isFavorite = !item.isFavorite)
        }
    }

    fun moveToVault(item: MediaItem) {
        repository.moveToVault(item)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = null
        }
        showToast(if (isBangla.value) "সিক্রেট ভল্টে সরানো হয়েছে" else "Moved to Secret Vault")
    }

    fun restoreFromVault(item: MediaItem) {
        repository.restoreFromVault(item)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = null
        }
        showToast(if (isBangla.value) "ভল্ট থেকে ফিরিয়ে নেওয়া হয়েছে" else "Restored from Vault")
    }

    fun moveToSdCard(item: MediaItem) {
        repository.moveToStorage(item, StorageType.SD_CARD)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = _selectedItemForDetail.value?.copy(storageType = StorageType.SD_CARD)
        }
        showToast(if (isBangla.value) "এসডি কার্ডে স্থানান্তর সফল হয়েছে" else "Moved to SD Card successfully")
    }

    fun moveToInternal(item: MediaItem) {
        repository.moveToStorage(item, StorageType.INTERNAL)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = _selectedItemForDetail.value?.copy(storageType = StorageType.INTERNAL)
        }
        showToast(if (isBangla.value) "ইন্টারনাল মেমোরিতে স্থানান্তর সফল হয়েছে" else "Moved to Internal Storage")
    }

    fun moveSelectedToSdCard() {
        val selected = repository.mediaList.value.filter { _selectedMediaUris.value.contains(it.uri) }
        repository.moveMultipleToSdCard(selected)
        clearMultiSelect()
        showToast(if (isBangla.value) "${selected.size} টি ফাইল এসডি কার্ডে সরানো হয়েছে" else "${selected.size} files moved to SD Card")
    }

    fun moveSelectedToVault() {
        val selected = repository.mediaList.value.filter { _selectedMediaUris.value.contains(it.uri) }
        repository.moveMultipleToVault(selected)
        clearMultiSelect()
        showToast(if (isBangla.value) "${selected.size} টি ফাইল সিক্রেট ভল্টে স্থানান্তর হয়েছে" else "${selected.size} files secured in Secret Vault")
    }

    fun deleteMedia(item: MediaItem) {
        repository.deleteMedia(item)
        if (_selectedItemForDetail.value?.uri == item.uri) {
            _selectedItemForDetail.value = null
        }
        showToast(if (isBangla.value) "আইটেম মুছে ফেলা হয়েছে" else "Item deleted")
    }

    fun deleteSelectedMedia() {
        val selected = repository.mediaList.value.filter { _selectedMediaUris.value.contains(it.uri) }
        repository.deleteMultipleMedia(selected)
        clearMultiSelect()
        showToast(if (isBangla.value) "${selected.size} টি ফাইল মুছে ফেলা হয়েছে" else "${selected.size} items deleted")
    }

    fun createAlbum(name: String, storageType: StorageType) {
        repository.createAlbum(name, storageType)
        _showCreateAlbumDialog.value = false
        showToast(if (isBangla.value) "নতুন অ্যালবাম তৈরি করা হয়েছে" else "Album created: $name")
    }

    fun addMediaToAlbum(item: MediaItem, albumName: String) {
        repository.addMediaToAlbum(item, albumName)
        _showAddToAlbumDialog.value = null
        showToast(if (isBangla.value) "$albumName অ্যালবামে যোগ করা হয়েছে" else "Added to $albumName")
    }

    fun showCreateAlbumDialog(show: Boolean) {
        _showCreateAlbumDialog.value = show
    }

    fun showAddToAlbumDialog(item: MediaItem?) {
        _showAddToAlbumDialog.value = item
    }

    fun setShowImportToVaultDialog(show: Boolean) {
        _showImportToVaultDialog.value = show
    }

    fun unlockVault(pin: String): Boolean {
        val success = repository.verifyPin(pin)
        if (success) {
            _isVaultUnlocked.value = true
        }
        return success
    }

    fun unlockVaultDirectly() {
        _isVaultUnlocked.value = true
    }

    fun lockVault() {
        _isVaultUnlocked.value = false
    }

    fun setVaultPin(pin: String, answer: String) {
        repository.setVaultPin(pin, answer)
        _showPinSetupDialog.value = false
        showToast(if (isBangla.value) "নতুন পিন কোড সেট হয়েছে" else "PIN updated successfully")
    }

    fun resetPinWithAnswer(answer: String, newPin: String): Boolean {
        if (repository.verifySecurityAnswer(answer)) {
            repository.setVaultPin(newPin, answer)
            showToast(if (isBangla.value) "পিন কোড সফলভাবে পরিবর্তন হয়েছে" else "PIN reset successfully")
            return true
        }
        return false
    }

    fun showPinSetupDialog(show: Boolean) {
        _showPinSetupDialog.value = show
    }

    fun updateBackupConfig(config: BackupConfig) {
        repository.updateBackupConfig(config)
        showToast(if (isBangla.value) "ব্যাকআপ সেটিংস সংরক্ষিত হয়েছে" else "Backup settings updated")
    }

    fun triggerBackupNow() {
        viewModelScope.launch {
            repository.triggerBackupNow()
            showToast(if (isBangla.value) "ক্লাউড ব্যাকআপ সফলভাবে সম্পন্ন হয়েছে!" else "Cloud backup completed successfully!")
        }
    }

    fun refreshAllMedia() {
        viewModelScope.launch {
            repository.refreshMedia()
            showToast(if (isBangla.value) "মিডিয়া রিফ্রেশ করা হয়েছে" else "Media refreshed")
        }
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }
}
