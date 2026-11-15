package com.example.player

import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.Tracks
import androidx.media3.common.util.UnstableApi
import com.example.data.model.MediaItem
import com.example.ui.components.resolvePlayableUri
import java.util.Locale
import kotlin.math.abs

data class StreamPropertyRow(
    val label: String,
    val value: String
)

data class MediaStreamInfo(
    val streamNumber: Int,
    val streamType: String,
    val codecShortTitle: String,
    val rows: List<StreamPropertyRow>
)

@OptIn(UnstableApi::class)
object StreamPropertyExtractor {

    fun parseCodecShortTitle(mimeType: String?): String {
        val mime = mimeType?.lowercase(Locale.US) ?: return "Unknown"
        return when {
            mime == MimeTypes.VIDEO_AV1 || mime == "video/av1" -> "AV1"
            mime == MimeTypes.VIDEO_H264 || mime == "video/h264" -> "H.264 (AVC)"
            mime == MimeTypes.VIDEO_H265 || mime == "video/h265" -> "H.265 (HEVC)"
            mime == MimeTypes.VIDEO_VP9 || mime == "video/vp9" -> "VP9"
            mime == MimeTypes.VIDEO_VP8 -> "VP8"
            mime == MimeTypes.VIDEO_MP4V -> "MPEG-4"
            mime == MimeTypes.AUDIO_AAC -> "AAC"
            mime == MimeTypes.AUDIO_OPUS -> "Opus"
            mime == MimeTypes.AUDIO_MPEG -> "MP3"
            mime == MimeTypes.AUDIO_FLAC -> "FLAC"
            mime == MimeTypes.AUDIO_VORBIS -> "Vorbis"
            mime == MimeTypes.AUDIO_AC3 -> "AC-3"
            mime == MimeTypes.AUDIO_E_AC3 || mime == MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3"
            mime == MimeTypes.AUDIO_DTS || mime == MimeTypes.AUDIO_DTS_HD -> "DTS"
            mime.startsWith("text/") || mime.contains("subrip") || mime.contains("ssa") -> "Subtitle"
            else -> mime.substringAfter('/').uppercase(Locale.US)
        }
    }

    fun parseAudioCodecFullName(mimeType: String?): String {
        return when (mimeType?.lowercase(Locale.US)) {
            MimeTypes.AUDIO_AAC -> "AAC (Advanced Audio Coding)"
            MimeTypes.AUDIO_OPUS -> "Opus (Interactive Audio)"
            MimeTypes.AUDIO_MPEG -> "MP3 (MPEG Audio Layer III)"
            MimeTypes.AUDIO_FLAC -> "FLAC (Free Lossless Audio Codec)"
            MimeTypes.AUDIO_VORBIS -> "Vorbis Audio"
            MimeTypes.AUDIO_AC3 -> "AC-3 (Dolby Digital)"
            MimeTypes.AUDIO_E_AC3, MimeTypes.AUDIO_E_AC3_JOC -> "E-AC-3 (Dolby Digital Plus)"
            MimeTypes.AUDIO_DTS, MimeTypes.AUDIO_DTS_HD -> "DTS Audio"
            else -> parseCodecShortTitle(mimeType)
        }
    }

    fun formatDecoderDisplayName(mimeType: String?, decoderName: String?): String {
        val raw = decoderName?.takeIf { it.isNotBlank() && it != "Pending" }
        val mime = mimeType?.lowercase(Locale.US)
        if (raw == null) {
            return if (mime?.startsWith("audio/") == true) {
                parseAudioCodecFullName(mime)
            } else {
                parseCodecShortTitle(mime)
            }
        }
        val lower = raw.lowercase(Locale.US)
        return when {
            lower.contains("dav1d") -> "dav1d AV1 decoder by VideoLAN ($raw)"
            lower.contains("av1") && (lower.startsWith("c2.android") || lower.startsWith("omx.google")) ->
                "libgav1 AV1 Software decoder ($raw)"
            mime == MimeTypes.AUDIO_AAC -> "AAC (Advanced Audio Coding) • $raw"
            else -> "${parseCodecShortTitle(mime)} ($raw)"
        }
    }

    fun parseVideoProfile(mimeType: String?, codecs: String?, mediaFormatProfile: Int? = null): String {
        val mime = mimeType?.lowercase(Locale.US)
        val c = codecs?.lowercase(Locale.US).orEmpty()

        when {
            mime == MimeTypes.VIDEO_AV1 || c.startsWith("av01.") -> {
                val parts = c.split('.')
                val profCode = parts.getOrNull(1)?.toIntOrNull()
                return when (profCode) {
                    0 -> "Main"
                    1 -> "High"
                    2 -> "Professional"
                    else -> when {
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            mediaFormatProfile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain10 -> "Main 10"
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            mediaFormatProfile == MediaCodecInfo.CodecProfileLevel.AV1ProfileMain8 -> "Main"
                        else -> "Main"
                    }
                }
            }
            mime == MimeTypes.VIDEO_H264 || c.startsWith("avc1.") || c.startsWith("avc3.") -> {
                val hexProfile = c.substringAfter('.', "").take(2).toIntOrNull(16)
                return when (hexProfile) {
                    66 -> "Baseline"
                    77 -> "Main"
                    88 -> "Extended"
                    100 -> "High"
                    110 -> "High 10"
                    122 -> "High 4:2:2"
                    else -> when (mediaFormatProfile) {
                        MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline -> "Baseline"
                        MediaCodecInfo.CodecProfileLevel.AVCProfileMain -> "Main"
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh -> "High"
                        MediaCodecInfo.CodecProfileLevel.AVCProfileHigh10 -> "High 10"
                        else -> "High / Main"
                    }
                }
            }
            mime == MimeTypes.VIDEO_H265 || c.startsWith("hvc1.") || c.startsWith("hev1.") -> {
                val profPart = c.split('.').getOrNull(1)?.toIntOrNull()
                return when (profPart) {
                    1 -> "Main"
                    2 -> "Main 10"
                    4 -> "Main Still / Rext"
                    else -> when (mediaFormatProfile) {
                        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain -> "Main"
                        MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10 -> "Main 10"
                        else -> "Main"
                    }
                }
            }
            mime == MimeTypes.VIDEO_VP9 || c.startsWith("vp09.") -> {
                val profPart = c.split('.').getOrNull(1)?.toIntOrNull()
                return when (profPart) {
                    0 -> "Profile 0 (8-bit)"
                    1 -> "Profile 1"
                    2 -> "Profile 2 (10-bit)"
                    3 -> "Profile 3 (10-bit)"
                    else -> "Profile 0"
                }
            }
            else -> return codecs?.takeIf { it.isNotBlank() } ?: "Main"
        }
    }

    fun parseAudioProfile(mimeType: String?, codecs: String?, aacProfileInt: Int? = null): String {
        val c = codecs?.lowercase(Locale.US).orEmpty()
        if (mimeType?.lowercase(Locale.US) == MimeTypes.AUDIO_AAC || c.startsWith("mp4a.")) {
            val objType = c.substringAfter("mp4a.40.", "").toIntOrNull() ?: aacProfileInt
            return when (objType) {
                1, MediaCodecInfo.CodecProfileLevel.AACObjectMain -> "Main"
                2, MediaCodecInfo.CodecProfileLevel.AACObjectLC -> "LC"
                3, MediaCodecInfo.CodecProfileLevel.AACObjectSSR -> "SSR"
                4, MediaCodecInfo.CodecProfileLevel.AACObjectLTP -> "LTP"
                5, MediaCodecInfo.CodecProfileLevel.AACObjectHE -> "HE-AAC"
                29, MediaCodecInfo.CodecProfileLevel.AACObjectHE_PS -> "HE-AAC v2"
                42 -> "xHE-AAC"
                else -> "LC"
            }
        }
        return codecs?.takeIf { it.isNotBlank() } ?: "Standard"
    }

    fun formatFrameRate(frameRate: Float): String {
        if (frameRate <= 0f) return "-"
        val rounded = kotlin.math.round(frameRate)
        return if (abs(frameRate - rounded) < 0.05f) {
            "${rounded.toInt()} FPS"
        } else {
            String.format(Locale.US, "%.2f FPS", frameRate)
        }
    }

    fun formatBitrate(bitrateBps: Int, preferKbps: Boolean = false): String {
        if (bitrateBps <= 0) return "-"
        return if (!preferKbps && bitrateBps >= 1_000_000) {
            String.format(Locale.US, "%.1f Mbit/s", bitrateBps / 1_000_000.0)
        } else {
            String.format(Locale.US, "%.1f kbits/sec", bitrateBps / 1_000.0)
        }
    }

    fun formatChannelLayout(channelCount: Int): String {
        return when (channelCount) {
            1 -> "Mono (1 Channel)"
            2 -> "Stereo (2 Channel)"
            6 -> "5.1 Surround (6 Channel)"
            8 -> "7.1 Surround (8 Channel)"
            in 3..16 -> "$channelCount Channel"
            else -> "-"
        }
    }

    @OptIn(UnstableApi::class)
    fun extractEncodingSoftware(format: Format?, mediaMetadata: MediaMetadata? = null): String? {
        val metadata = format?.metadata
        if (metadata != null) {
            for (i in 0 until metadata.length()) {
                val entryStr = metadata.get(i).toString()
                val match = Regex(
                    "(?:ENCODER|encoder|writing_application|software|TSSE)\\s*[=:]\\s*\\[?([^\\],\\n]+)",
                    RegexOption.IGNORE_CASE
                ).find(entryStr)
                val value = match?.groupValues?.getOrNull(1)?.trim()
                if (!value.isNullOrBlank()) {
                    return value
                }
                if (entryStr.contains("Lavc", ignoreCase = true) ||
                    entryStr.contains("Lavf", ignoreCase = true) ||
                    entryStr.contains("libsvtav1", ignoreCase = true) ||
                    entryStr.contains("x264", ignoreCase = true) ||
                    entryStr.contains("x265", ignoreCase = true)
                ) {
                    return entryStr.substringAfter("value=", entryStr.substringAfter('=')).trim()
                }
            }
        }
        val writer = mediaMetadata?.writer?.toString()?.takeIf { it.isNotBlank() }
        return writer
    }

    @OptIn(UnstableApi::class)
    fun buildMediaStreamInfos(
        tracks: Tracks?,
        diagnostics: PlaybackDiagnosticsState,
        fileSizeBytes: Long = 0L,
        durationMs: Long = 0L,
        isBangla: Boolean = false
    ): List<MediaStreamInfo> {
        val streams = mutableListOf<MediaStreamInfo>()
        var streamNum = 1

        var videoFormat: Format? = diagnostics.videoFormat
        var audioFormat: Format? = diagnostics.audioFormat
        val subtitleFormats = mutableListOf<Format>()

        tracks?.groups?.forEach { group ->
            when (group.type) {
                C.TRACK_TYPE_VIDEO -> {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i) || videoFormat == null) {
                            videoFormat = group.getTrackFormat(i)
                            if (group.isTrackSelected(i)) break
                        }
                    }
                }
                C.TRACK_TYPE_AUDIO -> {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i) || audioFormat == null) {
                            audioFormat = group.getTrackFormat(i)
                            if (group.isTrackSelected(i)) break
                        }
                    }
                }
                C.TRACK_TYPE_TEXT -> {
                    for (i in 0 until group.length) {
                        if (group.isTrackSelected(i)) {
                            subtitleFormats.add(group.getTrackFormat(i))
                        }
                    }
                }
            }
        }

        // Stream #1 — Video
        val vf = videoFormat
        if (vf != null || diagnostics.width > 0 || diagnostics.videoMimeType != "-") {
            val mime = vf?.sampleMimeType ?: diagnostics.videoMimeType.takeIf { it != "-" }
            val codecs = vf?.codecs ?: diagnostics.videoCodec.takeIf { it != "-" }
            val w = (vf?.width?.takeIf { it > 0 } ?: diagnostics.width).coerceAtLeast(0)
            val h = (vf?.height?.takeIf { it > 0 } ?: diagnostics.height).coerceAtLeast(0)
            val fps = vf?.frameRate?.takeIf { it > 0f } ?: diagnostics.frameRate
            val rawBitrate = vf?.bitrate?.takeIf { it > 0 }
                ?: diagnostics.bitrateBps.takeIf { it > 0 }
                ?: estimateVideoBitrateFallback(fileSizeBytes, durationMs, audioFormat?.bitrate ?: 0)
            val encSw = extractEncodingSoftware(vf) ?: diagnostics.encodingSoftware

            val rows = buildList {
                add(StreamPropertyRow(if (isBangla) "ধরন" else "Type", if (isBangla) "ভিডিও (Video)" else "Video"))
                add(
                    StreamPropertyRow(
                        if (isBangla) "কোডেক" else "Codec",
                        formatDecoderDisplayName(mime, diagnostics.selectedVideoDecoder)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "প্রোফাইল" else "Profile",
                        parseVideoProfile(mime, codecs)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "রেজোলিউশন" else "Resolution",
                        if (w > 0 && h > 0) "$w × $h" else "-"
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "ফ্রেম রেট" else "Frame rate",
                        formatFrameRate(fps)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "বিট রেট" else "Bit rate",
                        formatBitrate(rawBitrate, preferKbps = false)
                    )
                )
                if (!encSw.isNullOrBlank()) {
                    add(StreamPropertyRow("Encoding SW", encSw))
                }
            }

            streams.add(
                MediaStreamInfo(
                    streamNumber = streamNum++,
                    streamType = if (isBangla) "ভিডিও" else "Video",
                    codecShortTitle = "Video Codec: ${parseCodecShortTitle(mime)}",
                    rows = rows
                )
            )
        }

        // Stream #2 — Audio
        val af = audioFormat
        if (af != null || diagnostics.audioMimeType != "-") {
            val mime = af?.sampleMimeType ?: diagnostics.audioMimeType.takeIf { it != "-" }
            val codecs = af?.codecs ?: diagnostics.audioCodec.takeIf { it != "-" }
            val sampleRate = af?.sampleRate?.takeIf { it > 0 } ?: 0
            val channels = af?.channelCount?.takeIf { it > 0 } ?: 0
            val bitrate = af?.bitrate?.takeIf { it > 0 } ?: 0

            val rows = buildList {
                add(StreamPropertyRow(if (isBangla) "ধরন" else "Type", if (isBangla) "অডিও (Audio)" else "Audio"))
                add(
                    StreamPropertyRow(
                        if (isBangla) "কোডেক" else "Codec",
                        formatDecoderDisplayName(mime, diagnostics.selectedAudioDecoder)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "প্রোফাইল" else "Profile",
                        parseAudioProfile(mime, codecs)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "স্যাম্পল রেট" else "Sample rate",
                        if (sampleRate > 0) "$sampleRate Hz" else "-"
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "চ্যানেল" else "Channels",
                        formatChannelLayout(channels)
                    )
                )
                add(
                    StreamPropertyRow(
                        if (isBangla) "বিট রেট" else "Bit rate",
                        formatBitrate(bitrate, preferKbps = true)
                    )
                )
            }

            streams.add(
                MediaStreamInfo(
                    streamNumber = streamNum++,
                    streamType = if (isBangla) "অডিও" else "Audio",
                    codecShortTitle = "Audio Codec: ${parseCodecShortTitle(mime)}",
                    rows = rows
                )
            )
        }

        // Subtitle streams if active
        subtitleFormats.forEach { subFmt ->
            streams.add(
                MediaStreamInfo(
                    streamNumber = streamNum++,
                    streamType = if (isBangla) "সাবটাইটেল" else "Subtitle",
                    codecShortTitle = "Subtitle: ${parseCodecShortTitle(subFmt.sampleMimeType)}",
                    rows = listOf(
                        StreamPropertyRow(if (isBangla) "ধরন" else "Type", "Subtitle"),
                        StreamPropertyRow(if (isBangla) "ফরম্যাট" else "Format", subFmt.sampleMimeType ?: "Text"),
                        StreamPropertyRow(
                            if (isBangla) "ভাষা" else "Language",
                            subFmt.language ?: subFmt.label ?: "Default"
                        )
                    )
                )
            )
        }

        return streams
    }

    fun inspectFileStreamsOffline(
        context: Context,
        item: MediaItem,
        isBangla: Boolean = false
    ): List<MediaStreamInfo> {
        val uri: Uri = resolvePlayableUri(item)
        val extractor = MediaExtractor()
        val streams = mutableListOf<MediaStreamInfo>()
        var streamNum = 1

        try {
            if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                extractor.setDataSource(uri.path!!)
            } else {
                extractor.setDataSource(context, uri, null)
            }

            var retrieverBitrate = 0
            try {
                val mmr = MediaMetadataRetriever()
                if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                    mmr.setDataSource(uri.path!!)
                } else {
                    mmr.setDataSource(context, uri)
                }
                retrieverBitrate = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
                mmr.release()
            } catch (_: Exception) {
            }

            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/", ignoreCase = true)) {
                    val w = fmt.safeInt(MediaFormat.KEY_WIDTH).takeIf { it > 0 } ?: item.width
                    val h = fmt.safeInt(MediaFormat.KEY_HEIGHT).takeIf { it > 0 } ?: item.height
                    val fps = fmt.safeFloat(MediaFormat.KEY_FRAME_RATE)
                    val prof = fmt.safeInt(MediaFormat.KEY_PROFILE).takeIf { it > 0 }
                    val br = fmt.safeInt(MediaFormat.KEY_BIT_RATE).takeIf { it > 0 }
                        ?: retrieverBitrate.takeIf { it > 0 }
                        ?: estimateVideoBitrateFallback(item.size, item.duration, 0)
                    val codecStr = fmt.safeString("codecs-string")

                    streams.add(
                        MediaStreamInfo(
                            streamNumber = streamNum++,
                            streamType = if (isBangla) "ভিডিও" else "Video",
                            codecShortTitle = "Video Codec: ${parseCodecShortTitle(mime)}",
                            rows = listOf(
                                StreamPropertyRow(if (isBangla) "ধরন" else "Type", "Video"),
                                StreamPropertyRow(if (isBangla) "কোডেক" else "Codec", parseCodecShortTitle(mime)),
                                StreamPropertyRow(if (isBangla) "প্রোফাইল" else "Profile", parseVideoProfile(mime, codecStr, prof)),
                                StreamPropertyRow(if (isBangla) "রেজোলিউশন" else "Resolution", if (w > 0 && h > 0) "$w × $h" else "-"),
                                StreamPropertyRow(if (isBangla) "ফ্রেম রেট" else "Frame rate", formatFrameRate(fps)),
                                StreamPropertyRow(if (isBangla) "বিট রেট" else "Bit rate", formatBitrate(br))
                            )
                        )
                    )
                } else if (mime.startsWith("audio/", ignoreCase = true)) {
                    val sr = fmt.safeInt(MediaFormat.KEY_SAMPLE_RATE)
                    val ch = fmt.safeInt(MediaFormat.KEY_CHANNEL_COUNT)
                    val br = fmt.safeInt(MediaFormat.KEY_BIT_RATE)
                    val aacProf = fmt.safeInt(MediaFormat.KEY_AAC_PROFILE).takeIf { it > 0 }
                    val codecStr = fmt.safeString("codecs-string")

                    streams.add(
                        MediaStreamInfo(
                            streamNumber = streamNum++,
                            streamType = if (isBangla) "অডিও" else "Audio",
                            codecShortTitle = "Audio Codec: ${parseCodecShortTitle(mime)}",
                            rows = listOf(
                                StreamPropertyRow(if (isBangla) "ধরন" else "Type", "Audio"),
                                StreamPropertyRow(if (isBangla) "কোডেক" else "Codec", parseAudioCodecFullName(mime)),
                                StreamPropertyRow(if (isBangla) "প্রোফাইল" else "Profile", parseAudioProfile(mime, codecStr, aacProf)),
                                StreamPropertyRow(if (isBangla) "স্যাম্পল রেট" else "Sample rate", if (sr > 0) "$sr Hz" else "-"),
                                StreamPropertyRow(if (isBangla) "চ্যানেল" else "Channels", formatChannelLayout(ch)),
                                StreamPropertyRow(if (isBangla) "বিট রেট" else "Bit rate", formatBitrate(br, preferKbps = true))
                            )
                        )
                    )
                }
            }
        } catch (_: Exception) {
        } finally {
            try {
                extractor.release()
            } catch (_: Exception) {
            }
        }
        return streams
    }

    private fun estimateVideoBitrateFallback(fileSizeBytes: Long, durationMs: Long, audioBitrateBps: Int): Int {
        if (fileSizeBytes <= 0L || durationMs <= 0L) return 0
        val totalBps = ((fileSizeBytes * 8L * 1000L) / durationMs).toInt()
        return (totalBps - audioBitrateBps.coerceAtLeast(0)).coerceAtLeast(0)
    }

    private fun MediaFormat.safeInt(key: String): Int {
        return try {
            if (containsKey(key)) getInteger(key) else 0
        } catch (_: Exception) {
            0
        }
    }

    private fun MediaFormat.safeFloat(key: String): Float {
        return try {
            if (containsKey(key)) {
                try {
                    getFloat(key)
                } catch (_: Exception) {
                    getInteger(key).toFloat()
                }
            } else 0f
        } catch (_: Exception) {
            0f
        }
    }

    private fun MediaFormat.safeString(key: String): String? {
        return try {
            if (containsKey(key)) getString(key) else null
        } catch (_: Exception) {
            null
        }
    }
}
