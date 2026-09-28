package com.example.update.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.example.ui.components.Material3WavyLinearProgressIndicator
import com.example.update.GitHubRelease
import com.example.update.ReleaseAsset
import com.example.update.ReleaseChannel
import com.example.update.UpdateState

@Composable
fun UpdateDialog(
    updateState: UpdateState,
    onDismiss: () -> Unit,
    onStartDownload: (ReleaseAsset, GitHubRelease) -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val availableState = when (updateState) {
        is UpdateState.UpdateAvailable -> updateState
        is UpdateState.Downloading -> null
        is UpdateState.ReadyToInstall -> null
        is UpdateState.Error -> null
        else -> null
    }

    AlertDialog(
        onDismissRequest = {
            if (updateState !is UpdateState.Downloading) {
                onDismiss()
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = updateState !is UpdateState.Downloading,
            dismissOnClickOutside = updateState !is UpdateState.Downloading
        ),
        shape = RoundedCornerShape(24.dp),
        icon = {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Pembaruan",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        title = {
            val titleText = when (updateState) {
                is UpdateState.UpdateAvailable -> {
                    if (updateState.channel == ReleaseChannel.BETA) "Pembaruan Beta Tersedia" else "Pembaruan Tersedia"
                }
                else -> "Pembaruan Tersedia"
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = titleText,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (updateState) {
                    is UpdateState.UpdateAvailable -> {
                        UpdateInfoSection(updateState)
                    }

                    is UpdateState.Downloading -> {
                        UpdateDownloadingSection(updateState)
                    }

                    is UpdateState.ReadyToInstall -> {
                        UpdateReadySection()
                    }

                    is UpdateState.Error -> {
                        UpdateErrorSection(updateState.message)
                    }

                    else -> Unit
                }
            }
        },
        confirmButton = {
            when (updateState) {
                is UpdateState.UpdateAvailable -> {
                    Button(
                        onClick = { onStartDownload(updateState.apkAsset, updateState.release) },
                        modifier = Modifier.testTag("update_confirm_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Perbarui Sekarang")
                    }
                }

                is UpdateState.ReadyToInstall -> {
                    Button(
                        onClick = onInstall,
                        modifier = Modifier.testTag("update_install_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pasang Pembaruan")
                    }
                }

                is UpdateState.Error -> {
                    if (updateState.canRetry) {
                        Button(
                            onClick = onRetry,
                            modifier = Modifier.testTag("update_retry_button")
                        ) {
                            Text("Coba Lagi")
                        }
                    }
                }

                else -> Unit
            }
        },
        dismissButton = {
            if (updateState !is UpdateState.Downloading) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("update_dismiss_button")
                ) {
                    Text("Nanti Saja")
                }
            }
        },
        modifier = modifier.testTag("app_update_dialog")
    )
}

@Composable
private fun UpdateInfoSection(state: UpdateState.UpdateAvailable) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Version header & channel badge
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DownTik ${state.versionName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Rilis: ${state.publishedDate}" + if (state.apkSizeFormatted.isNotBlank()) " • ${state.apkSizeFormatted}" else "",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            // Channel badge
            ChannelBadge(channel = state.channel)
        }

        // Release notes card
        Text(
            text = "Apa yang Baru:",
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        )

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = state.releaseNotes,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                modifier = Modifier.padding(14.dp)
            )
        }
    }
}

@Composable
private fun UpdateDownloadingSection(state: UpdateState.Downloading) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Mengunduh pembaruan...",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Material 3 Expressive Wavy Linear Progress Indicator
        Material3WavyLinearProgressIndicator(
            progress = { state.progress },
            modifier = Modifier.fillMaxWidth(),
            strokeWidth = 8.dp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val downloadedMb = state.downloadedBytes / (1024.0 * 1024.0)
            val totalMb = state.totalBytes / (1024.0 * 1024.0)
            val text = if (state.totalBytes > 0) {
                String.format("%.1f MB / %.1f MB", downloadedMb, totalMb)
            } else {
                String.format("%.1f MB", downloadedMb)
            }

            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )

            Text(
                text = state.percentageText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
private fun UpdateReadySection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "File Pembaruan Siap!",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Pemasang paket Android akan terbuka untuk memperbarui aplikasi DownTik.",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun UpdateErrorSection(errorMessage: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.Warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(36.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Gagal Mengunduh Pembaruan",
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
fun ChannelBadge(
    channel: ReleaseChannel,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor) = when (channel) {
        ReleaseChannel.STABLE -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        ReleaseChannel.BETA -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = bgColor,
        modifier = modifier
    ) {
        Text(
            text = channel.badgeLabel,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}
