package com.example.ui.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.example.data.model.MediaItem
import java.io.File
import java.util.Locale

enum class AspectRatioMode {
    FIT,        // Letterbox
    STRETCH,    // Stretch to exact screen size
    CROP_FILL,  // Zoom in to fill screen with no black bars
    ORIGINAL    // 1:1 original aspect ratio
}

data class VideoQualityOption(
    val id: String,
    val label: String,
    val shortLabel: String,
    val detail: String,
    val width: Int,
    val height: Int,
    val bitrate: Int,
    val trackGroup: Tracks.Group?,
    val trackIndex: Int,
    val isSupported: Boolean = true
)

data class AudioTrackOption(
    val id: String,
    val label: String,
    val detail: String,
    val trackGroup: Tracks.Group?,
    val trackIndex: Int,
    val isSelected: Boolean
)

data class SubtitleTrackOption(
    val id: String,
    val label: String,
    val detail: String,
    val trackGroup: Tracks.Group?,
    val trackIndex: Int,
    val isSelected: Boolean,
    val isOffOption: Boolean = false
)

internal fun formatQualityLabels(width: Int, height: Int): Pair<String, String> {
    val shortSide = minOf(width, height)
    val longSide = maxOf(width, height)
    return when {
        shortSide >= 2160 || longSide >= 3840 -> "2160p 4K UHD" to "4K"
        shortSide >= 1440 || longSide >= 2560 -> "1440p 2K QHD" to "2K"
        shortSide >= 1080 || longSide >= 1920 -> "1080p Full HD" to "1080p"
        shortSide >= 720 -> "720p HD" to "720p"
        shortSide > 0 -> "${shortSide}p" to "${shortSide}p"
        else -> "Standard" to "SD"
    }
}

@OptIn(UnstableApi::class)
fun extractVideoQualityOptions(tracks: Tracks, isBangla: Boolean): List<VideoQualityOption> {
    val trackOptions = mutableListOf<VideoQualityOption>()
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_VIDEO) {
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                if (format.width > 0 && format.height > 0) {
                    val (label, shortLabel) = formatQualityLabels(format.width, format.height)
                    val bitratePart = if (format.bitrate > 0) {
                        String.format(Locale.US, " • %.1f Mbps", format.bitrate / 1_000_000f)
                    } else {
                        ""
                    }
                    val fpsPart = if (format.frameRate > 0f) {
                        " • ${format.frameRate.toInt()}fps"
                    } else {
                        ""
                    }
                    trackOptions.add(
                        VideoQualityOption(
                            id = "${groupIdx}_$trackIdx",
                            label = label,
                            shortLabel = shortLabel,
                            detail = "${format.width}×${format.height}$fpsPart$bitratePart",
                            width = format.width,
                            height = format.height,
                            bitrate = format.bitrate,
                            trackGroup = group,
                            trackIndex = trackIdx,
                            isSupported = group.isTrackSupported(trackIdx)
                        )
                    )
                }
            }
        }
    }

    val sortedTracks = trackOptions.sortedWith(
        compareByDescending<VideoQualityOption> { minOf(it.width, it.height) }
            .thenByDescending { maxOf(it.width, it.height) }
            .thenByDescending { it.bitrate }
    )

    val autoOption = VideoQualityOption(
        id = "auto",
        label = if (isBangla) "অটো (স্বয়ংক্রিয়)" else "Auto (Recommended)",
        shortLabel = if (isBangla) "অটো" else "Auto",
        detail = if (isBangla) "ডিভাইসের সক্ষমতা অনুযায়ী সেরা কোয়ালিটি" else "Adapts to device decoder & capabilities",
        width = 0,
        height = 0,
        bitrate = 0,
        trackGroup = null,
        trackIndex = -1
    )

    return listOf(autoOption) + sortedTracks
}

fun extractAudioTrackOptions(tracks: Tracks, isBangla: Boolean): List<AudioTrackOption> {
    val result = mutableListOf<AudioTrackOption>()
    var counter = 1
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_AUDIO) {
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
                    ?.let { Locale(it).getDisplayLanguage(Locale.ENGLISH).ifBlank { it } }
                val label = format.label?.takeIf { it.isNotBlank() }
                    ?: lang
                    ?: if (isBangla) "অডিও ট্র্যাক $counter" else "Audio Track $counter"
                val chText = if (format.channelCount > 0) "${format.channelCount}ch" else ""
                val rateText = if (format.sampleRate > 0) "${format.sampleRate / 1000}kHz" else ""
                val codecText = format.codecs ?: format.sampleMimeType ?: ""
                val detail = listOf(codecText, chText, rateText).filter { it.isNotBlank() }.joinToString(" • ")

                result.add(
                    AudioTrackOption(
                        id = "audio_${groupIdx}_$trackIdx",
                        label = label,
                        detail = detail.ifBlank { if (isBangla) "স্ট্যান্ডার্ড অডিও" else "Standard audio" },
                        trackGroup = group,
                        trackIndex = trackIdx,
                        isSelected = group.isTrackSelected(trackIdx)
                    )
                )
                counter++
            }
        }
    }
    return result
}

fun extractSubtitleTrackOptions(tracks: Tracks, isBangla: Boolean): List<SubtitleTrackOption> {
    val textTracks = mutableListOf<SubtitleTrackOption>()
    var counter = 1
    tracks.groups.forEachIndexed { groupIdx, group ->
        if (group.type == C.TRACK_TYPE_TEXT) {
            for (trackIdx in 0 until group.length) {
                val format = group.getTrackFormat(trackIdx)
                val lang = format.language?.takeIf { it.isNotBlank() && it != "und" }
                    ?.let { Locale(it).getDisplayLanguage(Locale.ENGLISH).ifBlank { it } }
                val label = format.label?.takeIf { it.isNotBlank() }
                    ?: lang
                    ?: if (isBangla) "সাবটাইটেল $counter" else "Subtitle $counter"
                val detail = format.sampleMimeType ?: "Text"

                textTracks.add(
                    SubtitleTrackOption(
                        id = "sub_${groupIdx}_$trackIdx",
                        label = label,
                        detail = detail,
                        trackGroup = group,
                        trackIndex = trackIdx,
                        isSelected = group.isTrackSelected(trackIdx)
                    )
                )
                counter++
            }
        }
    }

    val anySelected = textTracks.any { it.isSelected }
    val offOption = SubtitleTrackOption(
        id = "sub_off",
        label = if (isBangla) "সাবটাইটেল বন্ধ" else "Subtitles Off",
        detail = if (isBangla) "কোনো সাবটাইটেল দেখাবে না" else "Disable subtitle rendering",
        trackGroup = null,
        trackIndex = -1,
        isSelected = !anySelected,
        isOffOption = true
    )

    return listOf(offOption) + textTracks
}

fun detectSubtitleMimeType(uri: Uri, displayName: String?): String {
    val target = (displayName ?: uri.toString()).lowercase(Locale.US)
    return when {
        target.endsWith(".vtt") -> MimeTypes.TEXT_VTT
        target.endsWith(".ass") || target.endsWith(".ssa") -> MimeTypes.TEXT_SSA
        target.endsWith(".ttml") || target.endsWith(".xml") -> MimeTypes.APPLICATION_TTML
        else -> MimeTypes.APPLICATION_SUBRIP
    }
}

fun resolvePlayableUri(item: MediaItem): Uri {
    val uriStr = item.uri.trim()
    if (uriStr.startsWith("content://") ||
        uriStr.startsWith("android.resource://") ||
        uriStr.startsWith("http://") ||
        uriStr.startsWith("https://") ||
        uriStr.startsWith("file://")
    ) {
        return Uri.parse(uriStr)
    }
    if (item.path.isNotBlank()) {
        return Uri.fromFile(File(item.path))
    }
    return Uri.parse(uriStr)
}
