package com.example.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import com.example.data.model.VideoInfo
import kotlinx.coroutines.delay

/**
 * Material 3 Expressive Video Player for DownTik.
 * - Always visible 'X' close button and dedicated on-tap controller overlay.
 * - Full controls: Play/Pause, 10s Seek, Interactive timeline slider, Time display, Speed menu, Loop toggle.
 * - Shows offline alert ONLY when disconnected from internet.
 * - Suppresses all rogue error popups during active playback.
 */
@OptIn(UnstableApi::class)
@Composable
fun DownTikVideoPlayer(
    player: Player,
    videoInfo: VideoInfo,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Intercept hardware/system back button to smoothly close player
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    var isOffline by remember { mutableStateOf(!isInternetAvailable(context)) }

    // Monitor network changes
    DisposableEffect(context) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOffline = false
            }
            override fun onLost(network: Network) {
                isOffline = !isInternetAvailable(context)
            }
        }
        try {
            cm?.registerDefaultNetworkCallback(callback)
        } catch (_: Exception) {}

        onDispose {
            try {
                cm?.unregisterNetworkCallback(callback)
            } catch (_: Exception) {}
        }
    }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var playbackState by remember { mutableIntStateOf(player.playbackState) }
    var repeatMode by remember { mutableIntStateOf(player.repeatMode) }
    var currentSpeed by remember { mutableFloatStateOf(player.playbackParameters.speed) }

    // Controls visibility: defaults to true when entering player
    var controlsVisible by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Timeline progress tracking
    var currentPositionMs by remember { mutableLongStateOf(0L) }
    var durationMs by remember { mutableLongStateOf(0L) }
    var isSeeking by remember { mutableStateOf(false) }
    var seekPositionMs by remember { mutableFloatStateOf(0f) }

    // Auto-hide controls after 4.5 seconds of user inactivity when playing
    LaunchedEffect(controlsVisible, isPlaying, lastInteractionTime) {
        if (controlsVisible && isPlaying) {
            delay(4500)
            controlsVisible = false
        }
    }

    // Player position polling
    LaunchedEffect(player, isPlaying) {
        while (true) {
            if (!isSeeking) {
                currentPositionMs = player.currentPosition.coerceAtLeast(0L)
                val d = player.duration
                if (d > 0L) durationMs = d
            }
            delay(250)
        }
    }

    // Player state listener
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
                val d = player.duration
                if (d > 0L) durationMs = d
            }

            override fun onRepeatModeChanged(mode: Int) {
                repeatMode = mode
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
                currentSpeed = playbackParameters.speed
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.pause()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .testTag("downtik_media3_player_container")
    ) {
        // 1. Direct Video Rendering Surface
        PlayerSurface(
            player = player,
            modifier = Modifier.fillMaxSize()
        )

        // 2. Full-screen Tap Listener to Toggle Controls
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    controlsVisible = !controlsVisible
                    lastInteractionTime = System.currentTimeMillis()
                }
        )

        // 3. Persistent Floating 'X' Close Button
        // Always accessible on top corner so user can close anytime in 1 tap
        if (!controlsVisible) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .size(44.dp)
                    .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                    .testTag("player_persistent_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup Pemutar",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 4. Expressive Full Controls Overlay
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
            ) {
                // Top Bar Controls: Close 'X' Button, Title, Replay & Speed
                PlayerTopBar(
                    videoInfo = videoInfo,
                    currentSpeed = currentSpeed,
                    onClose = onClose,
                    onReplay = {
                        player.seekTo(0L)
                        player.play()
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onSpeedChange = { speed ->
                        player.playbackParameters = PlaybackParameters(speed)
                        currentSpeed = speed
                        lastInteractionTime = System.currentTimeMillis()
                    }
                )

                // Center Controls: Rewind 10s, Play/Pause, Forward 10s & Buffering
                PlayerCenterControls(
                    isPlaying = isPlaying,
                    isBuffering = playbackState == Player.STATE_BUFFERING,
                    onRewind = {
                        player.seekTo((player.currentPosition - 10000L).coerceAtLeast(0L))
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onTogglePlay = {
                        if (isPlaying) player.pause() else player.play()
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onForward = {
                        val maxPos = if (durationMs > 0L) durationMs else Long.MAX_VALUE
                        player.seekTo((player.currentPosition + 10000L).coerceAtMost(maxPos))
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.Center)
                )

                // Bottom Controls: Slider, Time Text & Loop Button
                PlayerBottomBar(
                    currentPositionMs = if (isSeeking) seekPositionMs.toLong() else currentPositionMs,
                    durationMs = durationMs,
                    repeatMode = repeatMode,
                    onSeekChange = {
                        isSeeking = true
                        seekPositionMs = it
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onSeekFinished = {
                        player.seekTo(seekPositionMs.toLong())
                        isSeeking = false
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    onToggleRepeat = {
                        val newMode = if (player.repeatMode == Player.REPEAT_MODE_ONE) {
                            Player.REPEAT_MODE_OFF
                        } else {
                            Player.REPEAT_MODE_ONE
                        }
                        player.repeatMode = newMode
                        repeatMode = newMode
                        lastInteractionTime = System.currentTimeMillis()
                    },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }

        // 5. Offline Dialog (ONLY shown when device is actually offline)
        if (isOffline) {
            AlertDialog(
                onDismissRequest = { /* stay visible while offline */ },
                icon = {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = "Offline",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(36.dp)
                    )
                },
                title = {
                    Text(
                        text = "Koneksi Offline",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        textAlign = TextAlign.Center
                    )
                },
                text = {
                    Text(
                        text = "Tidak dapat memutar aliran video karena perangkat sedang offline. Silakan sambungkan kembali ke internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            if (isInternetAvailable(context)) {
                                isOffline = false
                                player.prepare()
                                player.play()
                            }
                        },
                        modifier = Modifier.testTag("offline_retry_button")
                    ) {
                        Text("Coba Lagi")
                    }
                },
                dismissButton = {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier.testTag("offline_close_button")
                    ) {
                        Text("Tutup")
                    }
                },
                modifier = Modifier.testTag("offline_dialog")
            )
        }
    }
}

/**
 * Top App Bar of Video Player with prominent 'X' close button and video info.
 */
@Composable
private fun PlayerTopBar(
    videoInfo: VideoInfo,
    currentSpeed: Float,
    onClose: () -> Unit,
    onReplay: () -> Unit,
    onSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var showSpeedMenu by remember { mutableStateOf(false) }

    Surface(
        color = Color.Black.copy(alpha = 0.65f),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Prominent 'X' (Close) Button
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    .testTag("player_close_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Tutup",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Video Title & Author
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = videoInfo.title.ifBlank { "Video TikTok" },
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val author = videoInfo.authorNickname ?: videoInfo.authorUsername
                if (!author.isNullOrBlank()) {
                    Text(
                        text = "@$author",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Replay from start
            IconButton(
                onClick = onReplay,
                modifier = Modifier.testTag("player_replay_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Replay,
                    contentDescription = "Putar dari awal",
                    tint = Color.White
                )
            }

            // Speed Selector
            Box {
                IconButton(
                    onClick = { showSpeedMenu = true },
                    modifier = Modifier.testTag("player_speed_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Kecepatan",
                        tint = Color.White
                    )
                }

                DropdownMenu(
                    expanded = showSpeedMenu,
                    onDismissRequest = { showSpeedMenu = false }
                ) {
                    val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
                    speeds.forEach { speed ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "${speed}x" + if (speed == currentSpeed) " ✓" else "",
                                    fontWeight = if (speed == currentSpeed) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            onClick = {
                                showSpeedMenu = false
                                onSpeedChange(speed)
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Center Controls: Rewind 10s, Large Play/Pause, Forward 10s.
 */
@Composable
private fun PlayerCenterControls(
    isPlaying: Boolean,
    isBuffering: Boolean,
    onRewind: () -> Unit,
    onTogglePlay: () -> Unit,
    onForward: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.65f),
                modifier = Modifier.size(64.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            Row(
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rewind 10s
                FilledTonalIconButton(
                    onClick = onRewind,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("player_seek_back")
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay10,
                        contentDescription = "Mundur 10 detik",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Primary Large Play/Pause Button
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = MaterialTheme.colorScheme.primary,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .size(72.dp)
                        .testTag("player_play_pause_container")
                ) {
                    IconButton(
                        onClick = onTogglePlay,
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("player_play_pause")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Jeda" else "Putar",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // Forward 10s
                FilledTonalIconButton(
                    onClick = onForward,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("player_seek_forward")
                ) {
                    Icon(
                        imageVector = Icons.Default.Forward10,
                        contentDescription = "Maju 10 detik",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    }
}

/**
 * Bottom Controls: Slider Timeline, Position/Duration display, Loop toggle.
 */
@Composable
private fun PlayerBottomBar(
    currentPositionMs: Long,
    durationMs: Long,
    repeatMode: Int,
    onSeekChange: (Float) -> Unit,
    onSeekFinished: () -> Unit,
    onToggleRepeat: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.75f),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Interactive Progress Slider
            val maxRange = if (durationMs > 0L) durationMs.toFloat() else 100f
            val sliderValue = currentPositionMs.toFloat().coerceIn(0f, maxRange)

            Slider(
                value = sliderValue,
                onValueChange = onSeekChange,
                onValueChangeFinished = onSeekFinished,
                valueRange = 0f..maxRange,
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("player_progress_slider")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Position and Total Duration
                Text(
                    text = "${formatTime(currentPositionMs)} / ${formatTime(durationMs)}",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = Color.White,
                    modifier = Modifier.testTag("player_time_display")
                )

                // Loop toggle button
                IconButton(
                    onClick = onToggleRepeat,
                    modifier = Modifier.testTag("player_repeat_toggle")
                ) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Loop video",
                        tint = if (repeatMode == Player.REPEAT_MODE_ONE) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    if (millis <= 0L) return "00:00"
    val totalSeconds = millis / 1000
    val seconds = totalSeconds % 60
    val minutes = (totalSeconds / 60) % 60
    val hours = totalSeconds / 3600
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

private fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
    val network = cm.activeNetwork ?: return false
    val capabilities = cm.getNetworkCapabilities(network) ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}
