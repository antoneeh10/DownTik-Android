package com.example.ui.player

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.material3.MiniController
import com.example.player.PlayingAudioInfo

/**
 * Media3 Material 3 MiniController for audio playback in DownTik.
 * Displayed directly above the bottom navigation bar when an audio track is active.
 */
@OptIn(UnstableApi::class)
@Composable
fun DownTikAudioMiniPlayer(
    player: Player,
    playingAudioInfo: PlayingAudioInfo?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = playingAudioInfo != null,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("audio_mini_player_surface")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    MiniController(
                        player = player,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .testTag("media3_mini_controller")
                    )
                }

                IconButton(
                    onClick = onClose,
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("close_mini_controller_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Tutup audio player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
