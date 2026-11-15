package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecRenderer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.upstream.DefaultAllocator
import androidx.media3.exoplayer.util.EventLogger
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.MediaItem
import com.example.player.CapabilityAwareMediaCodecSelector
import com.example.player.DecoderStrategy
import com.example.player.DeviceCapabilityInspector
import com.example.player.PlaybackDiagnosticsCollector
import com.example.player.PlaybackDiagnosticsState
import com.example.player.RenderSurfaceMode
import com.example.player.StreamPropertyExtractor
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import androidx.media3.common.MediaItem as Media3MediaItem

@OptIn(UnstableApi::class)
@Composable
fun MxVideoPlayerDialog(
    item: MediaItem,
    isBangla: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    BackHandler(onBack = onDismiss)

    val deviceProfile = remember(context) { DeviceCapabilityInspector.inspect(context) }
    val audioManager = remember(context) { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember(audioManager) { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Playback & recovery states
    var isPlayerPrepared by remember(item.uri, item.path) { mutableStateOf(false) }
    var isPlaying by remember(item.uri, item.path) { mutableStateOf(false) }
    var isBuffering by remember(item.uri, item.path) { mutableStateOf(true) }
    var playbackError by remember(item.uri, item.path) { mutableStateOf<String?>(null) }
    var reloadTrigger by remember(item.uri, item.path) { mutableIntStateOf(0) }

    var surfaceMode by remember(item.uri, item.path) { mutableStateOf(deviceProfile.recommendedSurfaceMode) }
    var decoderStrategy by remember(item.uri, item.path) { mutableStateOf(DecoderStrategy.HARDWARE_FIRST) }
    var failedDecoderNames by remember(item.uri, item.path) { mutableStateOf<Set<String>>(emptySet()) }
    var resumePositionMs by remember(item.uri, item.path) { mutableLongStateOf(0L) }
    var autoSurfaceRetryAttempted by remember(item.uri, item.path) { mutableStateOf(false) }

    var currentPositionMs by remember(item.uri, item.path) { mutableLongStateOf(0L) }
    var totalDurationMs by remember(item.uri, item.path) { mutableLongStateOf(item.duration.coerceAtLeast(1000L)) }
    var videoWidth by remember(item.uri, item.path) { mutableIntStateOf(item.width.coerceAtLeast(1)) }
    var videoHeight by remember(item.uri, item.path) { mutableIntStateOf(item.height.coerceAtLeast(1)) }

    // Controls & modal states
    var controlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var currentAspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var showAudioSubDialog by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    var qualityOptions by remember(item.uri, item.path) {
        mutableStateOf(
            listOf(
                VideoQualityOption(
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
            )
        )
    }
    var selectedQualityId by remember(item.uri, item.path) { mutableStateOf("auto") }
    var audioTrackOptions by remember(item.uri, item.path) { mutableStateOf<List<AudioTrackOption>>(emptyList()) }
    var subtitleTrackOptions by remember(item.uri, item.path) { mutableStateOf<List<SubtitleTrackOption>>(emptyList()) }
    var externalSubtitleUri by remember(item.uri, item.path) { mutableStateOf<Uri?>(null) }
    var externalSubtitleName by remember(item.uri, item.path) { mutableStateOf<String?>(null) }

    var diagnosticsState by remember(item.uri, item.path) {
        mutableStateOf(
            PlaybackDiagnosticsState(
                surfaceMode = surfaceMode,
                decoderStrategy = decoderStrategy
            )
        )
    }

    // Gesture HUD Overlays
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var currentVolumePercent by remember { mutableIntStateOf(0) }
    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var currentBrightnessPercent by remember { mutableIntStateOf(50) }
    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDeltaSeconds by remember { mutableIntStateOf(0) }
    var seekTargetMs by remember { mutableLongStateOf(0L) }
    var showRewindFeedback by remember { mutableStateOf(false) }
    var showForwardFeedback by remember { mutableStateOf(false) }

    // External Subtitle Document Picker (.srt, .vtt, .ass, .ssa)
    val subtitlePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            externalSubtitleUri = uri
            externalSubtitleName = uri.lastPathSegment?.substringAfterLast('/') ?: "External Subtitle"
        }
    }

    // Single capability-aware ExoPlayer instance; recreated only when media, decoder strategy, external sub, or retry changes
    val exoPlayer = remember(
        item.uri,
        item.path,
        reloadTrigger,
        decoderStrategy,
        failedDecoderNames,
        externalSubtitleUri
    ) {
        val codecSelector = CapabilityAwareMediaCodecSelector(
            strategy = decoderStrategy,
            failedDecoderNames = failedDecoderNames
        )

        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setMediaCodecSelector(codecSelector)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
            .forceEnableMediaCodecAsynchronousQueueing()

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setMaxVideoFrameRate(Int.MAX_VALUE)
                    .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                    .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .setAllowVideoNonSeamlessAdaptiveness(true)
                    .setAllowAudioMixedMimeTypeAdaptiveness(true)
                    .setAllowAudioMixedSampleRateAdaptiveness(true)
                    .setAllowAudioMixedChannelCountAdaptiveness(true)
                    .setAllowAudioMixedDecoderSupportAdaptiveness(true)
                    .build()
            )
        }

        val bufferCfg = deviceProfile.bufferConfig
        val loadControl = DefaultLoadControl.Builder()
            .setAllocator(DefaultAllocator(true, C.DEFAULT_BUFFER_SEGMENT_SIZE))
            .setBufferDurationsMs(
                bufferCfg.minBufferMs,
                bufferCfg.maxBufferMs,
                bufferCfg.bufferForPlaybackMs,
                bufferCfg.bufferForPlaybackAfterRebufferMs
            )
            .setTargetBufferBytes(bufferCfg.targetBufferBytes)
            .setPrioritizeTimeOverSizeThresholds(true)
            .setBackBuffer(bufferCfg.backBufferDurationMs, false)
            .build()

        val extractorsFactory = DefaultExtractorsFactory()
            .setConstantBitrateSeekingEnabled(true)

        val mediaSourceFactory = DefaultMediaSourceFactory(context, extractorsFactory)

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
                playbackParameters = PlaybackParameters(playbackSpeed)
                playWhenReady = true
            }
    }

    fun safePlayPause() {
        try {
            if (exoPlayer.playbackState == Player.STATE_ENDED) {
                exoPlayer.seekTo(0L)
                exoPlayer.play()
                isPlaying = true
            } else if (exoPlayer.isPlaying) {
                exoPlayer.pause()
                isPlaying = false
            } else {
                exoPlayer.play()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun safeSeekTo(msec: Long) {
        if (!isPlayerPrepared) return
        try {
            val target = msec.coerceIn(0L, totalDurationMs)
            exoPlayer.seekTo(target)
            currentPositionMs = target
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun safeSetSpeed(speed: Float) {
        try {
            exoPlayer.playbackParameters = PlaybackParameters(speed)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectVideoQuality(option: VideoQualityOption) {
        if (option.id == selectedQualityId) return
        val videoTrackCount = qualityOptions.count { it.trackGroup != null }
        if (videoTrackCount <= 1) {
            selectedQualityId = option.id
            return
        }
        try {
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
            if (option.trackGroup == null || option.trackIndex < 0) {
                builder
                    .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                selectedQualityId = "auto"
            } else {
                builder.setOverrideForType(
                    TrackSelectionOverride(option.trackGroup.mediaTrackGroup, option.trackIndex)
                )
                selectedQualityId = option.id
            }
            exoPlayer.trackSelectionParameters = builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectAudioTrack(option: AudioTrackOption) {
        val group = option.trackGroup ?: return
        if (option.trackIndex < 0) return
        try {
            exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                .buildUpon()
                .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, option.trackIndex))
                .build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun selectSubtitleTrack(option: SubtitleTrackOption) {
        try {
            val builder = exoPlayer.trackSelectionParameters.buildUpon()
            if (option.isOffOption || option.trackGroup == null || option.trackIndex < 0) {
                builder
                    .clearOverridesOfType(C.TRACK_TYPE_TEXT)
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, true)
            } else {
                builder
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setOverrideForType(TrackSelectionOverride(option.trackGroup.mediaTrackGroup, option.trackIndex))
            }
            exoPlayer.trackSelectionParameters = builder.build()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    var hasRenderedFirstFrame by remember(exoPlayer) { mutableStateOf(false) }
    var hasVideoTracks by remember(exoPlayer) { mutableStateOf(false) }
    var latestTracks by remember(exoPlayer) { mutableStateOf<Tracks?>(null) }

    // Bind media, listeners, EventLogger & DiagnosticsCollector; release strictly on dispose
    DisposableEffect(exoPlayer) {
        isPlayerPrepared = false
        isBuffering = true
        playbackError = null
        hasRenderedFirstFrame = false
        hasVideoTracks = false

        val eventLogger = EventLogger("AuraExoEvent")
        val diagnosticsCollector = PlaybackDiagnosticsCollector(
            initialSurfaceMode = surfaceMode,
            initialStrategy = decoderStrategy,
            onStateChanged = { nextDiag ->
                diagnosticsState = nextDiag.copy(
                    surfaceMode = surfaceMode,
                    decoderStrategy = decoderStrategy
                )
            }
        )

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> isBuffering = true
                    Player.STATE_READY -> {
                        isPlayerPrepared = true
                        isBuffering = false
                        playbackError = null
                        val dur = exoPlayer.duration
                        if (dur > 0L && dur != C.TIME_UNSET) {
                            totalDurationMs = dur
                        }
                    }
                    Player.STATE_ENDED -> {
                        isPlaying = false
                        isBuffering = false
                        controlsVisible = true
                        currentPositionMs = totalDurationMs
                    }
                    Player.STATE_IDLE -> isBuffering = false
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onRenderedFirstFrame() {
                hasRenderedFirstFrame = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                latestTracks = tracks
                val videoGroups = tracks.groups.filter { it.type == C.TRACK_TYPE_VIDEO && it.length > 0 }
                hasVideoTracks = videoGroups.isNotEmpty()
                if (videoGroups.isNotEmpty() && videoGroups.none { it.isSelected } && decoderStrategy == DecoderStrategy.HARDWARE_FIRST) {
                    resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                    playbackError = null
                    isBuffering = true
                    decoderStrategy = DecoderStrategy.SOFTWARE_FALLBACK
                    return
                }

                val extracted = extractVideoQualityOptions(tracks, isBangla)
                qualityOptions = extracted
                audioTrackOptions = extractAudioTrackOptions(tracks, isBangla)
                subtitleTrackOptions = extractSubtitleTrackOptions(tracks, isBangla)

                val videoTrackCount = extracted.count { it.trackGroup != null }
                if (videoTrackCount <= 1) return

                val hasVideoOverride = exoPlayer.trackSelectionParameters.overrides.values.any {
                    it.type == C.TRACK_TYPE_VIDEO
                }
                if (!hasVideoOverride && selectedQualityId != "auto") {
                    val target = extracted.firstOrNull { it.id == selectedQualityId }
                    if (target?.trackGroup != null && target.trackIndex >= 0) {
                        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
                            .buildUpon()
                            .setOverrideForType(
                                TrackSelectionOverride(target.trackGroup.mediaTrackGroup, target.trackIndex)
                            )
                            .build()
                    } else {
                        selectedQualityId = "auto"
                    }
                } else if (!hasVideoOverride) {
                    selectedQualityId = "auto"
                } else {
                    val activeTrackOption = extracted.firstOrNull { opt ->
                        opt.trackGroup?.isTrackSelected(opt.trackIndex) == true
                    }
                    if (activeTrackOption != null) {
                        selectedQualityId = activeTrackOption.id
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                val isDecoderFailure = error.errorCode == PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED ||
                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES

                val decoderInitException = error.cause as? MediaCodecRenderer.DecoderInitializationException
                val failedCodecName = decoderInitException?.codecInfo?.name
                if (!failedCodecName.isNullOrBlank()) {
                    failedDecoderNames = failedDecoderNames + failedCodecName
                }

                if (decoderStrategy == DecoderStrategy.HARDWARE_FIRST && isDecoderFailure) {
                    resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                    playbackError = null
                    isBuffering = true
                    decoderStrategy = DecoderStrategy.SOFTWARE_FALLBACK
                    return
                }

                if (decoderStrategy == DecoderStrategy.SOFTWARE_FALLBACK && isDecoderFailure && selectedQualityId != "auto") {
                    resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                    selectedQualityId = "auto"
                    playbackError = null
                    isBuffering = true
                    reloadTrigger++
                    return
                }

                isPlayerPrepared = false
                isPlaying = false
                isBuffering = false
                playbackError = if (isBangla) {
                    "ভিডিওটি চালানো সম্ভব হয়নি (${error.errorCodeName})"
                } else {
                    "Unable to play video (${error.errorCodeName})"
                }
            }
        }

        exoPlayer.addListener(listener)
        exoPlayer.addAnalyticsListener(eventLogger)
        exoPlayer.addAnalyticsListener(diagnosticsCollector)

        try {
            val resolvedUri = resolvePlayableUri(item)
            val mediaItemBuilder = Media3MediaItem.Builder().setUri(resolvedUri)
            externalSubtitleUri?.let { subUri ->
                val subConfig = Media3MediaItem.SubtitleConfiguration.Builder(subUri)
                    .setMimeType(detectSubtitleMimeType(subUri, externalSubtitleName))
                    .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                    .build()
                mediaItemBuilder.setSubtitleConfigurations(listOf(subConfig))
            }
            exoPlayer.setMediaItem(mediaItemBuilder.build())
            if (resumePositionMs > 0L) {
                exoPlayer.seekTo(resumePositionMs)
            }
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
            isBuffering = false
            playbackError = if (isBangla) "ভিডিও সোর্স পাওয়া যায়নি" else "Video source unavailable"
        }

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.removeAnalyticsListener(eventLogger)
            exoPlayer.removeAnalyticsListener(diagnosticsCollector)
            exoPlayer.clearVideoSurface()
            exoPlayer.release()
        }
    }

    // Watchdog for black-screen hardware decoders / incompatible surface:
    // Stage 1: If SurfaceView + HW decoder produces audio for 1.5s without rendering a frame, switch to TextureView or SW fallback
    LaunchedEffect(
        exoPlayer,
        isPlayerPrepared,
        isPlaying,
        hasRenderedFirstFrame,
        hasVideoTracks,
        decoderStrategy,
        surfaceMode,
        autoSurfaceRetryAttempted
    ) {
        if (isPlayerPrepared && isPlaying && hasVideoTracks && !hasRenderedFirstFrame) {
            var activePlayMs = 0L
            while (!hasRenderedFirstFrame && exoPlayer.isPlaying && activePlayMs < 1500L) {
                delay(250)
                activePlayMs += 250L
            }
            if (!hasRenderedFirstFrame && hasVideoTracks && exoPlayer.isPlaying) {
                if (surfaceMode == RenderSurfaceMode.SURFACE_VIEW && !autoSurfaceRetryAttempted) {
                    autoSurfaceRetryAttempted = true
                    surfaceMode = RenderSurfaceMode.TEXTURE_VIEW
                } else if (decoderStrategy == DecoderStrategy.HARDWARE_FIRST) {
                    resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                    playbackError = null
                    isBuffering = true
                    decoderStrategy = DecoderStrategy.SOFTWARE_FALLBACK
                }
            }
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(
        controlsVisible,
        isPlaying,
        isScreenLocked,
        showQualityDialog,
        showAudioSubDialog,
        showDiagnosticsDialog,
        showSpeedMenu
    ) {
        if (controlsVisible && isPlaying && !isScreenLocked && !showQualityDialog && !showAudioSubDialog && !showDiagnosticsDialog && !showSpeedMenu) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Progress ticker loop
    LaunchedEffect(exoPlayer, isPlayerPrepared, isPlaying) {
        while (true) {
            if (isPlayerPrepared && isPlaying) {
                try {
                    val pos = exoPlayer.currentPosition
                    if (pos >= 0L) {
                        currentPositionMs = pos
                    }
                    val dur = exoPlayer.duration
                    if (dur > 0L && dur != C.TIME_UNSET) {
                        totalDurationMs = dur
                    }
                } catch (_: Exception) {
                }
            }
            delay(250)
        }
    }

    // Keep screen on & enable immersive system bars while full-screen player is active
    DisposableEffect(activity, view) {
        val window = activity?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val insetsController = window?.let { WindowCompat.getInsetsController(it, view) }
        insetsController?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController?.hide(WindowInsetsCompat.Type.systemBars())

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            // Re-create PlayerView cleanly when surfaceMode switches between SurfaceView and TextureView
            key(surfaceMode) {
                AndroidView(
                    factory = { ctx ->
                        val layoutId = if (surfaceMode == RenderSurfaceMode.SURFACE_VIEW) {
                            R.layout.mx_player_surface_view
                        } else {
                            R.layout.mx_player_view
                        }
                        (LayoutInflater.from(ctx).inflate(layoutId, null, false) as PlayerView).apply {
                            player = exoPlayer
                            useController = false
                            keepScreenOn = true
                            setShutterBackgroundColor(android.graphics.Color.BLACK)
                            if (Build.VERSION.SDK_INT >= 34) {
                                setEnableComposeSurfaceSyncWorkaround(true)
                            }
                        }
                    },
                    update = { playerView ->
                        if (playerView.player !== exoPlayer) {
                            playerView.player = exoPlayer
                        }
                        playerView.resizeMode = when (currentAspectRatioMode) {
                            AspectRatioMode.FIT -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                            AspectRatioMode.CROP_FILL -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            AspectRatioMode.STRETCH -> AspectRatioFrameLayout.RESIZE_MODE_FILL
                            AspectRatioMode.ORIGINAL -> AspectRatioFrameLayout.RESIZE_MODE_FIT
                        }
                    },
                    onRelease = { playerView ->
                        playerView.player = null
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Buffering Spinner
            if (isBuffering && playbackError == null) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        color = NeonCyan,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(52.dp)
                    )
                }
            }

            // Error Overlay
            if (playbackError != null) {
                PlaybackErrorOverlay(
                    playbackError = playbackError ?: "Error",
                    fileName = item.name,
                    isBangla = isBangla,
                    onRetry = {
                        resumePositionMs = currentPositionMs.coerceAtLeast(0L)
                        reloadTrigger++
                    },
                    onOpenDiagnostics = { showDiagnosticsDialog = true },
                    onDismiss = onDismiss
                )
            }

            // Gesture Detection Layer (only active when no blocking error overlay is shown)
            if (playbackError == null) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isScreenLocked, isPlayerPrepared) {
                            if (isScreenLocked) {
                                detectTapGestures { controlsVisible = !controlsVisible }
                            } else {
                                detectTapGestures(
                                    onTap = { controlsVisible = !controlsVisible },
                                    onDoubleTap = { offset ->
                                        if (!isPlayerPrepared) return@detectTapGestures
                                        val isRightHalf = offset.x > (containerWidth / 2f)
                                        if (isRightHalf) {
                                            safeSeekTo(currentPositionMs + 10000L)
                                            showForwardFeedback = true
                                            scope.launch {
                                                delay(650)
                                                showForwardFeedback = false
                                            }
                                        } else {
                                            safeSeekTo(currentPositionMs - 10000L)
                                            showRewindFeedback = true
                                            scope.launch {
                                                delay(650)
                                                showRewindFeedback = false
                                            }
                                        }
                                    }
                                )
                            }
                        }
                        .pointerInput(isScreenLocked, isPlayerPrepared) {
                            if (!isScreenLocked) {
                                var dragMode = 0
                                var totalDragX = 0f
                                var totalDragY = 0f
                                var initialSeekPos = 0L

                                detectDragGestures(
                                    onDragStart = {
                                        totalDragX = 0f
                                        totalDragY = 0f
                                        dragMode = 0
                                        initialSeekPos = currentPositionMs
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        totalDragX += dragAmount.x
                                        totalDragY += dragAmount.y

                                        if (dragMode == 0) {
                                            if (abs(totalDragX) > 30f && abs(totalDragX) > abs(totalDragY)) {
                                                dragMode = 3
                                            } else if (abs(totalDragY) > 30f) {
                                                dragMode = if (change.position.x > containerWidth / 2f) 1 else 2
                                            }
                                        }

                                        when (dragMode) {
                                            1 -> {
                                                val step = (-dragAmount.y / containerHeight) * maxVolume
                                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val newVol = (currentVol + step).toInt().coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                currentVolumePercent = (newVol * 100) / maxVolume
                                                showVolumeOverlay = true
                                            }
                                            2 -> {
                                                val window = activity?.window
                                                val lp = window?.attributes
                                                val cur = if (lp?.screenBrightness != null && lp.screenBrightness >= 0f) lp.screenBrightness else 0.5f
                                                val delta = -dragAmount.y / (containerHeight * 0.7f)
                                                val newBrightness = (cur + delta).coerceIn(0.05f, 1.0f)
                                                lp?.screenBrightness = newBrightness
                                                window?.attributes = lp
                                                currentBrightnessPercent = (newBrightness * 100).toInt()
                                                showBrightnessOverlay = true
                                            }
                                            3 -> {
                                                if (isPlayerPrepared) {
                                                    val duration = totalDurationMs.coerceAtLeast(1000L)
                                                    val seekDeltaMs = ((totalDragX / containerWidth) * 90000L).toLong()
                                                    val target = (initialSeekPos + seekDeltaMs).coerceIn(0L, duration)
                                                    seekTargetMs = target
                                                    seekDeltaSeconds = ((target - initialSeekPos) / 1000).toInt()
                                                    showSeekOverlay = true
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (dragMode == 3 && showSeekOverlay) {
                                            safeSeekTo(seekTargetMs)
                                        }
                                        showVolumeOverlay = false
                                        showBrightnessOverlay = false
                                        showSeekOverlay = false
                                        dragMode = 0
                                    },
                                    onDragCancel = {
                                        showVolumeOverlay = false
                                        showBrightnessOverlay = false
                                        showSeekOverlay = false
                                        dragMode = 0
                                    }
                                )
                            }
                        }
                )
            }

            // Gesture HUD Overlays
            AnimatedVisibility(visible = showVolumeOverlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.8f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Icon(
                            imageVector = when {
                                currentVolumePercent == 0 -> Icons.AutoMirrored.Filled.VolumeMute
                                currentVolumePercent < 50 -> Icons.AutoMirrored.Filled.VolumeDown
                                else -> Icons.AutoMirrored.Filled.VolumeUp
                            },
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(44.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Volume: $currentVolumePercent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showBrightnessOverlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.8f), border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        Icon(imageVector = Icons.Default.BrightnessHigh, contentDescription = null, tint = GoldenVault, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Brightness: $currentBrightnessPercent%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            AnimatedVisibility(visible = showSeekOverlay, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.align(Alignment.Center)) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.Black.copy(alpha = 0.85f), border = BorderStroke(1.5.dp, ElectricIndigo)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)) {
                        val sign = if (seekDeltaSeconds >= 0) "+${seekDeltaSeconds}s" else "${seekDeltaSeconds}s"
                        Text(text = sign, color = ElectricIndigo, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = String.format(
                                Locale.US,
                                "%02d:%02d / %02d:%02d",
                                (seekTargetMs / 1000) / 60,
                                (seekTargetMs / 1000) % 60,
                                (totalDurationMs / 1000) / 60,
                                (totalDurationMs / 1000) % 60
                            ),
                            color = Color.White,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (showRewindFeedback) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Replay10, contentDescription = "-10s", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            if (showForwardFeedback) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(16.dp)
                ) {
                    Icon(Icons.Default.Forward10, contentDescription = "+10s", tint = Color.White, modifier = Modifier.size(36.dp))
                }
            }

            // Screen Lock Floating Button
            if (isScreenLocked) {
                IconButton(
                    onClick = { isScreenLocked = false },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .statusBarsPadding()
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .testTag("unlock_screen_btn")
                ) {
                    Icon(Icons.Default.Lock, contentDescription = "Unlock Controls", tint = GoldenVault, modifier = Modifier.size(24.dp))
                }
            }

            // On-screen Controls
            AnimatedVisibility(
                visible = controlsVisible && !isScreenLocked && playbackError == null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.fillMaxSize()
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top Control Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent)
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 6.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss, modifier = Modifier.testTag("mx_back_btn")) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }

                        Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            Text(
                                text = item.name,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val shortSide = minOf(videoWidth, videoHeight)
                                val longSide = maxOf(videoWidth, videoHeight)
                                val qualityBadge = when {
                                    shortSide >= 2160 || longSide >= 3840 -> "4K UHD"
                                    shortSide >= 1440 || longSide >= 2560 -> "2K QHD"
                                    shortSide >= 1080 || longSide >= 1920 -> "1080p FHD"
                                    shortSide >= 720 -> "720p HD"
                                    else -> "SD"
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(NeonCyan.copy(alpha = 0.25f))
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(text = qualityBadge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeonCyan)
                                }

                                val modeTag = buildString {
                                    append(" • ${videoWidth}×${videoHeight}")
                                    append(if (surfaceMode == RenderSurfaceMode.SURFACE_VIEW) " • SV" else " • TV")
                                    if (decoderStrategy == DecoderStrategy.SOFTWARE_FALLBACK) append(" SW")
                                }
                                Text(
                                    text = modeTag,
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            val selectedQualityOption = qualityOptions.firstOrNull { it.id == selectedQualityId }
                            val qualityBtnLabel = if (selectedQualityOption == null || selectedQualityOption.id == "auto") {
                                val (_, activeShort) = formatQualityLabels(videoWidth, videoHeight)
                                if (isBangla) "অটো ($activeShort)" else "Auto ($activeShort)"
                            } else {
                                selectedQualityOption.shortLabel
                            }

                            Surface(
                                onClick = { showQualityDialog = true },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selectedQualityId != "auto") NeonCyan.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.14f),
                                border = BorderStroke(1.dp, if (selectedQualityId != "auto") NeonCyan else Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .padding(end = 2.dp)
                                    .testTag("mx_quality_btn")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.HighQuality,
                                        contentDescription = if (isBangla) "ভিডিও কোয়ালিটি" else "Video Quality",
                                        tint = if (selectedQualityId != "auto") NeonCyan else Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = qualityBtnLabel,
                                        color = if (selectedQualityId != "auto") NeonCyan else Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            IconButton(
                                onClick = { showAudioSubDialog = true },
                                modifier = Modifier.testTag("mx_audio_sub_btn")
                            ) {
                                Icon(Icons.Default.Subtitles, contentDescription = "Audio & Subtitles", tint = Color.White)
                            }

                            IconButton(
                                onClick = { showDiagnosticsDialog = true },
                                modifier = Modifier.testTag("mx_diagnostics_btn")
                            ) {
                                Icon(Icons.Default.Info, contentDescription = "Playback Diagnostics", tint = Color.White)
                            }

                            IconButton(
                                onClick = {
                                    currentAspectRatioMode = when (currentAspectRatioMode) {
                                        AspectRatioMode.FIT -> AspectRatioMode.CROP_FILL
                                        AspectRatioMode.CROP_FILL -> AspectRatioMode.STRETCH
                                        AspectRatioMode.STRETCH -> AspectRatioMode.ORIGINAL
                                        AspectRatioMode.ORIGINAL -> AspectRatioMode.FIT
                                    }
                                },
                                modifier = Modifier.testTag("mx_aspect_ratio_btn")
                            ) {
                                Icon(Icons.Default.AspectRatio, contentDescription = "Aspect Ratio", tint = Color.White)
                            }

                            Box {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.testTag("mx_speed_btn")
                                ) {
                                    Text(text = "${playbackSpeed}x", color = GoldenVault, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                DropdownMenu(expanded = showSpeedMenu, onDismissRequest = { showSpeedMenu = false }) {
                                    listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f).forEach { speed ->
                                        DropdownMenuItem(
                                            text = { Text("${speed}x ${if (speed == 1.0f) "(Normal)" else ""}") },
                                            onClick = {
                                                playbackSpeed = speed
                                                safeSetSpeed(speed)
                                                showSpeedMenu = false
                                            }
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { isScreenLocked = true },
                                modifier = Modifier.testTag("mx_lock_screen_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Lock Screen", tint = Color.White)
                            }
                        }
                    }

                    // Center Play / Pause Button
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.65f))
                            .pointerInput(isPlayerPrepared) {
                                detectTapGestures { safePlayPause() }
                            }
                            .testTag("mx_center_play_pause_btn")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp)
                        )
                    }

                    // Bottom Control Bar
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f))
                                )
                            )
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "%d:%02d", (currentPositionMs / 1000) / 60, (currentPositionMs / 1000) % 60),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )

                            Text(
                                text = when (currentAspectRatioMode) {
                                    AspectRatioMode.FIT -> if (isBangla) "ফিট স্ক্রিন" else "Fit Screen"
                                    AspectRatioMode.CROP_FILL -> if (isBangla) "ফুল স্ক্রিন (ক্রপ)" else "Crop to Fill"
                                    AspectRatioMode.STRETCH -> if (isBangla) "স্ট্রেচ" else "Stretch"
                                    AspectRatioMode.ORIGINAL -> if (isBangla) "আসল অনুপাত" else "100% Original"
                                },
                                color = GoldenVault,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                text = String.format(Locale.US, "%d:%02d", (totalDurationMs / 1000) / 60, (totalDurationMs / 1000) % 60),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        val sliderPosition = if (totalDurationMs > 0) {
                            (currentPositionMs.toFloat() / totalDurationMs.toFloat()).coerceIn(0f, 1f)
                        } else 0f

                        Slider(
                            value = sliderPosition,
                            onValueChange = { fraction ->
                                val targetMs = (fraction * totalDurationMs).toLong()
                                currentPositionMs = targetMs
                                safeSeekTo(targetMs)
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = NeonCyan,
                                activeTrackColor = NeonCyan,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("mx_progress_slider")
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = { safeSeekTo(currentPositionMs - 10000L) }) {
                                Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(
                                onClick = { safePlayPause() },
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = "Play/Pause",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            IconButton(onClick = { safeSeekTo(currentPositionMs + 10000L) }) {
                                Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                            }
                        }
                    }
                }
            }

            if (showQualityDialog) {
                VideoQualitySelectionDialog(
                    qualityOptions = qualityOptions,
                    selectedQualityId = selectedQualityId,
                    videoWidth = videoWidth,
                    videoHeight = videoHeight,
                    isBangla = isBangla,
                    onSelectQuality = { selectVideoQuality(it) },
                    onDismiss = { showQualityDialog = false }
                )
            }

            if (showAudioSubDialog) {
                AudioSubtitleSelectionDialog(
                    audioTracks = audioTrackOptions,
                    subtitleTracks = subtitleTrackOptions,
                    externalSubtitleName = externalSubtitleName,
                    isBangla = isBangla,
                    onSelectAudio = { selectAudioTrack(it) },
                    onSelectSubtitle = { selectSubtitleTrack(it) },
                    onPickExternalSubtitle = {
                        resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                        subtitlePickerLauncher.launch(arrayOf("application/x-subrip", "text/vtt", "text/*", "*/*"))
                    },
                    onDismiss = { showAudioSubDialog = false }
                )
            }

            if (showDiagnosticsDialog) {
                val streamInfos = remember(latestTracks, diagnosticsState, totalDurationMs, isBangla) {
                    StreamPropertyExtractor.buildMediaStreamInfos(
                        tracks = latestTracks,
                        diagnostics = diagnosticsState,
                        fileSizeBytes = item.size,
                        durationMs = totalDurationMs,
                        isBangla = isBangla
                    )
                }
                PlaybackDiagnosticsDialog(
                    deviceProfile = deviceProfile,
                    diagnostics = diagnosticsState,
                    streamInfos = streamInfos,
                    isBangla = isBangla,
                    onSwitchSurfaceMode = { nextSurface ->
                        surfaceMode = nextSurface
                        diagnosticsState = diagnosticsState.copy(surfaceMode = nextSurface)
                    },
                    onSwitchDecoderStrategy = { nextStrategy ->
                        resumePositionMs = exoPlayer.currentPosition.coerceAtLeast(currentPositionMs).coerceAtLeast(0L)
                        failedDecoderNames = emptySet()
                        decoderStrategy = nextStrategy
                        playbackError = null
                        reloadTrigger++
                    },
                    onDismiss = { showDiagnosticsDialog = false }
                )
            }
        }
    }
}
