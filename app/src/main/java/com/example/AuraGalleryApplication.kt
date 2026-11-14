package com.example

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.decode.VideoFrameDecoder
import coil.memory.MemoryCache
import com.example.player.DeviceCapabilityInspector

class AuraGalleryApplication : Application(), ImageLoaderFactory {
    override fun newImageLoader(): ImageLoader {
        val deviceProfile = DeviceCapabilityInspector.inspect(this)
        val cachePercent = if (deviceProfile.isLowRamDevice) 0.15 else 0.25

        return ImageLoader.Builder(this)
            .components {
                add(VideoFrameDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(cachePercent)
                    .build()
            }
            .allowRgb565(deviceProfile.isLowRamDevice)
            .crossfade(true)
            .build()
    }
}
