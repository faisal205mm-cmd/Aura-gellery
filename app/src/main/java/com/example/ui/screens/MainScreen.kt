package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.StorageType
import com.example.ui.components.AddToAlbumDialog
import com.example.ui.components.CreateAlbumDialog
import com.example.ui.components.ImportToVaultDialog
import com.example.ui.components.MediaViewerDialog
import com.example.ui.components.MxVideoPlayerDialog
import com.example.ui.components.PinSetupDialog
import com.example.ui.theme.GoldenVault
import com.example.ui.viewmodel.GalleryTab
import com.example.ui.viewmodel.GalleryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GalleryViewModel) {
    val context = LocalContext.current

    val currentTab by viewModel.currentTab.collectAsState()
    val isBangla by viewModel.isBangla.collectAsState()
    val mediaFilter by viewModel.mediaFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedItem by viewModel.selectedItemForDetail.collectAsState()
    val selectedAlbum by viewModel.selectedAlbum.collectAsState()
    val multiSelectMode by viewModel.multiSelectMode.collectAsState()
    val selectedUris by viewModel.selectedMediaUris.collectAsState()
    val isVaultUnlocked by viewModel.isVaultUnlocked.collectAsState()
    val showPinSetup by viewModel.showPinSetupDialog.collectAsState()
    val showCreateAlbum by viewModel.showCreateAlbumDialog.collectAsState()
    val showAddToAlbum by viewModel.showAddToAlbumDialog.collectAsState()
    val showImportToVault by viewModel.showImportToVaultDialog.collectAsState()
    val selectedVideoForMxPlayer by viewModel.selectedVideoForMxPlayer.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()

    val timelineMedia by viewModel.displayedTimelineMedia.collectAsState()
    val vaultMedia by viewModel.vaultMedia.collectAsState()
    val albums by viewModel.albums.collectAsState()
    val currentAlbumMedia by viewModel.currentAlbumMedia.collectAsState()
    val storageStats by viewModel.storageStats.collectAsState()
    val backupConfig by viewModel.backupConfig.collectAsState()
    val backupProgress by viewModel.backupProgress.collectAsState()

    var isSearchActive by remember { mutableStateOf(false) }

    // Toast listener
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    // Back handling for album detail and search
    BackHandler(enabled = selectedAlbum != null || isSearchActive || multiSelectMode) {
        when {
            selectedAlbum != null -> viewModel.closeAlbum()
            isSearchActive -> {
                isSearchActive = false
                viewModel.setSearchQuery("")
            }
            multiSelectMode -> viewModel.clearMultiSelect()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (isSearchActive) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.setSearchQuery(it) },
                            placeholder = { Text(if (isBangla) "ছবি বা অ্যালবাম খুঁজুন..." else "Search photos or albums...") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = {
                                    isSearchActive = false
                                    viewModel.setSearchQuery("")
                                }) {
                                    Icon(Icons.Default.Close, contentDescription = "Close search")
                                }
                            },
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_input")
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = when (currentTab) {
                                    GalleryTab.PHOTOS -> if (isBangla) "অরা গ্যালারি" else "Aura Gallery"
                                    GalleryTab.ALBUMS -> if (isBangla) "অ্যালবাম সমূহ" else "Albums"
                                    GalleryTab.VAULT -> if (isBangla) "সিক্রেট ভল্ট" else "Private Vault"
                                    GalleryTab.STORAGE_BACKUP -> if (isBangla) "স্টোরেজ ও ব্যাকআপ" else "Storage & Backup"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = if (currentTab == GalleryTab.VAULT) GoldenVault else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                },
                actions = {
                    if (!isSearchActive && currentTab != GalleryTab.VAULT && currentTab != GalleryTab.STORAGE_BACKUP) {
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.testTag("topbar_search_btn")
                        ) {
                            Icon(Icons.Default.Search, contentDescription = "Search")
                        }
                    }

                    // Language Toggle (EN / বাংলা)
                    IconButton(
                        onClick = { viewModel.toggleLanguage() },
                        modifier = Modifier.testTag("topbar_lang_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Language, contentDescription = "Language", modifier = Modifier.size(15.dp))
                            Text(
                                text = if (isBangla) "বাং" else "EN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 2.dp)
                            )
                        }
                    }

                    // Refresh Button
                    IconButton(
                        onClick = { viewModel.refreshAllMedia() },
                        modifier = Modifier.testTag("topbar_refresh_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .testTag("main_bottom_nav")
            ) {
                // Tab 1: Photos
                NavigationBarItem(
                    selected = currentTab == GalleryTab.PHOTOS,
                    onClick = { viewModel.setTab(GalleryTab.PHOTOS) },
                    icon = { Icon(Icons.Default.PhotoLibrary, contentDescription = "Photos") },
                    label = { Text(if (isBangla) "ছবি" else "Photos") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_photos")
                )

                // Tab 2: Albums
                NavigationBarItem(
                    selected = currentTab == GalleryTab.ALBUMS,
                    onClick = { viewModel.setTab(GalleryTab.ALBUMS) },
                    icon = { Icon(Icons.Default.FolderSpecial, contentDescription = "Albums") },
                    label = { Text(if (isBangla) "অ্যালবাম" else "Albums") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_albums")
                )

                // Tab 3: Vault
                NavigationBarItem(
                    selected = currentTab == GalleryTab.VAULT,
                    onClick = { viewModel.setTab(GalleryTab.VAULT) },
                    icon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Vault",
                            tint = if (currentTab == GalleryTab.VAULT) GoldenVault else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    label = {
                        Text(
                            text = if (isBangla) "ভল্ট" else "Vault",
                            color = if (currentTab == GalleryTab.VAULT) GoldenVault else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = GoldenVault,
                        indicatorColor = GoldenVault.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier.testTag("nav_tab_vault")
                )

                // Tab 4: Storage & Backup
                NavigationBarItem(
                    selected = currentTab == GalleryTab.STORAGE_BACKUP,
                    onClick = { viewModel.setTab(GalleryTab.STORAGE_BACKUP) },
                    icon = { Icon(Icons.Default.SdCard, contentDescription = "Storage") },
                    label = { Text(if (isBangla) "স্টোরেজ" else "Storage") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_tab_storage")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentTab) {
                GalleryTab.PHOTOS -> {
                    PhotosScreen(
                        items = timelineMedia,
                        isBangla = isBangla,
                        activeFilter = mediaFilter,
                        isMultiSelectMode = multiSelectMode,
                        selectedUris = selectedUris,
                        onFilterChanged = { viewModel.setFilter(it) },
                        onItemClick = { viewModel.handleMediaClick(it) },
                        onItemLongClick = { viewModel.toggleMultiSelect(it) },
                        onMoveSelectedToSdCard = { viewModel.moveSelectedToSdCard() },
                        onMoveSelectedToVault = { viewModel.moveSelectedToVault() },
                        onDeleteSelected = { viewModel.deleteSelectedMedia() },
                        onDeleteSingleItem = { viewModel.deleteMedia(it) },
                        onMoveSingleToVault = { viewModel.moveToVault(it) },
                        onMoveSingleToSdCard = { itm ->
                            if (itm.storageType == StorageType.SD_CARD) viewModel.moveToInternal(itm)
                            else viewModel.moveToSdCard(itm)
                        },
                        onClearMultiSelect = { viewModel.clearMultiSelect() }
                    )
                }

                GalleryTab.ALBUMS -> {
                    AlbumsScreen(
                        albums = albums,
                        selectedAlbum = selectedAlbum,
                        albumMedia = currentAlbumMedia,
                        isBangla = isBangla,
                        onAlbumClick = { viewModel.openAlbum(it) },
                        onCloseAlbum = { viewModel.closeAlbum() },
                        onMediaClick = { viewModel.handleMediaClick(it) },
                        onMediaLongClick = { viewModel.toggleMultiSelect(it) },
                        onCreateAlbumClick = { viewModel.showCreateAlbumDialog(true) }
                    )
                }

                GalleryTab.VAULT -> {
                    VaultScreen(
                        isVaultUnlocked = isVaultUnlocked,
                        vaultItems = vaultMedia,
                        isBangla = isBangla,
                        onVerifyPin = { viewModel.unlockVault(it) },
                        onUnlockSuccess = { viewModel.unlockVaultDirectly() },
                        onLockVault = { viewModel.lockVault() },
                        onResetPinWithAnswer = { ans, newPin -> viewModel.resetPinWithAnswer(ans, newPin) },
                        onOpenPinSetup = { viewModel.showPinSetupDialog(true) },
                        onAddPhotosClick = { viewModel.setShowImportToVaultDialog(true) },
                        onMediaClick = { viewModel.handleMediaClick(it) },
                        onMediaLongClick = { /* No-op in vault */ }
                    )
                }

                GalleryTab.STORAGE_BACKUP -> {
                    StorageBackupScreen(
                        storageStats = storageStats,
                        backupConfig = backupConfig,
                        backupProgress = backupProgress,
                        allMedia = timelineMedia,
                        isBangla = isBangla,
                        onUpdateBackupConfig = { viewModel.updateBackupConfig(it) },
                        onTriggerBackupNow = { viewModel.triggerBackupNow() },
                        onMoveAllToSdCard = {
                            val internalItems = timelineMedia.filter { it.storageType == StorageType.INTERNAL }
                            viewModel.repository.moveMultipleToSdCard(internalItems)
                            viewModel.showToast(if (isBangla) "${internalItems.size} টি ফাইল এসডি কার্ডে সফলভাবে সরানো হয়েছে" else "${internalItems.size} files moved to SD Card")
                        }
                    )
                }
            }
        }

        // Full Screen Media Viewer Dialog
        selectedItem?.let { item ->
            MediaViewerDialog(
                item = item,
                isBangla = isBangla,
                onDismiss = { viewModel.selectItemForDetail(null) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onMoveToStorage = { itm, target ->
                    if (target == StorageType.SD_CARD) viewModel.moveToSdCard(itm)
                    else viewModel.moveToInternal(itm)
                },
                onMoveToVault = { viewModel.moveToVault(it) },
                onRestoreFromVault = { viewModel.restoreFromVault(it) },
                onAddToAlbum = { viewModel.showAddToAlbumDialog(it) },
                onDelete = { viewModel.deleteMedia(it) },
                onPlayVideoInMxPlayer = { viewModel.playVideoInMxPlayer(it) }
            )
        }

        // MX Player High Quality Video Player Dialog
        selectedVideoForMxPlayer?.let { videoItem ->
            MxVideoPlayerDialog(
                item = videoItem,
                isBangla = isBangla,
                onDismiss = { viewModel.playVideoInMxPlayer(null) }
            )
        }

        // PIN Setup Dialog
        if (showPinSetup) {
            PinSetupDialog(
                isBangla = isBangla,
                onDismiss = { viewModel.showPinSetupDialog(false) },
                onSavePin = { pin, answer -> viewModel.setVaultPin(pin, answer) }
            )
        }

        // Create Album Dialog
        if (showCreateAlbum) {
            CreateAlbumDialog(
                isBangla = isBangla,
                onDismiss = { viewModel.showCreateAlbumDialog(false) },
                onCreateAlbum = { name, storage -> viewModel.createAlbum(name, storage) }
            )
        }

        // Add To Album Dialog
        showAddToAlbum?.let { item ->
            AddToAlbumDialog(
                item = item,
                albums = albums,
                isBangla = isBangla,
                onDismiss = { viewModel.showAddToAlbumDialog(null) },
                onAlbumSelected = { albumName -> viewModel.addMediaToAlbum(item, albumName) }
            )
        }

        // Import To Vault Dialog
        if (showImportToVault) {
            ImportToVaultDialog(
                availableMedia = timelineMedia,
                isBangla = isBangla,
                onDismiss = { viewModel.setShowImportToVaultDialog(false) },
                onMediaSelected = { item ->
                    viewModel.moveToVault(item)
                    viewModel.setShowImportToVaultDialog(false)
                }
            )
        }
    }
}
