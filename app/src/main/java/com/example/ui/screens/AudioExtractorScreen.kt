package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import com.example.ui.components.Material3WavyCircularProgressIndicator
import com.example.ui.components.Material3WavyLinearProgressIndicator
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.VideoInfo
import com.example.downloader.DownTikStorage
import com.example.ui.AudioScreenState
import com.example.ui.MainViewModel
import com.example.ui.theme.SuccessGreen

@Composable
fun AudioExtractorScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val urlInput by viewModel.audioUrlInput.collectAsStateWithLifecycle()
    val inputError by viewModel.audioInputError.collectAsStateWithLifecycle()
    val uiState by viewModel.audioUiState.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 600.dp)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Headline Section
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ekstrak Audio",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tempel link video untuk mengekstrak lagu / audio (M4A)",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // URL Input Field
            OutlinedTextField(
                value = urlInput,
                onValueChange = { viewModel.onAudioUrlChange(it) },
                label = { Text("Video URL") },
                placeholder = { Text("https://vt.tiktok.com/...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = "Icon Link Video",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (urlInput.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearAudioUrl() },
                                modifier = Modifier.testTag("clear_audio_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Hapus URL"
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    val text = clipboardManager.getText()?.text
                                    if (!text.isNullOrBlank()) {
                                        viewModel.pasteAudioUrl(text)
                                    }
                                },
                                modifier = Modifier.testTag("paste_audio_url_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Tempel dari clipboard",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                },
                isError = inputError != null,
                supportingText = {
                    if (inputError != null) {
                        Text(
                            text = inputError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(
                    onGo = {
                        keyboardController?.hide()
                        viewModel.fetchAudioSourceInfo()
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("audio_url_input_field")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Primary "Get Audio" Button
            val isBusy = uiState is AudioScreenState.Loading || uiState is AudioScreenState.Extracting
            Button(
                onClick = {
                    keyboardController?.hide()
                    viewModel.fetchAudioSourceInfo()
                },
                enabled = !isBusy,
                shape = RoundedCornerShape(14.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("get_audio_button")
            ) {
                if (uiState is AudioScreenState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Mengambil Audio...",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Audiotrack,
                        contentDescription = "Get Audio",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Get Audio",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Dynamic Result Area (Smooth transition on state change, completely steady during extraction)
            AnimatedContent(
                targetState = uiState.categoryName,
                label = "AudioContentTransition"
            ) { category ->
                when (category) {
                    "Idle" -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hasil video dan tombol ekstrak audio akan tampil di sini.",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                ),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    "Loading" -> {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = CardDefaults.outlinedCardBorder(),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(40.dp))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Mengambil sumber video...",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Menyiapkan media untuk ekstraksi audio",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                )
                            }
                        }
                    }

                    "VideoReady" -> {
                        val state = uiState as? AudioScreenState.VideoReady
                        if (state != null) {
                            AudioReadyCard(
                                videoInfo = state.videoInfo,
                                onExtractClick = { viewModel.startExtractAudio(state.videoInfo) }
                            )
                        }
                    }

                    "Extracting" -> {
                        val state = uiState as? AudioScreenState.Extracting
                        if (state != null) {
                            AudioExtractingProgressCard(
                                videoInfo = state.videoInfo,
                                progress = state.progress,
                                percentageText = state.percentageText,
                                statusMessage = state.statusMessage
                            )
                        }
                    }

                    "AudioSaved" -> {
                        val state = uiState as? AudioScreenState.AudioSaved
                        if (state != null) {
                            AudioSavedCard(
                                videoInfo = state.videoInfo,
                                fileName = state.fileName,
                                totalBytes = state.totalBytes,
                                onOpenAudio = { DownTikStorage.openMedia(context, state.audioUri, "audio/mp4") },
                                onOpenFolder = { DownTikStorage.openFolder(context) },
                                onExtractAgain = { viewModel.resetAudioToIdle() }
                            )
                        }
                    }

                    "Error" -> {
                        val state = uiState as? AudioScreenState.Error
                        if (state != null) {
                            AudioErrorCard(
                                message = state.message,
                                canRetry = state.canRetry,
                                onRetry = { viewModel.retryAudio() },
                                onDismiss = { viewModel.resetAudioToIdle() }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioReadyCard(
    videoInfo: VideoInfo,
    onExtractClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_ready_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 100.dp, height = 125.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (!videoInfo.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(videoInfo.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail Video",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    if (videoInfo.durationSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = formatDuration(videoInfo.durationSeconds),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = videoInfo.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    val author = videoInfo.authorNickname ?: videoInfo.authorUsername
                    if (!author.isNullOrBlank()) {
                        Text(
                            text = "@$author",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Medium
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = "Format Output: M4A Audio (Lossless AAC)",
                            style = MaterialTheme.typography.labelSmall.copy(
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onExtractClick,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("extract_audio_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Audiotrack,
                    contentDescription = "Extract Audio Icon",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Extract Audio",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun AudioExtractingProgressCard(
    videoInfo: VideoInfo,
    progress: Float,
    percentageText: String,
    statusMessage: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_extracting_progress_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Material 3 Expressive Wavy Circular Indicator (Wavy from 4% to 90%, still otherwise)
                Material3WavyCircularProgressIndicator(
                    progress = { if (progress > 0f) progress else 0.04f },
                    modifier = Modifier.size(28.dp),
                    strokeWidth = 3.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Extracting Audio...",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = videoInfo.title,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = percentageText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Material 3 Expressive Wavy Linear Progress Bar (Wavy from 4% to 90%, flattens out at 90%)
            Material3WavyLinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                strokeWidth = 8.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = statusMessage,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "Please wait",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                )
            }
        }
    }
}

@Composable
private fun AudioSavedCard(
    videoInfo: VideoInfo,
    fileName: String,
    totalBytes: Long,
    onOpenAudio: () -> Unit,
    onOpenFolder: () -> Unit,
    onExtractAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_saved_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = SuccessGreen,
                    modifier = Modifier.size(36.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Audio Saved",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Audio berhasil disimpan ke Download/DownTik/Audio",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$fileName • ${DownTikStorage.formatFileSize(totalBytes)}",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = MaterialTheme.colorScheme.outline
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenAudio,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("open_audio_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar audio",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Audio")
                }

                OutlinedButton(
                    onClick = onOpenFolder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("open_audio_folder_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Folder,
                        contentDescription = "Buka folder",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Open Folder")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onExtractAgain,
                modifier = Modifier.testTag("extract_again_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Ekstrak lagi",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Ekstrak Lagi")
            }
        }
    }
}

@Composable
private fun AudioErrorCard(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        ),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("audio_error_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Error Icon",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Gagal Mengekstrak Audio",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onErrorContainer
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (canRetry) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("audio_retry_button")
                    ) {
                        Text("Coba Lagi")
                    }
                }

                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format("%02d:%02d", m, s)
}
