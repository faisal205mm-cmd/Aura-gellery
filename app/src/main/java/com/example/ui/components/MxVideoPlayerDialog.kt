package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.view.LayoutInflater
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.R
import com.example.data.model.MediaItem
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs
import androidx.media3.common.MediaItem as Media3MediaItem

enum class AspectRatioMode {
    FIT,        // Letterbox
    STRETCH,    // Stretch to exact screen size
    CROP_FILL,  // Zoom in to fill screen with no black bars
    ORIGINAL    // 1:1 original aspect ratio
}

private data class VideoQualityOption(
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

private fun extractVideoQualityOptions(tracks: Tracks, isBangla: Boolean): List<VideoQualityOption> {
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
        detail = if (isBangla) "সর্বোচ্চ ও স্থিতিশীল কোয়ালিটি নির্বাচন করে" else "Automatically picks best resolution",
        width = 0,
        height = 0,
        bitrate = 0,
        trackGroup = null,
        trackIndex = -1
    )

    return listOf(autoOption) + sortedTracks
}

private fun resolvePlayableUri(item: MediaItem): Uri {
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

@OptIn(UnstableApi::class)
@Composable
fun MxVideoPlayerDialog(
    item: MediaItem,
    isBangla: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1) }

    // Media3 ExoPlayer states
    var isPlayerPrepared by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var isBuffering by remember { mutableStateOf(true) }
    var playbackError by remember { mutableStateOf<String?>(null) }
    var reloadTrigger by remember { mutableIntStateOf(0) }

    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var totalDurationMs by remember { mutableLongStateOf(item.duration.coerceAtLeast(1000L)) }
    var videoWidth by remember { mutableIntStateOf(item.width.coerceAtLeast(1)) }
    var videoHeight by remember { mutableIntStateOf(item.height.coerceAtLeast(1)) }

    // MX Player Controls & Overlays
    var controlsVisible by remember { mutableStateOf(true) }
    var isScreenLocked by remember { mutableStateOf(false) }
    var currentAspectRatioMode by remember { mutableStateOf(AspectRatioMode.FIT) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    var showQualityDialog by remember { mutableStateOf(false) }
    var qualityOptions by remember {
        mutableStateOf(
            listOf(
                VideoQualityOption(
                    id = "auto",
                    label = if (isBangla) "অটো (স্বয়ংক্রিয়)" else "Auto (Recommended)",
                    shortLabel = if (isBangla) "অটো" else "Auto",
                    detail = if (isBangla) "সর্বোচ্চ ও স্থিতিশীল কোয়ালিটি নির্বাচন করে" else "Automatically picks best resolution",
                    width = 0,
                    height = 0,
                    bitrate = 0,
                    trackGroup = null,
                    trackIndex = -1
                )
            )
        )
    }
    var selectedQualityId by remember { mutableStateOf("auto") }

    // Gesture HUD Overlays (Volume, Brightness, Seek)
    var showVolumeOverlay by remember { mutableStateOf(false) }
    var currentVolumePercent by remember { mutableIntStateOf(0) }

    var showBrightnessOverlay by remember { mutableStateOf(false) }
    var currentBrightnessPercent by remember { mutableIntStateOf(50) }

    var showSeekOverlay by remember { mutableStateOf(false) }
    var seekDeltaSeconds by remember { mutableIntStateOf(0) }
    var seekTargetMs by remember { mutableLongStateOf(0L) }

    // Quick Double Tap Feedback (+10s / -10s)
    var showRewindFeedback by remember { mutableStateOf(false) }
    var showForwardFeedback by remember { mutableStateOf(false) }

    // Configure ExoPlayer with decoder fallback & unconstrained 1080p/4K/8K track selection
    val exoPlayer = remember(item.uri, item.path, reloadTrigger) {
        val renderersFactory = DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(Int.MAX_VALUE, Int.MAX_VALUE)
                    .setMaxVideoBitrate(Int.MAX_VALUE)
                    .setExceedVideoConstraintsIfNecessary(true)
                    .setExceedRendererCapabilitiesIfNecessary(true)
                    .setAllowVideoMixedMimeTypeAdaptiveness(true)
                    .build()
            )
        }

        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 2500,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 500,
                /* bufferForPlaybackAfterRebufferMs = */ 1500
            )
            .build()

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .setUsage(C.USAGE_MEDIA)
            .build()

        ExoPlayer.Builder(context, renderersFactory)
            .setTrackSelector(trackSelector)
            .setLoadControl(loadControl)
            .build()
            .apply {
                setAudioAttributes(audioAttributes, true)
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

    // Bind media item & listener, release cleanly on disposal or reload
    DisposableEffect(exoPlayer) {
        isPlayerPrepared = false
        isBuffering = true
        playbackError = null
        selectedQualityId = "auto"

        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_BUFFERING -> {
                        isBuffering = true
                    }
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
                    Player.STATE_IDLE -> {
                        isBuffering = false
                    }
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    videoWidth = videoSize.width
                    videoHeight = videoSize.height
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                val extracted = extractVideoQualityOptions(tracks, isBangla)
                qualityOptions = extracted
                val hasVideoOverride = exoPlayer.trackSelectionParameters.overrides.values.any {
                    it.type == C.TRACK_TYPE_VIDEO
                }
                if (!hasVideoOverride) {
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

        try {
            val resolvedUri = resolvePlayableUri(item)
            exoPlayer.setMediaItem(Media3MediaItem.fromUri(resolvedUri))
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
        } catch (e: Exception) {
            e.printStackTrace()
            isBuffering = false
            playbackError = if (isBangla) "ভিডিও সোর্স পাওয়া যায়নি" else "Video source unavailable"
        }

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    // Auto-hide controls timer (paused while quality or speed picker is open)
    LaunchedEffect(controlsVisible, isPlaying, isScreenLocked, showQualityDialog, showSpeedMenu) {
        if (controlsVisible && isPlaying && !isScreenLocked && !showQualityDialog && !showSpeedMenu) {
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

    // Keep screen on while playing video
    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val containerWidth = constraints.maxWidth.toFloat()
                val containerHeight = constraints.maxHeight.toFloat()

                // Media3 Hardware-Accelerated PlayerView (supports 1080p, 2K, 4K, HEVC, MKV, rotated camera videos)
                AndroidView(
                    factory = { ctx ->
                        (LayoutInflater.from(ctx).inflate(R.layout.mx_player_view, null, false) as PlayerView).apply {
                            player = exoPlayer
                            useController = false
                            keepScreenOn = true
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
                    modifier = Modifier.fillMaxSize()
                )

                // Buffering Spinner
                if (isBuffering && playbackError == null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            strokeWidth = 3.dp,
                            modifier = Modifier.size(52.dp)
                        )
                    }
                }

                // Error Card overlay if video cannot be played
                if (playbackError != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = playbackError ?: "Error",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = item.name,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 12.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    OutlinedButton(onClick = onDismiss) {
                                        Text(if (isBangla) "ফিরে যান" else "Close")
                                    }
                                    Button(
                                        onClick = { reloadTrigger++ },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(if (isBangla) "আবার চেষ্টা" else "Retry")
                                    }
                                }
                            }
                        }
                    }
                }

                // TOUCH & MX GESTURE DETECTION LAYER
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isScreenLocked, isPlayerPrepared) {
                            if (isScreenLocked) {
                                detectTapGestures {
                                    controlsVisible = !controlsVisible
                                }
                            } else {
                                detectTapGestures(
                                    onTap = {
                                        controlsVisible = !controlsVisible
                                    },
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
                                var dragMode = 0 // 0 = none, 1 = volume (right vertical), 2 = brightness (left vertical), 3 = seek (horizontal)
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
                                                dragMode = 3 // Horizontal Seek
                                            } else if (abs(totalDragY) > 30f) {
                                                dragMode = if (change.position.x > containerWidth / 2f) 1 else 2
                                            }
                                        }

                                        when (dragMode) {
                                            1 -> {
                                                // Volume Control (Right side)
                                                val step = (-dragAmount.y / containerHeight) * maxVolume
                                                val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                                                val newVol = (currentVol + step).toInt().coerceIn(0, maxVolume)
                                                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, 0)
                                                currentVolumePercent = (newVol * 100) / maxVolume
                                                showVolumeOverlay = true
                                            }
                                            2 -> {
                                                // Brightness Control (Left side)
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
                                                // Seek Control (Horizontal)
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

                // GESTURE HUD OVERLAYS (VOLUME, BRIGHTNESS, SEEK)
                // 1. Volume HUD
                AnimatedVisibility(
                    visible = showVolumeOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
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
                            Text(
                                text = "Volume: $currentVolumePercent%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // 2. Brightness HUD
                AnimatedVisibility(
                    visible = showBrightnessOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.BrightnessHigh,
                                contentDescription = null,
                                tint = GoldenVault,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Brightness: $currentBrightnessPercent%",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // 3. Horizontal Seek HUD
                AnimatedVisibility(
                    visible = showSeekOverlay,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.align(Alignment.Center)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.Black.copy(alpha = 0.85f),
                        border = BorderStroke(1.5.dp, ElectricIndigo)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 18.dp)
                        ) {
                            val sign = if (seekDeltaSeconds >= 0) "+${seekDeltaSeconds}s" else "${seekDeltaSeconds}s"
                            Text(
                                text = sign,
                                color = ElectricIndigo,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val targetMins = (seekTargetMs / 1000) / 60
                            val targetSecs = (seekTargetMs / 1000) % 60
                            val totalMins = (totalDurationMs / 1000) / 60
                            val totalSecs = (totalDurationMs / 1000) % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d / %02d:%02d", targetMins, targetSecs, totalMins, totalSecs),
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // 4. Double Tap Feedback (Left Rewind 10s / Right Fast-Forward 10s)
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

                // SCREEN LOCK FLOATING ICON (When Locked)
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

                // ON-SCREEN MX CONTROLS (TOP & BOTTOM BARS)
                AnimatedVisibility(
                    visible = controlsVisible && !isScreenLocked,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        // Top Bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.TopCenter)
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(Color.Black.copy(alpha = 0.8f), Color.Transparent)
                                    )
                                )
                                .statusBarsPadding()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss, modifier = Modifier.testTag("mx_back_btn")) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                            }

                            Column(modifier = Modifier.weight(1f).padding(horizontal = 6.dp)) {
                                Text(
                                    text = item.name,
                                    color = Color.White,
                                    fontSize = 15.sp,
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
                                        Text(
                                            text = qualityBadge,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    }

                                    Text(
                                        text = " • ${videoWidth}×${videoHeight} • Media3",
                                        color = Color.White.copy(alpha = 0.65f),
                                        fontSize = 11.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(start = 4.dp)
                                    )
                                }
                            }

                            // Quality Selector Button
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
                                border = BorderStroke(
                                    1.dp,
                                    if (selectedQualityId != "auto") NeonCyan else Color.White.copy(alpha = 0.3f)
                                ),
                                modifier = Modifier
                                    .padding(end = 4.dp)
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

                            // Aspect Ratio Mode Button
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

                            // Speed Selector
                            Box {
                                IconButton(
                                    onClick = { showSpeedMenu = true },
                                    modifier = Modifier.testTag("mx_speed_btn")
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${playbackSpeed}x",
                                            color = GoldenVault,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showSpeedMenu,
                                    onDismissRequest = { showSpeedMenu = false }
                                ) {
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

                            // Touch Lock Screen Button
                            IconButton(
                                onClick = { isScreenLocked = true },
                                modifier = Modifier.testTag("mx_lock_screen_btn")
                            ) {
                                Icon(Icons.Default.LockOpen, contentDescription = "Lock Screen", tint = Color.White)
                            }
                        }

                        // Center Play / Pause Big Button
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.65f))
                                .pointerInput(isPlayerPrepared) {
                                    detectTapGestures {
                                        safePlayPause()
                                    }
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
                            // Time Bar & Aspect Ratio Indicator
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val curSecs = currentPositionMs / 1000
                                val curMins = curSecs / 60
                                val curRemainder = curSecs % 60

                                val totSecs = totalDurationMs / 1000
                                val totMins = totSecs / 60
                                val totRemainder = totSecs % 60

                                Text(
                                    text = String.format(Locale.US, "%d:%02d", curMins, curRemainder),
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
                                    text = String.format(Locale.US, "%d:%02d", totMins, totRemainder),
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            // Scrub Slider
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

                            // Quick Controls Row (Rewind 10s, Play/Pause, Forward 10s)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        safeSeekTo(currentPositionMs - 10000L)
                                    }
                                ) {
                                    Icon(Icons.Default.Replay10, contentDescription = "Rewind 10s", tint = Color.White)
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                IconButton(
                                    onClick = {
                                        safePlayPause()
                                    },
                                    modifier = Modifier
                                        .size(44.dp)
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

                                IconButton(
                                    onClick = {
                                        safeSeekTo(currentPositionMs + 10000L)
                                    }
                                ) {
                                    Icon(Icons.Default.Forward10, contentDescription = "Forward 10s", tint = Color.White)
                                }
                            }
                        }
                    }
                }

                // MANUAL VIDEO QUALITY SELECTION MODAL OVERLAY
                if (showQualityDialog) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.65f))
                            .clickable { showQualityDialog = false }
                            .padding(24.dp)
                            .testTag("mx_quality_dialog")
                    ) {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 420.dp)
                                .clickable(enabled = false) {}
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.HighQuality,
                                            contentDescription = null,
                                            tint = NeonCyan,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (isBangla) "ভিডিও কোয়ালিটি নির্বাচন" else "Video Quality",
                                            color = Color.White,
                                            fontSize = 17.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = "${videoWidth}×${videoHeight}",
                                        color = GoldenVault,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isBangla) {
                                        "মিডিয়া সোর্সে উপলব্ধ রেজোলিউশন ট্র্যাক থেকে বেছে নিন"
                                    } else {
                                        "Switch between available resolution tracks from the media source"
                                    },
                                    color = Color.White.copy(alpha = 0.65f),
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                                Spacer(modifier = Modifier.height(8.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 280.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    qualityOptions.forEach { option ->
                                        val isSelected = option.id == selectedQualityId
                                        val isActiveAuto = option.id == "auto" && selectedQualityId == "auto"
                                        val detailText = if (option.id == "auto") {
                                            val (activeLabel, _) = formatQualityLabels(videoWidth, videoHeight)
                                            if (isBangla) "বর্তমান: $activeLabel (${videoWidth}×${videoHeight})" else "Active: $activeLabel (${videoWidth}×${videoHeight})"
                                        } else {
                                            option.detail
                                        }

                                        Surface(
                                            onClick = {
                                                selectVideoQuality(option)
                                                showQualityDialog = false
                                            },
                                            shape = RoundedCornerShape(12.dp),
                                            color = if (isSelected || isActiveAuto) {
                                                NeonCyan.copy(alpha = 0.16f)
                                            } else {
                                                Color.White.copy(alpha = 0.05f)
                                            },
                                            border = if (isSelected || isActiveAuto) {
                                                BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f))
                                            } else {
                                                null
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("quality_option_${option.id}")
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Text(
                                                            text = option.label,
                                                            color = if (isSelected || isActiveAuto) NeonCyan else Color.White,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                        if (!option.isSupported && option.id != "auto") {
                                                            Spacer(modifier = Modifier.width(6.dp))
                                                            Text(
                                                                text = if (isBangla) "(সফটওয়্যার)" else "(SW)",
                                                                color = GoldenVault,
                                                                fontSize = 10.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = detailText,
                                                        color = Color.White.copy(alpha = 0.65f),
                                                        fontSize = 11.sp
                                                    )
                                                }
                                                if (isSelected || isActiveAuto) {
                                                    Icon(
                                                        imageVector = Icons.Default.Check,
                                                        contentDescription = "Selected",
                                                        tint = NeonCyan,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    horizontalArrangement = Arrangement.End,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    TextButton(onClick = { showQualityDialog = false }) {
                                        Text(
                                            text = if (isBangla) "বন্ধ করুন" else "Close",
                                            color = Color.White.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
