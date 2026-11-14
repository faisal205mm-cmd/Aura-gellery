package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecInfo
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil

@OptIn(UnstableApi::class)
class CapabilityAwareMediaCodecSelector(
    private val strategy: DecoderStrategy,
    private val failedDecoderNames: Set<String> = emptySet()
) : MediaCodecSelector {

    override fun getDecoderInfos(
        mimeType: String,
        requiresSecureDecoder: Boolean,
        requiresTunnelingDecoder: Boolean
    ): MutableList<MediaCodecInfo> {
        val allDecoders = try {
            MediaCodecSelector.DEFAULT.getDecoderInfos(
                mimeType,
                requiresSecureDecoder,
                requiresTunnelingDecoder
            )
        } catch (_: MediaCodecUtil.DecoderQueryException) {
            mutableListOf()
        }

        if (!mimeType.startsWith("video/", ignoreCase = true) || allDecoders.isEmpty()) {
            return allDecoders
        }

        val healthyDecoders = allDecoders.filterNot { it.name in failedDecoderNames }
        val excludedDecoders = allDecoders.filter { it.name in failedDecoderNames }

        val candidates = if (healthyDecoders.isNotEmpty()) healthyDecoders else allDecoders

        val software = candidates.filter { it.softwareOnly }
        val hardware = candidates.filterNot { it.softwareOnly }

        val ordered = when (strategy) {
            DecoderStrategy.HARDWARE_FIRST -> hardware + software
            DecoderStrategy.SOFTWARE_FALLBACK -> software + hardware
        }

        val finalOrdered = if (healthyDecoders.isNotEmpty()) {
            ordered + excludedDecoders
        } else {
            ordered
        }

        return finalOrdered.toMutableList()
    }
}
