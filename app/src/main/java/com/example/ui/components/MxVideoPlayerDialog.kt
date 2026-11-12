package com.example.ui.components

import android.app.Activity
import android.content.Context
import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build
import android.view.Surface
import android.view.TextureView
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BrightnessHigh
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.MediaItem
import com.example.ui.theme.ElectricIndigo
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.NeonCyan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale
import kotlin.math.abs

enum class AspectRatioMode {
    FIT,        // Letterbox
    STRETCH,    // Stretch to exact screen size
    CROP_FILL,  // Zoom in to fill screen with no black bars
    ORIGINAL    // 1:1 original pixel scale
}

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

    // Media Player states
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
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

    // Safe playback actions that NEVER call MediaPlayer in un-prepared state
    fun safePlayPause() {
        val player = mediaPlayer ?: return
        if (!isPlayerPrepared) return
        try {
            if (player.isPlaying) {
                player.pause()
                isPlaying = false
            } else {
                player.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun safeSeekTo(msec: Long) {
        val player = mediaPlayer ?: return
        if (!isPlayerPrepared) return
        try {
            val target = msec.coerceIn(0L, totalDurationMs).toInt()
            player.seekTo(target)
            currentPositionMs = target.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun safeSetSpeed(speed: Float) {
        val player = mediaPlayer ?: return
        if (!isPlayerPrepared) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val params = player.playbackParams ?: PlaybackParams()
                player.playbackParams = params.setSpeed(speed)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-hide controls timer
    LaunchedEffect(controlsVisible, isPlaying, isScreenLocked) {
        if (controlsVisible && isPlaying && !isScreenLocked) {
            delay(3500)
            controlsVisible = false
        }
    }

    // Safe progress ticker loop (ONLY polls when player is prepared)
    LaunchedEffect(mediaPlayer, isPlayerPrepared, isPlaying) {
        while (true) {
            val player = mediaPlayer
            if (player != null && isPlayerPrepared && isPlaying) {
                try {
                    if (player.isPlaying) {
                        currentPositionMs = player.currentPosition.toLong()
                        if (player.duration > 0) {
                            totalDurationMs = player.duration.toLong()
                        }
                    }
                } catch (e: Exception) {
                    // Ignore transient state exception during seeking
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
            try {
                mediaPlayer?.let { mp ->
                    if (isPlayerPrepared && isPlaying) {
                        mp.stop()
                    }
                    mp.reset()
                    mp.release()
                }
                mediaPlayer = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Dialog(
        onDismissRequest = {
            onDismiss()
        },
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

                // Native Hardware-Accelerated Video Surface with Safe Lifecycle
                AndroidView(
                    factory = { ctx ->
                        TextureView(ctx).apply {
                            surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                                    val surface = Surface(st)
                                    val mp = MediaPlayer().apply {
                                        setAudioAttributes(
                                            AudioAttributes.Builder()
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MOVIE)
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .build()
                                        )
                                        setSurface(surface)

                                        try {
                                            isBuffering = true
                                            playbackError = null
                                            isPlayerPrepared = false

                                            val fileOnDisk = if (item.path.isNotEmpty()) File(item.path) else null
                                            if (fileOnDisk != null && fileOnDisk.exists()) {
                                                setDataSource(item.path)
                                            } else if (item.uri.startsWith("android.resource://") || item.uri.startsWith("content://")) {
                                                setDataSource(ctx, Uri.parse(item.uri))
                                            } else if (item.uri.startsWith("http://") || item.uri.startsWith("https://")) {
                                                setDataSource(ctx, Uri.parse(item.uri))
                                            } else {
                                                setDataSource(ctx, Uri.parse(item.uri))
                                            }

                                            setOnPreparedListener { player ->
                                                try {
                                                    isPlayerPrepared = true
                                                    isBuffering = false
                                                    playbackError = null
                                                    videoWidth = player.videoWidth.takeIf { it > 0 } ?: 1920
                                                    videoHeight = player.videoHeight.takeIf { it > 0 } ?: 1080
                                                    if (player.duration > 0) {
                                                        totalDurationMs = player.duration.toLong().coerceAtLeast(1000L)
                                                    }
                                                    player.start()
                                                    isPlaying = true
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }

                                            setOnCompletionListener {
                                                isPlaying = false
                                                controlsVisible = true
                                                currentPositionMs = totalDurationMs
                                            }

                                            setOnErrorListener { player, what, extra ->
                                                isPlayerPrepared = false
                                                isPlaying = false
                                                isBuffering = false
                                                playbackError = if (isBangla) "ভিডিও চালানো সম্ভব হয়নি" else "Unable to load video stream"
                                                try {
                                                    player.reset()
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                                true
                                            }

                                            prepareAsync()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                            isBuffering = false
                                            playbackError = if (isBangla) "ভিডিও সোর্স পাওয়া যায়নি" else "Video source unavailable"
                                        }
                                    }
                                    mediaPlayer = mp
                                }

                                override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {}
                                override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                    try {
                                        isPlayerPrepared = false
                                        mediaPlayer?.reset()
                                        mediaPlayer?.release()
                                        mediaPlayer = null
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    return true
                                }
                                override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                            }
                        }
                    },
                    update = { textureView ->
                        // Apply aspect ratio transformation matrix
                        val matrix = Matrix()
                        val vw = videoWidth.coerceAtLeast(1).toFloat()
                        val vh = videoHeight.coerceAtLeast(1).toFloat()
                        val sx = containerWidth / vw
                        val sy = containerHeight / vh

                        when (currentAspectRatioMode) {
                            AspectRatioMode.FIT -> {
                                val scale = minOf(sx, sy)
                                matrix.setScale(scale / sx, scale / sy, containerWidth / 2f, containerHeight / 2f)
                            }
                            AspectRatioMode.CROP_FILL -> {
                                val scale = maxOf(sx, sy)
                                matrix.setScale(scale / sx, scale / sy, containerWidth / 2f, containerHeight / 2f)
                            }
                            AspectRatioMode.STRETCH -> {
                                matrix.setScale(1f, 1f)
                            }
                            AspectRatioMode.ORIGINAL -> {
                                matrix.setScale(1f / sx, 1f / sy, containerWidth / 2f, containerHeight / 2f)
                            }
                        }
                        textureView.setTransform(matrix)
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
                                Button(
                                    onClick = onDismiss,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text(if (isBangla) "ফিরে যান" else "Close")
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
                                                if (change.position.x > containerWidth / 2f) {
                                                    dragMode = 1 // Right Vertical -> Volume
                                                } else {
                                                    dragMode = 2 // Left Vertical -> Brightness
                                                }
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(20.dp)
                        ) {
                            Icon(
                                imageVector = when {
                                    currentVolumePercent == 0 -> Icons.Default.VolumeMute
                                    currentVolumePercent < 50 -> Icons.Default.VolumeDown
                                    else -> Icons.Default.VolumeUp
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
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
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
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ElectricIndigo)
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
                                    // 1080p Badge
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(NeonCyan.copy(alpha = 0.25f))
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = if (videoHeight >= 1080) "1080p FHD" else "720p HD",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeonCyan
                                        )
                                    }

                                    Text(
                                        text = " • Hardware Accelerated",
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(start = 4.dp)
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
            }
        }
    }
}
