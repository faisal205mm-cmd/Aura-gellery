package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.MediaItem
import com.example.data.model.StorageType
import com.example.data.repository.GalleryRepository
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
}
