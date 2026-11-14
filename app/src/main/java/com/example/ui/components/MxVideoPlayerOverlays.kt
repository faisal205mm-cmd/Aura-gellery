package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Subtitles
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.player.DecoderStrategy
import com.example.player.DevicePlaybackProfile
import com.example.player.PlaybackDiagnosticsState
import com.example.player.RenderSurfaceMode
import com.example.ui.theme.GoldenVault
import com.example.ui.theme.NeonCyan
import java.util.Locale

@Composable
fun VideoQualitySelectionDialog(
    qualityOptions: List<VideoQualityOption>,
    selectedQualityId: String,
    videoWidth: Int,
    videoHeight: Int,
    isBangla: Boolean,
    onSelectQuality: (VideoQualityOption) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
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
                val videoTrackCount = qualityOptions.count { it.trackGroup != null }
                Text(
                    text = if (videoTrackCount <= 1) {
                        if (isBangla) {
                            "এই ভিডিওতে ১টি মূল রেজোলিউশন ট্র্যাক রয়েছে"
                        } else {
                            "This media source has 1 native resolution track"
                        }
                    } else {
                        if (isBangla) {
                            "মিডিয়া সোর্সে উপলব্ধ রেজোলিউশন ট্র্যাক থেকে বেছে নিন"
                        } else {
                            "Switch between available resolution tracks from the media source"
                        }
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
                        val detailText = if (option.id == "auto") {
                            val (activeLabel, _) = formatQualityLabels(videoWidth, videoHeight)
                            if (isBangla) "বর্তমান: $activeLabel (${videoWidth}×${videoHeight})"
                            else "Active: $activeLabel (${videoWidth}×${videoHeight})"
                        } else {
                            option.detail
                        }

                        Surface(
                            onClick = {
                                onSelectQuality(option)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) NeonCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                            border = if (isSelected) BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)) else null,
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
                                            color = if (isSelected) NeonCyan else Color.White,
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
                                if (isSelected) {
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
                    TextButton(onClick = onDismiss) {
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

@Composable
fun AudioSubtitleSelectionDialog(
    audioTracks: List<AudioTrackOption>,
    subtitleTracks: List<SubtitleTrackOption>,
    externalSubtitleName: String?,
    isBangla: Boolean,
    onSelectAudio: (AudioTrackOption) -> Unit,
    onSelectSubtitle: (SubtitleTrackOption) -> Unit,
    onPickExternalSubtitle: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(onClick = onDismiss)
            .padding(24.dp)
            .testTag("mx_audio_sub_dialog")
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161922)),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 440.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Subtitles,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isBangla) "অডিও ও সাবটাইটেল" else "Audio & Subtitles",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = if (isBangla) "অডিও ট্র্যাক" else "Audio Tracks",
                    color = GoldenVault,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                if (audioTracks.isEmpty()) {
                    Text(
                        text = if (isBangla) "কোনো অতিরিক্ত অডিও ট্র্যাক পাওয়া যায়নি" else "No selectable audio tracks found",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        audioTracks.forEach { track ->
                            Surface(
                                onClick = {
                                    onSelectAudio(track)
                                    onDismiss()
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (track.isSelected) NeonCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                                border = if (track.isSelected) BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)) else null,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AudioFile,
                                            contentDescription = null,
                                            tint = if (track.isSelected) NeonCyan else Color.White.copy(alpha = 0.7f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                text = track.label,
                                                color = if (track.isSelected) NeonCyan else Color.White,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = track.detail,
                                                color = Color.White.copy(alpha = 0.6f),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                    if (track.isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isBangla) "সাবটাইটেল ট্র্যাক" else "Subtitle Tracks",
                        color = GoldenVault,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onPickExternalSubtitle) {
                        Text(
                            text = if (isBangla) "+ ফাইল যোগ করুন" else "+ Load .srt/.vtt",
                            color = NeonCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (!externalSubtitleName.isNullOrBlank()) {
                    Text(
                        text = if (isBangla) "যুক্ত ফাইল: $externalSubtitleName" else "External: $externalSubtitleName",
                        color = NeonCyan.copy(alpha = 0.85f),
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    subtitleTracks.forEach { sub ->
                        Surface(
                            onClick = {
                                onSelectSubtitle(sub)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (sub.isSelected) NeonCyan.copy(alpha = 0.16f) else Color.White.copy(alpha = 0.05f),
                            border = if (sub.isSelected) BorderStroke(1.dp, NeonCyan.copy(alpha = 0.7f)) else null,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sub.label,
                                        color = if (sub.isSelected) NeonCyan else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = sub.detail,
                                        color = Color.White.copy(alpha = 0.6f),
                                        fontSize = 11.sp
                                    )
                                }
                                if (sub.isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = "Selected", tint = NeonCyan, modifier = Modifier.size(18.dp))
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
                    TextButton(onClick = onDismiss) {
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

@Composable
fun PlaybackDiagnosticsDialog(
    deviceProfile: DevicePlaybackProfile,
    diagnostics: PlaybackDiagnosticsState,
    isBangla: Boolean,
    onSwitchSurfaceMode: (RenderSurfaceMode) -> Unit,
    onSwitchDecoderStrategy: (DecoderStrategy) -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(onClick = onDismiss)
            .padding(20.dp)
            .testTag("mx_diagnostics_dialog")
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF12161F)),
            border = BorderStroke(1.dp, NeonCyan.copy(alpha = 0.35f)),
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .clickable(enabled = false) {}
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isBangla) "প্লেব্যাক ডায়াগনস্টিকস" else "Playback Diagnostics",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = diagnostics.playbackStateLabel,
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                DiagnosticRow("Device", "${deviceProfile.manufacturer} ${deviceProfile.model} (Android ${deviceProfile.androidVersion} / API ${deviceProfile.sdkInt})")
                DiagnosticRow("Memory", "${deviceProfile.availableRamMb} MB free / ${deviceProfile.totalRamMb} MB total" + if (deviceProfile.isLowRamDevice) " [Low-RAM Tuned]" else "")
                DiagnosticRow("Display", "${deviceProfile.displayWidth}×${deviceProfile.displayHeight}")
                DiagnosticRow("Surface", diagnostics.surfaceMode.label)
                DiagnosticRow("Strategy", diagnostics.decoderStrategy.label)
                DiagnosticRow("Video Stream", "${diagnostics.width}×${diagnostics.height} • ${diagnostics.videoMimeType} (${diagnostics.videoCodec})")
                val fpsLabel = if (diagnostics.frameRate > 0f) String.format(Locale.US, "%.2f fps", diagnostics.frameRate) else "-"
                val brLabel = if (diagnostics.bitrateBps > 0) "${diagnostics.bitrateBps / 1000} kbps" else "-"
                DiagnosticRow("FPS / Bitrate", "$fpsLabel • $brLabel")
                DiagnosticRow("Video Decoder", diagnostics.selectedVideoDecoder)
                DiagnosticRow("Audio Decoder", "${diagnostics.selectedAudioDecoder} (${diagnostics.audioMimeType})")
                DiagnosticRow("Frames", "Rendered=${diagnostics.firstFrameRendered} • Dropped=${diagnostics.droppedFramesTotal}")

                if (!diagnostics.lastDecoderError.isNullOrBlank()) {
                    DiagnosticRow("Decoder Error", diagnostics.lastDecoderError, Color(0xFFF87171))
                }
                if (!diagnostics.lastRenderError.isNullOrBlank()) {
                    DiagnosticRow("Render Error", diagnostics.lastRenderError, Color(0xFFF87171))
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Hardware / Software Codec Capabilities",
                    color = GoldenVault,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                deviceProfile.codecSummaries.values.forEach { cap ->
                    DiagnosticRow(
                        cap.displayName,
                        "${cap.maxSupportedLabel} (HW=${cap.hardwareDecoderName ?: "none"}, SW=${cap.softwareDecoderName ?: "none"})"
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isBangla) "রেন্ডারিং ও ডিকোডার সুইচ" else "Rendering & Decoder Override",
                    color = GoldenVault,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    val nextSurface = if (diagnostics.surfaceMode == RenderSurfaceMode.SURFACE_VIEW) {
                        RenderSurfaceMode.TEXTURE_VIEW
                    } else {
                        RenderSurfaceMode.SURFACE_VIEW
                    }
                    OutlinedButton(
                        onClick = { onSwitchSurfaceMode(nextSurface) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (nextSurface == RenderSurfaceMode.SURFACE_VIEW) "Use SurfaceView" else "Use TextureView",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    val nextStrategy = if (diagnostics.decoderStrategy == DecoderStrategy.HARDWARE_FIRST) {
                        DecoderStrategy.SOFTWARE_FALLBACK
                    } else {
                        DecoderStrategy.HARDWARE_FIRST
                    }
                    OutlinedButton(
                        onClick = { onSwitchDecoderStrategy(nextStrategy) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (nextStrategy == DecoderStrategy.SOFTWARE_FALLBACK) "Try SW Decoder" else "Try HW Decoder",
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }

                if (diagnostics.recentEvents.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Analytics Event Log",
                        color = GoldenVault,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    diagnostics.recentEvents.takeLast(8).forEach { line ->
                        Text(
                            text = "• $line",
                            color = Color.White.copy(alpha = 0.72f),
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                    TextButton(onClick = onDismiss) {
                        Text(if (isBangla) "বন্ধ করুন" else "Close", color = NeonCyan)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String, valueColor: Color = Color.White.copy(alpha = 0.88f)) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            color = Color.White.copy(alpha = 0.55f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(102.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 11.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PlaybackErrorOverlay(
    playbackError: String,
    fileName: String,
    isBangla: Boolean,
    onRetry: () -> Unit,
    onOpenDiagnostics: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f))
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
                    text = playbackError,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = fileName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onDismiss) {
                        Text(if (isBangla) "ফিরে যান" else "Close")
                    }
                    OutlinedButton(onClick = onOpenDiagnostics) {
                        Text(if (isBangla) "তথ্য" else "Info")
                    }
                    Button(
                        onClick = onRetry,
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
