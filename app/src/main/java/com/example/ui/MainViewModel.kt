package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadHistoryEntity
import com.example.data.model.VideoInfo
import com.example.data.repository.HistoryRepository
import com.example.data.repository.SettingsRepository
import com.example.data.repository.VideoRepository
import com.example.downloader.AudioExtractionState
import com.example.downloader.AudioExtractor
import com.example.downloader.AudioStage
import com.example.downloader.DownTikStorage
import com.example.downloader.DownloadState
import com.example.downloader.VideoDownloader
import com.example.data.network.NetworkClient
import com.example.ui.theme.ThemeMode
import com.example.update.GitHubRelease
import com.example.update.ReleaseAsset
import com.example.update.ReleaseChannel
import com.example.update.UpdateChecker
import com.example.update.UpdateDownloader
import com.example.update.UpdateInstaller
import com.example.update.UpdateState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainScreenState {
    val categoryName: String
        get() = when (this) {
            is Idle -> "Idle"
            is Loading -> "Loading"
            is VideoReady -> "VideoReady"
            is Downloading -> "Downloading"
            is DownloadComplete -> "DownloadComplete"
            is Error -> "Error"
        }

    data object Idle : MainScreenState
    data object Loading : MainScreenState
    data class VideoReady(
        val videoInfo: VideoInfo,
        val preferHd: Boolean = true
    ) : MainScreenState
    data class Downloading(
        val videoInfo: VideoInfo,
        val progress: Float,
        val percentageText: String,
        val sizeText: String
    ) : MainScreenState
    data class DownloadComplete(
        val videoInfo: VideoInfo,
        val downloadedUri: Uri,
        val fileName: String,
        val totalBytes: Long
    ) : MainScreenState
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : MainScreenState
}

sealed interface AudioScreenState {
    val categoryName: String
        get() = when (this) {
            is Idle -> "Idle"
            is Loading -> "Loading"
            is VideoReady -> "VideoReady"
            is Extracting -> "Extracting"
            is AudioSaved -> "AudioSaved"
            is Error -> "Error"
        }

    data object Idle : AudioScreenState
    data object Loading : AudioScreenState
    data class VideoReady(val videoInfo: VideoInfo) : AudioScreenState
    data class Extracting(
        val videoInfo: VideoInfo,
        val stage: AudioStage,
        val progress: Float,
        val percentageText: String,
        val statusMessage: String
    ) : AudioScreenState
    data class AudioSaved(
        val videoInfo: VideoInfo,
        val audioUri: Uri,
        val fileName: String,
        val totalBytes: Long
    ) : AudioScreenState
    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : AudioScreenState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val videoRepository = VideoRepository()
    private val database = AppDatabase.getInstance(application)
    private val historyRepository = HistoryRepository(database.downloadHistoryDao())
    private val settingsRepository = SettingsRepository(application)
    private val videoDownloader = VideoDownloader(application)
    private val audioExtractor = AudioExtractor(application)
    val playerManager = com.example.player.DownTikPlayerManager(application)
    val currentPlayingAudio = playerManager.currentPlayingAudio

    // ================= Update System State =================
    private val updateChecker = UpdateChecker(application)
    private val updateDownloader = UpdateDownloader(application, NetworkClient.okHttpClient)

    val updateChannel: StateFlow<ReleaseChannel> = settingsRepository.updateChannel

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog.asStateFlow()

    private var downloadJob: Job? = null

    init {
        // Startup check: non-blocking asynchronous check on selected channel
        viewModelScope.launch {
            delay(1200) // slight delay to allow smooth initial app launch
            checkForUpdate(isManual = false)
        }
    }

    // ================= Video Downloader State =================
    private val _videoUrlInput = MutableStateFlow("")
    val videoUrlInput: StateFlow<String> = _videoUrlInput.asStateFlow()

    private val _videoInputError = MutableStateFlow<String?>(null)
    val videoInputError: StateFlow<String?> = _videoInputError.asStateFlow()

    private val _videoUiState = MutableStateFlow<MainScreenState>(MainScreenState.Idle)
    val videoUiState: StateFlow<MainScreenState> = _videoUiState.asStateFlow()

    private val _preferHd = MutableStateFlow(true)
    val preferHd: StateFlow<Boolean> = _preferHd.asStateFlow()

    private val _isVideoPreviewOpen = MutableStateFlow(false)
    val isVideoPreviewOpen: StateFlow<Boolean> = _isVideoPreviewOpen.asStateFlow()

    fun openVideoPreview(videoInfo: VideoInfo) {
        val streamUrl = videoInfo.getBestDownloadUrl(_preferHd.value)
        if (streamUrl.isNotBlank()) {
            playerManager.prepareAndPlayVideo(streamUrl)
            _isVideoPreviewOpen.value = true
        }
    }

    fun closeVideoPreview() {
        playerManager.stopVideo()
        _isVideoPreviewOpen.value = false
    }

    fun setVideoPreviewOpen(open: Boolean) {
        if (!open) {
            playerManager.stopVideo()
        }
        _isVideoPreviewOpen.value = open
    }

    private var activeDownloadJob: Job? = null
    private var lastRequestedVideoUrl: String = ""

    // ================= Audio Extractor State =================
    private val _audioUrlInput = MutableStateFlow("")
    val audioUrlInput: StateFlow<String> = _audioUrlInput.asStateFlow()

    private val _audioInputError = MutableStateFlow<String?>(null)
    val audioInputError: StateFlow<String?> = _audioInputError.asStateFlow()

    private val _audioUiState = MutableStateFlow<AudioScreenState>(AudioScreenState.Idle)
    val audioUiState: StateFlow<AudioScreenState> = _audioUiState.asStateFlow()

    private var activeExtractJob: Job? = null
    private var lastRequestedAudioUrl: String = ""

    // ================= Common State =================
    val historyList: StateFlow<List<DownloadHistoryEntity>> = historyRepository.historyFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode

    // ---------------- Video Downloader Actions ----------------
    fun onVideoUrlChange(newUrl: String) {
        _videoUrlInput.value = newUrl
        if (_videoInputError.value != null) {
            _videoInputError.value = null
        }
    }

    fun clearVideoUrl() {
        _videoUrlInput.value = ""
        _videoInputError.value = null
        if (_videoUiState.value !is MainScreenState.Downloading) {
            _videoUiState.value = MainScreenState.Idle
        }
    }

    fun pasteVideoUrl(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _videoUrlInput.value = trimmed
            _videoInputError.value = null
        }
    }

    fun togglePreferHd(prefer: Boolean) {
        _preferHd.value = prefer
        val current = _videoUiState.value
        if (current is MainScreenState.VideoReady) {
            _videoUiState.value = current.copy(preferHd = prefer)
        }
    }

    fun fetchVideoInfo() {
        val input = _videoUrlInput.value.trim()
        if (input.isEmpty()) {
            _videoInputError.value = "Masukkan link video terlebih dahulu"
            return
        }

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            _videoInputError.value = "Format URL tidak valid (harus dimulai dengan http:// atau https://)"
            return
        }

        if (_videoUiState.value is MainScreenState.Loading || _videoUiState.value is MainScreenState.Downloading) {
            return
        }

        lastRequestedVideoUrl = input
        _videoInputError.value = null
        _videoUiState.value = MainScreenState.Loading

        viewModelScope.launch {
            val result = videoRepository.fetchVideoInfo(input)
            result.fold(
                onSuccess = { info ->
                    val hasHd = !info.hdPlayUrl.isNullOrBlank()
                    _preferHd.value = hasHd
                    _videoUiState.value = MainScreenState.VideoReady(info, preferHd = hasHd)
                },
                onFailure = { err ->
                    _videoUiState.value = MainScreenState.Error(
                        message = err.localizedMessage ?: "Gagal memproses video. Periksa URL Anda.",
                        canRetry = true
                    )
                }
            )
        }
    }

    fun startDownloadVideo(videoInfo: VideoInfo) {
        if (_videoUiState.value is MainScreenState.Downloading) return

        val downloadUrl = videoInfo.getBestDownloadUrl(_preferHd.value)
        if (downloadUrl.isBlank()) {
            _videoUiState.value = MainScreenState.Error("URL video tidak ditemukan untuk diunduh", canRetry = false)
            return
        }

        activeDownloadJob?.cancel()
        activeDownloadJob = viewModelScope.launch {
            _videoUiState.value = MainScreenState.Downloading(
                videoInfo = videoInfo,
                progress = 0f,
                percentageText = "0%",
                sizeText = "Memulai download..."
            )

            videoDownloader.downloadVideo(downloadUrl, videoInfo.title)
                .collect { downloadState ->
                    when (downloadState) {
                        is DownloadState.Downloading -> {
                            _videoUiState.value = MainScreenState.Downloading(
                                videoInfo = videoInfo,
                                progress = downloadState.progress,
                                percentageText = downloadState.percentageText,
                                sizeText = downloadState.sizeText
                            )
                        }
                        is DownloadState.Completed -> {
                            historyRepository.addHistory(
                                videoId = videoInfo.id,
                                title = videoInfo.title,
                                coverUrl = videoInfo.coverUrl,
                                author = videoInfo.authorNickname ?: videoInfo.authorUsername,
                                downloadUri = downloadState.uri.toString(),
                                fileName = downloadState.fileName,
                                filePath = "${DownTikStorage.VIDEO_FOLDER}/${downloadState.fileName}",
                                fileSizeBytes = downloadState.totalBytes,
                                mediaType = "VIDEO",
                                storagePath = DownTikStorage.VIDEO_FOLDER
                            )

                            _videoUiState.value = MainScreenState.DownloadComplete(
                                videoInfo = videoInfo,
                                downloadedUri = downloadState.uri,
                                fileName = downloadState.fileName,
                                totalBytes = downloadState.totalBytes
                            )
                        }
                        is DownloadState.Failed -> {
                            _videoUiState.value = MainScreenState.Error(
                                message = downloadState.message,
                                canRetry = true
                            )
                        }
                        DownloadState.Idle -> {}
                    }
                }
        }
    }

    fun resetVideoToIdle() {
        activeDownloadJob?.cancel()
        activeDownloadJob = null
        _isVideoPreviewOpen.value = false
        _videoUiState.value = MainScreenState.Idle
    }

    fun retryVideo() {
        if (lastRequestedVideoUrl.isNotBlank()) {
            _videoUrlInput.value = lastRequestedVideoUrl
            fetchVideoInfo()
        } else {
            resetVideoToIdle()
        }
    }

    // ---------------- Audio Extractor Actions ----------------
    fun onAudioUrlChange(newUrl: String) {
        _audioUrlInput.value = newUrl
        if (_audioInputError.value != null) {
            _audioInputError.value = null
        }
    }

    fun clearAudioUrl() {
        _audioUrlInput.value = ""
        _audioInputError.value = null
        if (_audioUiState.value !is AudioScreenState.Extracting) {
            _audioUiState.value = AudioScreenState.Idle
        }
    }

    fun pasteAudioUrl(text: String) {
        val trimmed = text.trim()
        if (trimmed.isNotEmpty()) {
            _audioUrlInput.value = trimmed
            _audioInputError.value = null
        }
    }

    fun fetchAudioSourceInfo() {
        val input = _audioUrlInput.value.trim()
        if (input.isEmpty()) {
            _audioInputError.value = "Masukkan link video terlebih dahulu"
            return
        }

        if (!input.startsWith("http://") && !input.startsWith("https://")) {
            _audioInputError.value = "Format URL tidak valid (harus dimulai dengan http:// atau https://)"
            return
        }

        if (_audioUiState.value is AudioScreenState.Loading || _audioUiState.value is AudioScreenState.Extracting) {
            return
        }

        lastRequestedAudioUrl = input
        _audioInputError.value = null
        _audioUiState.value = AudioScreenState.Loading

        viewModelScope.launch {
            val result = videoRepository.fetchVideoInfo(input)
            result.fold(
                onSuccess = { info ->
                    _audioUiState.value = AudioScreenState.VideoReady(info)
                },
                onFailure = { err ->
                    _audioUiState.value = AudioScreenState.Error(
                        message = err.localizedMessage ?: "Gagal memproses video. Periksa URL Anda.",
                        canRetry = true
                    )
                }
            )
        }
    }

    fun startExtractAudio(videoInfo: VideoInfo) {
        if (_audioUiState.value is AudioScreenState.Extracting) return

        val sourceVideoUrl = videoInfo.getBestDownloadUrl(preferHd = false)
        if (sourceVideoUrl.isBlank()) {
            _audioUiState.value = AudioScreenState.Error("Sumber video tidak ditemukan untuk ekstraksi", canRetry = false)
            return
        }

        activeExtractJob?.cancel()
        activeExtractJob = viewModelScope.launch {
            _audioUiState.value = AudioScreenState.Extracting(
                videoInfo = videoInfo,
                stage = AudioStage.DOWNLOADING_SOURCE,
                progress = 0.05f,
                percentageText = "5%",
                statusMessage = "Mengunduh sumber video..."
            )

            audioExtractor.extractAudio(
                videoUrl = sourceVideoUrl,
                title = videoInfo.title,
                durationSeconds = videoInfo.durationSeconds
            ).collect { state ->
                when (state) {
                    is AudioExtractionState.Progress -> {
                        _audioUiState.value = AudioScreenState.Extracting(
                            videoInfo = videoInfo,
                            stage = state.stage,
                            progress = state.progress,
                            percentageText = state.percentageText,
                            statusMessage = state.statusMessage
                        )
                    }
                    is AudioExtractionState.Completed -> {
                        historyRepository.addHistory(
                            videoId = videoInfo.id,
                            title = videoInfo.title,
                            coverUrl = videoInfo.coverUrl,
                            author = videoInfo.authorNickname ?: videoInfo.authorUsername,
                            downloadUri = state.uri.toString(),
                            fileName = state.fileName,
                            filePath = "${DownTikStorage.AUDIO_FOLDER}/${state.fileName}",
                            fileSizeBytes = state.totalBytes,
                            mediaType = "AUDIO",
                            storagePath = DownTikStorage.AUDIO_FOLDER
                        )

                        _audioUiState.value = AudioScreenState.AudioSaved(
                            videoInfo = videoInfo,
                            audioUri = state.uri,
                            fileName = state.fileName,
                            totalBytes = state.totalBytes
                        )
                    }
                    is AudioExtractionState.Failed -> {
                        _audioUiState.value = AudioScreenState.Error(
                            message = state.message,
                            canRetry = true
                        )
                    }
                    AudioExtractionState.Idle -> {}
                }
            }
        }
    }

    fun resetAudioToIdle() {
        activeExtractJob?.cancel()
        activeExtractJob = null
        _audioUiState.value = AudioScreenState.Idle
    }

    fun retryAudio() {
        if (lastRequestedAudioUrl.isNotBlank()) {
            _audioUrlInput.value = lastRequestedAudioUrl
            fetchAudioSourceInfo()
        } else {
            resetAudioToIdle()
        }
    }

    // ---------------- Common Actions ----------------
    fun deleteHistoryItem(id: Long) {
        viewModelScope.launch {
            historyRepository.deleteHistory(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            historyRepository.clearAllHistory()
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun playAudio(uri: Uri, title: String, author: String?) {
        playerManager.playAudio(uri, title, author)
    }

    fun closeAudioPlayer() {
        playerManager.closeAudio()
    }

    // ---------------- Update System Actions ----------------
    fun checkForUpdate(isManual: Boolean = false) {
        viewModelScope.launch {
            if (isManual) {
                _updateState.value = UpdateState.Checking
            }
            val result = updateChecker.checkUpdate(settingsRepository.updateChannel.value)
            _updateState.value = result
            if (result is UpdateState.UpdateAvailable) {
                _showUpdateDialog.value = true
            }
        }
    }

    fun setUpdateChannel(channel: ReleaseChannel) {
        settingsRepository.setUpdateChannel(channel)
        updateChecker.invalidateCache()
        checkForUpdate(isManual = true)
    }

    fun startDownloadUpdate(asset: ReleaseAsset, release: GitHubRelease) {
        downloadJob?.cancel()
        downloadJob = viewModelScope.launch {
            updateDownloader.downloadApk(asset, release).collect { state ->
                _updateState.value = state
                if (state is UpdateState.ReadyToInstall) {
                    UpdateInstaller.installApk(getApplication(), state.apkFile)
                }
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun retryUpdateDownload() {
        val current = _updateState.value
        if (current is UpdateState.UpdateAvailable) {
            startDownloadUpdate(current.apkAsset, current.release)
        } else {
            checkForUpdate(isManual = true)
        }
    }

    fun installDownloadedUpdate() {
        val current = _updateState.value
        if (current is UpdateState.ReadyToInstall) {
            UpdateInstaller.installApk(getApplication(), current.apkFile)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerManager.releaseAll()
    }
}
