package com.example.player

import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.Format
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.analytics.AnalyticsListener
import java.util.Locale

data class PlaybackDiagnosticsState(
    val videoMimeType: String = "-",
    val videoCodec: String = "-",
    val width: Int = 0,
    val height: Int = 0,
    val frameRate: Float = 0f,
    val bitrateBps: Int = 0,
    val audioMimeType: String = "-",
    val audioCodec: String = "-",
    val selectedVideoDecoder: String = "Pending",
    val selectedAudioDecoder: String = "Pending",
    val droppedFramesTotal: Int = 0,
    val playbackStateLabel: String = "IDLE",
    val firstFrameRendered: Boolean = false,
    val lastDecoderError: String? = null,
    val lastRenderError: String? = null,
    val surfaceMode: RenderSurfaceMode = RenderSurfaceMode.SURFACE_VIEW,
    val decoderStrategy: DecoderStrategy = DecoderStrategy.HARDWARE_FIRST,
    val recentEvents: List<String> = emptyList(),
    val videoFormat: Format? = null,
    val audioFormat: Format? = null,
    val encodingSoftware: String? = null
)

@OptIn(UnstableApi::class)
class PlaybackDiagnosticsCollector(
    private val initialSurfaceMode: RenderSurfaceMode,
    private val initialStrategy: DecoderStrategy,
    private val onStateChanged: (PlaybackDiagnosticsState) -> Unit
) : AnalyticsListener {

    companion object {
        private const val TAG = "AuraMedia3Diag"
        private const val MAX_EVENTS = 16
    }

    private var current = PlaybackDiagnosticsState(
        surfaceMode = initialSurfaceMode,
        decoderStrategy = initialStrategy
    )

    private fun pushState(eventLine: String? = null, mutator: (PlaybackDiagnosticsState) -> PlaybackDiagnosticsState) {
        val nextEvents = if (eventLine != null) {
            (current.recentEvents + eventLine).takeLast(MAX_EVENTS)
        } else {
            current.recentEvents
        }
        current = mutator(current).copy(recentEvents = nextEvents)
        onStateChanged(current)
    }

    override fun onPlaybackStateChanged(eventTime: AnalyticsListener.EventTime, state: Int) {
        val label = when (state) {
            Player.STATE_IDLE -> "IDLE"
            Player.STATE_BUFFERING -> "BUFFERING"
            Player.STATE_READY -> "READY"
            Player.STATE_ENDED -> "ENDED"
            else -> "UNKNOWN($state)"
        }
        Log.i(TAG, "state=$label mediaPos=${eventTime.currentPlaybackPositionMs}ms")
        pushState("State -> $label @ ${eventTime.currentPlaybackPositionMs}ms") {
            it.copy(playbackStateLabel = label)
        }
    }

    override fun onVideoInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?
    ) {
        val mime = format.sampleMimeType ?: "-"
        val codecs = format.codecs ?: "-"
        val fpsText = if (format.frameRate > 0f) String.format(Locale.US, "%.2ffps", format.frameRate) else "unknown fps"
        val bitrateText = if (format.bitrate > 0) "${format.bitrate / 1000}kbps" else "unknown bitrate"
        Log.i(
            TAG,
            "videoFormat mime=$mime codecs=$codecs res=${format.width}x${format.height} fps=$fpsText bitrate=$bitrateText"
        )
        val encSw = StreamPropertyExtractor.extractEncodingSoftware(format)
        pushState("Video: ${format.width}x${format.height} $mime ($codecs) $fpsText $bitrateText") {
            it.copy(
                videoMimeType = mime,
                videoCodec = codecs,
                width = format.width.coerceAtLeast(0),
                height = format.height.coerceAtLeast(0),
                frameRate = format.frameRate,
                bitrateBps = format.bitrate,
                videoFormat = format,
                encodingSoftware = encSw ?: it.encodingSoftware
            )
        }
    }

    override fun onAudioInputFormatChanged(
        eventTime: AnalyticsListener.EventTime,
        format: Format,
        decoderReuseEvaluation: DecoderReuseEvaluation?
    ) {
        val mime = format.sampleMimeType ?: "-"
        val codecs = format.codecs ?: "-"
        Log.i(TAG, "audioFormat mime=$mime codecs=$codecs ch=${format.channelCount} sr=${format.sampleRate}")
        val encSw = StreamPropertyExtractor.extractEncodingSoftware(format)
        pushState("Audio: $mime ($codecs) ${format.channelCount}ch ${format.sampleRate}Hz") {
            it.copy(
                audioMimeType = mime,
                audioCodec = codecs,
                audioFormat = format,
                encodingSoftware = it.encodingSoftware ?: encSw
            )
        }
    }

    override fun onVideoDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long
    ) {
        Log.i(TAG, "videoDecoderInitialized name=$decoderName initMs=$initializationDurationMs")
        pushState("Video Decoder: $decoderName (${initializationDurationMs}ms)") {
            it.copy(selectedVideoDecoder = decoderName, lastDecoderError = null)
        }
    }

    override fun onAudioDecoderInitialized(
        eventTime: AnalyticsListener.EventTime,
        decoderName: String,
        initializedTimestampMs: Long,
        initializationDurationMs: Long
    ) {
        Log.i(TAG, "audioDecoderInitialized name=$decoderName initMs=$initializationDurationMs")
        pushState("Audio Decoder: $decoderName (${initializationDurationMs}ms)") {
            it.copy(selectedAudioDecoder = decoderName)
        }
    }

    override fun onRenderedFirstFrame(
        eventTime: AnalyticsListener.EventTime,
        output: Any,
        renderTimeMs: Long
    ) {
        Log.i(TAG, "renderedFirstFrame output=${output.javaClass.simpleName} surface=${current.surfaceMode.name}")
        pushState("First Frame Rendered (${current.surfaceMode.name})") {
            it.copy(firstFrameRendered = true, lastRenderError = null)
        }
    }

    override fun onVideoSizeChanged(eventTime: AnalyticsListener.EventTime, videoSize: VideoSize) {
        if (videoSize.width > 0 && videoSize.height > 0) {
            Log.i(TAG, "videoSize=${videoSize.width}x${videoSize.height} pixelRatio=${videoSize.pixelWidthHeightRatio}")
            pushState(null) {
                it.copy(width = videoSize.width, height = videoSize.height)
            }
        }
    }

    override fun onDroppedVideoFrames(
        eventTime: AnalyticsListener.EventTime,
        droppedFrames: Int,
        elapsedMs: Long
    ) {
        val total = current.droppedFramesTotal + droppedFrames
        Log.w(TAG, "droppedVideoFrames=$droppedFrames elapsedMs=$elapsedMs total=$total")
        pushState("Dropped frames: +$droppedFrames (total $total)") {
            it.copy(droppedFramesTotal = total)
        }
    }

    override fun onVideoCodecError(eventTime: AnalyticsListener.EventTime, videoCodecError: Exception) {
        val msg = "${videoCodecError.javaClass.simpleName}: ${videoCodecError.message ?: "unknown"}"
        Log.e(TAG, "videoCodecError: $msg", videoCodecError)
        pushState("Video Codec Error: $msg") {
            it.copy(lastDecoderError = msg)
        }
    }

    override fun onPlayerError(eventTime: AnalyticsListener.EventTime, error: PlaybackException) {
        val msg = "${error.errorCodeName}: ${error.cause?.message ?: error.message ?: "unknown"}"
        Log.e(TAG, "playerError: $msg", error)
        pushState("Player Error: $msg") {
            it.copy(lastRenderError = msg)
        }
    }
}
