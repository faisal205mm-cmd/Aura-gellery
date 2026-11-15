package com.example.player

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build

enum class RenderSurfaceMode(val label: String) {
    SURFACE_VIEW("SurfaceView (Hardware HWC)"),
    TEXTURE_VIEW("TextureView (GL Composition)")
}

enum class DecoderStrategy(val label: String) {
    HARDWARE_FIRST("Hardware Preferred + Capability Guard"),
    SOFTWARE_FALLBACK("Software Decoder Fallback")
}

data class CodecSupportSummary(
    val mimeType: String,
    val displayName: String,
    val hasHardwareDecoder: Boolean,
    val hasSoftwareDecoder: Boolean,
    val hardwareDecoderName: String?,
    val softwareDecoderName: String?,
    val maxHardwareWidth: Int,
    val maxHardwareHeight: Int,
    val supports1080pHardware: Boolean,
    val supports1080p60Hardware: Boolean,
    val supports1440pHardware: Boolean,
    val supports4kHardware: Boolean,
    val supports10BitProfile: Boolean,
    val maxSoftwareWidth: Int,
    val maxSoftwareHeight: Int
) {
    val maxSupportedLabel: String
        get() = when {
            supports4kHardware -> "4K HW"
            supports1440pHardware -> "1440p HW"
            supports1080p60Hardware -> "1080p60 HW"
            supports1080pHardware -> "1080p HW"
            maxHardwareHeight >= 720 || maxHardwareWidth >= 1280 -> "720p HW"
            hasHardwareDecoder -> "${maxHardwareWidth}x${maxHardwareHeight} HW"
            hasSoftwareDecoder -> "${maxSoftwareWidth}x${maxSoftwareHeight} SW"
            else -> "Unsupported"
        }
}

data class PlaybackBufferConfig(
    val minBufferMs: Int,
    val maxBufferMs: Int,
    val bufferForPlaybackMs: Int,
    val bufferForPlaybackAfterRebufferMs: Int,
    val targetBufferBytes: Int,
    val backBufferDurationMs: Int
)

data class DevicePlaybackProfile(
    val manufacturer: String,
    val model: String,
    val hardware: String,
    val androidVersion: String,
    val sdkInt: Int,
    val totalRamMb: Long,
    val availableRamMb: Long,
    val isLowRamDevice: Boolean,
    val displayWidth: Int,
    val displayHeight: Int,
    val recommendedSurfaceMode: RenderSurfaceMode,
    val bufferConfig: PlaybackBufferConfig,
    val codecSummaries: Map<String, CodecSupportSummary>
) {
    fun summaryFor(mimeType: String?): CodecSupportSummary? {
        if (mimeType.isNullOrBlank()) return null
        val normalized = normalizeVideoMimeType(mimeType)
        return codecSummaries[normalized]
    }
}

fun normalizeVideoMimeType(mimeType: String): String {
    return when (mimeType.lowercase()) {
        "video/h264", "video/avc1" -> MediaFormat.MIMETYPE_VIDEO_AVC
        "video/h265", "video/hev1", "video/hvc1" -> MediaFormat.MIMETYPE_VIDEO_HEVC
        "video/vp9" -> MediaFormat.MIMETYPE_VIDEO_VP9
        "video/av1" -> "video/av1"
        else -> mimeType.lowercase()
    }
}

fun isSoftwareCodecInfo(info: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return info.isSoftwareOnly
    }
    val name = info.name.lowercase()
    return name.startsWith("omx.google.") ||
        name.startsWith("c2.android.") ||
        name.contains(".sw.") ||
        name.contains("ffmpeg")
}

object DeviceCapabilityInspector {

    private const val MIME_VIDEO_AV1 = "video/av1"

    private val trackedMimeTypes = listOf(
        MediaFormat.MIMETYPE_VIDEO_AVC to "H.264 / AVC",
        MediaFormat.MIMETYPE_VIDEO_HEVC to "H.265 / HEVC",
        MediaFormat.MIMETYPE_VIDEO_VP9 to "VP9",
        MIME_VIDEO_AV1 to "AV1",
        MediaFormat.MIMETYPE_VIDEO_MPEG4 to "MPEG-4 / AVI"
    )

    fun inspect(context: Context): DevicePlaybackProfile {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        val hasMemInfo = try {
            am?.getMemoryInfo(memInfo)
            true
        } catch (_: Exception) {
            false
        }

        val totalRamMb = if (hasMemInfo && memInfo.totalMem > 0L) {
            memInfo.totalMem / (1024L * 1024L)
        } else {
            2048L
        }
        val availRamMb = if (hasMemInfo && memInfo.availMem > 0L) {
            memInfo.availMem / (1024L * 1024L)
        } else {
            1024L
        }

        val isSystemLowRam = try {
            am?.isLowRamDevice == true
        } catch (_: Exception) {
            false
        }

        // Devices with <= 2.5GB RAM (such as 2GB devices) benefit from tight LoadControl caps
        val isLowRam = isSystemLowRam || memInfo.lowMemory || totalRamMb <= 2560L

        val metrics = context.resources.displayMetrics
        val displayWidth = metrics.widthPixels
        val displayHeight = metrics.heightPixels

        val bufferConfig = createBufferConfig(isLowRam)
        val codecSummaries = inspectVideoCodecs()

        // SurfaceView avoids GPU TextureView EGL copy overhead and handles hardware 1080p buffers reliably
        val recommendedSurface = RenderSurfaceMode.SURFACE_VIEW

        return DevicePlaybackProfile(
            manufacturer = Build.MANUFACTURER ?: "Unknown",
            model = Build.MODEL ?: "Unknown",
            hardware = Build.HARDWARE ?: "Unknown",
            androidVersion = Build.VERSION.RELEASE ?: "Unknown",
            sdkInt = Build.VERSION.SDK_INT,
            totalRamMb = totalRamMb,
            availableRamMb = availRamMb,
            isLowRamDevice = isLowRam,
            displayWidth = displayWidth,
            displayHeight = displayHeight,
            recommendedSurfaceMode = recommendedSurface,
            bufferConfig = bufferConfig,
            codecSummaries = codecSummaries
        )
    }

    internal fun createBufferConfig(isLowRamDevice: Boolean): PlaybackBufferConfig {
        return if (isLowRamDevice) {
            PlaybackBufferConfig(
                minBufferMs = 1500,
                maxBufferMs = 8000,
                bufferForPlaybackMs = 400,
                bufferForPlaybackAfterRebufferMs = 1000,
                targetBufferBytes = 12 * 1024 * 1024, // 12 MB cap for 2GB RAM safety
                backBufferDurationMs = 2000
            )
        } else {
            PlaybackBufferConfig(
                minBufferMs = 2500,
                maxBufferMs = 20000,
                bufferForPlaybackMs = 500,
                bufferForPlaybackAfterRebufferMs = 1500,
                targetBufferBytes = 32 * 1024 * 1024, // 32 MB cap
                backBufferDurationMs = 5000
            )
        }
    }

    private fun inspectVideoCodecs(): Map<String, CodecSupportSummary> {
        val allCodecs = try {
            MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
        } catch (_: Exception) {
            emptyArray()
        }

        val summaries = mutableMapOf<String, CodecSupportSummary>()
        for ((mimeType, label) in trackedMimeTypes) {
            var hasHw = false
            var hasSw = false
            var hwName: String? = null
            var swName: String? = null
            var maxHwW = 0
            var maxHwH = 0
            var maxSwW = 0
            var maxSwH = 0
            var hw1080 = false
            var hw1080p60 = false
            var hw1440 = false
            var hw4k = false
            var supports10Bit = false

            for (info in allCodecs) {
                if (info.isEncoder) continue
                val supportsMime = try {
                    info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) }
                } catch (_: Exception) {
                    false
                }
                if (!supportsMime) continue

                val caps = try {
                    info.getCapabilitiesForType(mimeType)
                } catch (_: Exception) {
                    null
                } ?: continue

                val videoCaps = caps.videoCapabilities
                val isSw = isSoftwareCodecInfo(info)
                val maxW = videoCaps?.supportedWidths?.upper ?: 0
                val maxH = videoCaps?.supportedHeights?.upper ?: 0

                val can1080 = videoCaps?.isSizeSupported(1920, 1080) == true ||
                    videoCaps?.isSizeSupported(1080, 1920) == true
                val can1080p60 = videoCaps?.areSizeAndRateSupported(1920, 1080, 60.0) == true
                val can1440 = videoCaps?.isSizeSupported(2560, 1440) == true
                val can4k = videoCaps?.isSizeSupported(3840, 2160) == true

                val has10BitProfile = caps.profileLevels?.any { pl ->
                    when (mimeType) {
                        MediaFormat.MIMETYPE_VIDEO_HEVC ->
                            pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 ||
                                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                    pl.profile == MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus)
                        MediaFormat.MIMETYPE_VIDEO_AVC ->
                            pl.profile == MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10
                        MediaFormat.MIMETYPE_VIDEO_VP9 ->
                            pl.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile2 ||
                                pl.profile == MediaCodecInfo.CodecProfileLevel.VP9Profile3
                        MIME_VIDEO_AV1 ->
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                                pl.profile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10
                        else -> false
                    }
                } == true

                if (isSw) {
                    hasSw = true
                    if (swName == null) swName = info.name
                    if (maxW * maxH > maxSwW * maxSwH) {
                        maxSwW = maxW
                        maxSwH = maxH
                    }
                } else {
                    hasHw = true
                    if (hwName == null) hwName = info.name
                    if (maxW * maxH > maxHwW * maxHwH) {
                        maxHwW = maxW
                        maxHwH = maxH
                    }
                    hw1080 = hw1080 || can1080
                    hw1080p60 = hw1080p60 || can1080p60
                    hw1440 = hw1440 || can1440
                    hw4k = hw4k || can4k
                    supports10Bit = supports10Bit || has10BitProfile
                }
            }

            summaries[mimeType] = CodecSupportSummary(
                mimeType = mimeType,
                displayName = label,
                hasHardwareDecoder = hasHw,
                hasSoftwareDecoder = hasSw,
                hardwareDecoderName = hwName,
                softwareDecoderName = swName,
                maxHardwareWidth = maxHwW,
                maxHardwareHeight = maxHwH,
                supports1080pHardware = hw1080,
                supports1080p60Hardware = hw1080p60,
                supports1440pHardware = hw1440,
                supports4kHardware = hw4k,
                supports10BitProfile = supports10Bit,
                maxSoftwareWidth = maxSwW,
                maxSoftwareHeight = maxSwH
            )
        }
        return summaries
    }
}
