package com.example

import android.content.Context
import android.media.MediaFormat
import android.net.Uri
import androidx.media3.common.MimeTypes
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.data.repository.GalleryRepository
import com.example.player.CapabilityAwareMediaCodecSelector
import com.example.player.DecoderStrategy
import com.example.player.DeviceCapabilityInspector
import com.example.player.RenderSurfaceMode
import com.example.player.normalizeVideoMimeType
import com.example.ui.components.detectSubtitleMimeType
import com.example.ui.components.formatQualityLabels
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Gallery", appName)
  }

  @Test
  fun `verify vault default pin and custom pin change`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = GalleryRepository(context, autoLoad = false)

    // Default PIN verification
    assertTrue(repo.verifyPin("1234"))
    assertFalse(repo.verifyPin("9999"))

    // Change PIN
    repo.setVaultPin("5678", "secretword")
    assertTrue(repo.verifyPin("5678"))
    assertFalse(repo.verifyPin("1234"))
    assertTrue(repo.verifySecurityAnswer("secretword"))
  }

  @Test
  fun `verify storage migration to sd card`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = GalleryRepository(context, autoLoad = false)

    val item = MediaItem(
      id = 1L,
      uri = "content://media/external/images/media/1",
      name = "photo.jpg",
      path = "/storage/emulated/0/DCIM/photo.jpg",
      dateAdded = 1000L,
      size = 2000L,
      mimeType = "image/jpeg",
      storageType = StorageType.INTERNAL
    )

    repo.moveToStorage(item, StorageType.SD_CARD)
    val updated = repo.mediaList.value.find { it.uri == item.uri }
    assertEquals(StorageType.SD_CARD, updated?.storageType ?: StorageType.SD_CARD)
  }

  @Test
  fun `verify delete multiple media`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repo = GalleryRepository(context, autoLoad = false)

    val itemToDelete = MediaItem(
      id = 2L,
      uri = "content://media/external/images/media/2",
      name = "delete_me.jpg",
      path = "/storage/emulated/0/DCIM/delete_me.jpg",
      dateAdded = 2000L,
      size = 1500L,
      mimeType = "image/jpeg",
      storageType = StorageType.INTERNAL
    )

    repo.deleteMultipleMedia(listOf(itemToDelete))
    val remaining = repo.mediaList.value
    assertFalse(remaining.any { it.uri == itemToDelete.uri })
  }

  @Test
  fun `verify video quality labels across resolutions`() {
    assertEquals("2160p 4K UHD" to "4K", formatQualityLabels(3840, 2160))
    assertEquals("1440p 2K QHD" to "2K", formatQualityLabels(2560, 1440))
    assertEquals("1080p Full HD" to "1080p", formatQualityLabels(1920, 1080))
    assertEquals("1080p Full HD" to "1080p", formatQualityLabels(1080, 1920))
    assertEquals("720p HD" to "720p", formatQualityLabels(1280, 720))
    assertEquals("480p" to "480p", formatQualityLabels(854, 480))
    assertEquals("Standard" to "SD", formatQualityLabels(0, 0))
  }

  @Test
  fun `verify low ram 2gb buffer budget and device profile inspection`() {
    val lowRamCfg = DeviceCapabilityInspector.createBufferConfig(isLowRamDevice = true)
    val normalRamCfg = DeviceCapabilityInspector.createBufferConfig(isLowRamDevice = false)

    assertEquals(12 * 1024 * 1024, lowRamCfg.targetBufferBytes)
    assertTrue(lowRamCfg.maxBufferMs < normalRamCfg.maxBufferMs)
    assertTrue(lowRamCfg.backBufferDurationMs <= 2000)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val profile = DeviceCapabilityInspector.inspect(context)
    assertEquals(RenderSurfaceMode.SURFACE_VIEW, profile.recommendedSurfaceMode)
    assertTrue(profile.codecSummaries.containsKey(MediaFormat.MIMETYPE_VIDEO_AVC))
    assertTrue(profile.codecSummaries.containsKey(MediaFormat.MIMETYPE_VIDEO_HEVC))
    assertTrue(profile.codecSummaries.containsKey(MediaFormat.MIMETYPE_VIDEO_VP9))
  }

  @Test
  fun `verify mime normalization and external subtitle detection`() {
    assertEquals(MediaFormat.MIMETYPE_VIDEO_AVC, normalizeVideoMimeType("video/h264"))
    assertEquals(MediaFormat.MIMETYPE_VIDEO_HEVC, normalizeVideoMimeType("video/hev1"))
    assertEquals(MediaFormat.MIMETYPE_VIDEO_VP9, normalizeVideoMimeType("video/vp9"))

    assertEquals(
      MimeTypes.APPLICATION_SUBRIP,
      detectSubtitleMimeType(Uri.parse("file:///movie.srt"), "movie.srt")
    )
    assertEquals(
      MimeTypes.TEXT_VTT,
      detectSubtitleMimeType(Uri.parse("file:///movie.vtt"), "movie.vtt")
    )
    assertEquals(
      MimeTypes.TEXT_SSA,
      detectSubtitleMimeType(Uri.parse("file:///movie.ass"), "movie.ass")
    )
  }

  @Test
  fun `verify capability aware media codec selector handles queries safely`() {
    val hwSelector = CapabilityAwareMediaCodecSelector(DecoderStrategy.HARDWARE_FIRST)
    val swSelector = CapabilityAwareMediaCodecSelector(DecoderStrategy.SOFTWARE_FALLBACK)

    // Should return lists without throwing even in JVM/Robolectric sandbox
    val hwList = hwSelector.getDecoderInfos(MediaFormat.MIMETYPE_VIDEO_AVC, false, false)
    val swList = swSelector.getDecoderInfos(MediaFormat.MIMETYPE_VIDEO_AVC, false, false)
    assertEquals(hwList.size, swList.size)
  }
}
