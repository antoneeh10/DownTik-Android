package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.VideoInfo
import com.example.downloader.DownTikStorage
import com.example.ui.components.Material3WavyCircularProgressIndicator
import com.example.ui.components.Material3WavyLinearProgressIndicator
import com.example.ui.MainScreenState
import com.example.ui.MainViewModel
import com.example.ui.player.DownTikVideoPlayer
import com.example.ui.theme.SuccessGreen
import kotlin.math.roundToInt

@Composable
fun MainDownloadScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val urlInput by viewModel.videoUrlInput.collectAsStateWithLifecycle()
    val inputError by viewModel.videoInputError.collectAsStateWithLifecycle()
    val uiState by viewModel.videoUiState.collectAsStateWithLifecycle()
    val preferHd by viewModel.preferHd.collectAsStateWithLifecycle()
    val isFullscreenPreviewOpen by viewModel.isVideoPreviewOpen.collectAsStateWithLifecycle()

    val scrollState = rememberScrollState()

    var thumbnailPositionInRoot by remember { mutableStateOf<Offset?>(null) }
    var thumbnailSizeInRoot by remember { mutableStateOf<IntSize?>(null) }
    var rootSize by remember { mutableStateOf<IntSize?>(null) }

    val expandProgress by animateFloatAsState(
        targetValue = if (isFullscreenPreviewOpen && uiState is MainScreenState.VideoReady) 1f else 0f,
        animationSpec = tween(
            durationMillis = if (isFullscreenPreviewOpen) 380 else 320,
            easing = FastOutSlowInEasing
        ),
        label = "ThumbnailToPlayerExpand"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .onGloballyPositioned { coordinates ->
                rootSize = coordinates.size
            }
    ) {
        // Base Content Column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .widthIn(max = 600.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Headline Section
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Download Video",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        ),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tempel link video untuk mulai mengunduh",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // URL Input Field
                    OutlinedTextField(
                        value = urlInput,
                        onValueChange = { viewModel.onVideoUrlChange(it) },
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
                                        onClick = { viewModel.clearVideoUrl() },
                                        modifier = Modifier.testTag("clear_url_button")
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
                                                viewModel.pasteVideoUrl(text)
                                            }
                                        },
                                        modifier = Modifier.testTag("paste_url_button")
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
                                viewModel.fetchVideoInfo()
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
                            .testTag("url_input_field")
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Primary "Get Video" Button
                    val isLoading = uiState is MainScreenState.Loading || uiState is MainScreenState.Downloading
                    Button(
                        onClick = {
                            keyboardController?.hide()
                            viewModel.fetchVideoInfo()
                        },
                        enabled = !isLoading,
                        shape = RoundedCornerShape(14.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("get_video_button")
                    ) {
                        if (uiState is MainScreenState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Mengambil Video...",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.CloudDownload,
                                contentDescription = "Get Video Icon",
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Get Video",
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Dynamic Result Area (Smooth transition on state change, completely steady during progress updates)
                    AnimatedContent(
                        targetState = uiState.categoryName,
                        label = "VideoDownloadContentTransition"
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
                                        text = "Hasil video dan opsi download akan tampil di sini.",
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
                                            text = "Mengambil informasi video...",
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Memverifikasi link dan format video",
                                            style = MaterialTheme.typography.bodySmall.copy(
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                    }
                                }
                            }

                            "VideoReady" -> {
                                val state = uiState as? MainScreenState.VideoReady
                                if (state != null) {
                                    VideoReadyCard(
                                        videoInfo = state.videoInfo,
                                        preferHd = preferHd,
                                        isExpanding = expandProgress > 0.001f,
                                        onThumbnailPositioned = { pos, size ->
                                            thumbnailPositionInRoot = pos
                                            thumbnailSizeInRoot = size
                                        },
                                        onToggleHd = { viewModel.togglePreferHd(it) },
                                        onPreviewClick = { viewModel.openVideoPreview(state.videoInfo) },
                                        onDownloadClick = { viewModel.startDownloadVideo(state.videoInfo) }
                                    )
                                }
                            }

                            "Downloading" -> {
                                val state = uiState as? MainScreenState.Downloading
                                if (state != null) {
                                    DownloadingProgressCard(
                                        videoInfo = state.videoInfo,
                                        progress = state.progress,
                                        percentageText = state.percentageText,
                                        sizeText = state.sizeText
                                    )
                                }
                            }

                            "DownloadComplete" -> {
                                val state = uiState as? MainScreenState.DownloadComplete
                                if (state != null) {
                                    DownloadCompleteCard(
                                        videoInfo = state.videoInfo,
                                        fileName = state.fileName,
                                        totalBytes = state.totalBytes,
                                        onOpenVideo = { DownTikStorage.openMedia(context, state.downloadedUri, "video/mp4") },
                                        onOpenFolder = { DownTikStorage.openFolder(context) },
                                        onDownloadAgain = { viewModel.resetVideoToIdle() }
                                    )
                                }
                            }

                            "Error" -> {
                                val state = uiState as? MainScreenState.Error
                                if (state != null) {
                                    ErrorCard(
                                        message = state.message,
                                        canRetry = state.canRetry,
                                        onRetry = { viewModel.retryVideo() },
                                        onDismiss = { viewModel.resetVideoToIdle() }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Smooth Expand-from-Thumbnail / Shrink-to-Thumbnail Container Transform
        val readyInfo = (uiState as? MainScreenState.VideoReady)?.videoInfo
        if (expandProgress > 0.001f && readyInfo != null) {
            val density = LocalDensity.current
            val rWidth = rootSize?.width?.toFloat() ?: 1080f
            val rHeight = rootSize?.height?.toFloat() ?: 2400f

            val startX = thumbnailPositionInRoot?.x ?: ((rWidth - with(density) { 110.dp.toPx() }) / 2f)
            val startY = thumbnailPositionInRoot?.y ?: ((rHeight - with(density) { 145.dp.toPx() }) / 2f)
            val startW = thumbnailSizeInRoot?.width?.toFloat() ?: with(density) { 110.dp.toPx() }
            val startH = thumbnailSizeInRoot?.height?.toFloat() ?: with(density) { 145.dp.toPx() }

            val currentX = startX + (0f - startX) * expandProgress
            val currentY = startY + (0f - startY) * expandProgress
            val currentW = startW + (rWidth - startW) * expandProgress
            val currentH = startH + (rHeight - startH) * expandProgress
            val currentCorner = (12f * (1f - expandProgress)).dp

            // Dim background as player expands to fullscreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = (0.85f * expandProgress).coerceIn(0f, 1f)))
            )

            // Expanding / Shrinking Container Box
            Box(
                modifier = Modifier
                    .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
                    .size(
                        width = with(density) { currentW.toDp() },
                        height = with(density) { currentH.toDp() }
                    )
                    .clip(RoundedCornerShape(currentCorner))
                    .background(Color.Black)
                    .testTag("expanding_video_player_container")
            ) {
                // 1. Thumbnail Content: fades out smoothly from progress 0f to 0.45f
                val thumbnailAlpha = (1f - (expandProgress / 0.45f)).coerceIn(0f, 1f)
                if (thumbnailAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = thumbnailAlpha },
                        contentAlignment = Alignment.Center
                    ) {
                        if (!readyInfo.coverUrl.isNullOrBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(readyInfo.coverUrl)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.58f),
                            modifier = Modifier.size(42.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Fullscreen Video Player: fades in smoothly from progress 0.35f to 1.0f
                val playerAlpha = ((expandProgress - 0.35f) / 0.55f).coerceIn(0f, 1f)
                if (playerAlpha > 0.01f) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { alpha = playerAlpha }
                    ) {
                        DownTikVideoPlayer(
                            player = viewModel.playerManager.videoPlayer,
                            videoInfo = readyInfo,
                            onClose = { viewModel.closeVideoPreview() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoReadyCard(
    videoInfo: VideoInfo,
    preferHd: Boolean,
    isExpanding: Boolean,
    onThumbnailPositioned: (Offset, IntSize) -> Unit,
    onToggleHd: (Boolean) -> Unit,
    onPreviewClick: () -> Unit,
    onDownloadClick: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_ready_card")
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
                // Interactive Thumbnail Box with Centered Play Button ▶
                Box(
                    modifier = Modifier
                        .size(width = 110.dp, height = 145.dp)
                        .onGloballyPositioned { coordinates ->
                            val pos = coordinates.positionInRoot()
                            val size = coordinates.size
                            onThumbnailPositioned(pos, size)
                        }
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .clickable(onClick = onPreviewClick)
                        .graphicsLayer {
                            // When expanding, hide the static thumbnail so there's no ghosting
                            alpha = if (isExpanding) 0f else 1f
                        }
                        .testTag("video_thumbnail_preview"),
                    contentAlignment = Alignment.Center
                ) {
                    if (!videoInfo.coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(videoInfo.coverUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Thumbnail Video Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Play Button Overlay in Center ▶
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.58f),
                        shadowElevation = 4.dp,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Video Preview",
                                tint = Color.White,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    // Duration badge in bottom-right
                    if (videoInfo.durationSeconds > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color.Black.copy(alpha = 0.75f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp)
                        ) {
                            Text(
                                text = formatDuration(videoInfo.durationSeconds),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
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

                    Spacer(modifier = Modifier.height(4.dp))

                    val estimatedSize = videoInfo.getEstimatedSize(preferHd)
                    if (estimatedSize != null && estimatedSize > 0) {
                        Text(
                            text = "Ukuran: ${DownTikStorage.formatFileSize(estimatedSize)}",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Ketuk thumbnail untuk preview video",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val hasHd = !videoInfo.hdPlayUrl.isNullOrBlank()
            if (hasHd) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = preferHd,
                        onClick = { onToggleHd(true) },
                        label = { Text("Kualitas HD") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.HighQuality,
                                contentDescription = "HD quality",
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("quality_hd_chip")
                    )

                    FilterChip(
                        selected = !preferHd,
                        onClick = { onToggleHd(false) },
                        label = { Text("Standar (Hemat Kuota)") },
                        modifier = Modifier.testTag("quality_sd_chip")
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            Button(
                onClick = onDownloadClick,
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("download_video_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Download,
                    contentDescription = "Icon Download Video",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasHd && preferHd) "Download (HD No Watermark)" else "Download (No Watermark)",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun DownloadingProgressCard(
    videoInfo: VideoInfo,
    progress: Float,
    percentageText: String,
    sizeText: String
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("downloading_progress_card")
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
                        text = "Downloading...",
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

            // Material 3 Expressive Wavy Linear Progress Bar
            // Starts waving at 4%, stops waving and flattens out smoothly at 90%
            Material3WavyLinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                strokeWidth = 8.dp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Menyimpan ke Download/DownTik/Video",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Text(
                    text = sizeText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun DownloadCompleteCard(
    videoInfo: VideoInfo,
    fileName: String,
    totalBytes: Long,
    onOpenVideo: () -> Unit,
    onOpenFolder: () -> Unit,
    onDownloadAgain: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = CardDefaults.outlinedCardBorder(),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_complete_card")
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
                text = "Download Complete",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Video berhasil disimpan ke Download/DownTik/Video",
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
                    onClick = onOpenVideo,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("open_video_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Putar video",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Video")
                }

                OutlinedButton(
                    onClick = onOpenFolder,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("open_folder_button")
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
                onClick = onDownloadAgain,
                modifier = Modifier.testTag("download_again_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Download lagi",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Download Lagi")
            }
        }
    }
}

@Composable
private fun ErrorCard(
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
            .testTag("error_card")
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
                text = "Gagal Mengunduh",
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
                        modifier = Modifier.testTag("retry_button")
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
