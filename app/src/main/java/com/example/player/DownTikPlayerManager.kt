package com.example.player

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.ForwardingAudioSink
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.nio.ByteBuffer

data class PlayingAudioInfo(
    val uri: Uri,
    val title: String,
    val author: String?,
    val durationMs: Long = 0L
)

@OptIn(UnstableApi::class)
class DownTikPlayerManager(private val context: Context) {

    private fun createTolerantRenderersFactory(): DefaultRenderersFactory {
        return object : DefaultRenderersFactory(context) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink? {
                val baseSink = DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()

                return object : ForwardingAudioSink(baseSink) {
                    override fun handleBuffer(
                        buffer: ByteBuffer,
                        presentationTimeUs: Long,
                        encodedAccessUnitCount: Int
                    ): Boolean {
                        return try {
                            super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
                        } catch (e: AudioSink.UnexpectedDiscontinuityException) {
                            // Recover gracefully from audio track timestamp jumps (frequent in TikTok CDN streams)
                            true
                        } catch (e: Exception) {
                            super.handleBuffer(buffer, presentationTimeUs, encodedAccessUnitCount)
                        }
                    }
                }
            }
        }
    }

    private var _videoPlayer: ExoPlayer? = null
    val videoPlayer: ExoPlayer
        get() {
            if (_videoPlayer == null) {
                _videoPlayer = ExoPlayer.Builder(context, createTolerantRenderersFactory())
                    .build()
                    .apply {
                        repeatMode = Player.REPEAT_MODE_ONE
                        playWhenReady = true
                        addListener(object : Player.Listener {
                            override fun onPlayerError(error: PlaybackException) {
                                // Auto-recover from transient errors
                                if (error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED ||
                                    error.errorCode == PlaybackException.ERROR_CODE_DECODING_FAILED
                                ) {
                                    prepare()
                                    play()
                                }
                            }
                        })
                    }
            }
            return _videoPlayer!!
        }

    private var _audioPlayer: ExoPlayer? = null
    val audioPlayer: ExoPlayer
        get() {
            if (_audioPlayer == null) {
                _audioPlayer = ExoPlayer.Builder(context, createTolerantRenderersFactory()).build().apply {
                    repeatMode = Player.REPEAT_MODE_OFF
                    playWhenReady = true
                    addListener(object : Player.Listener {
                        override fun onIsPlayingChanged(isPlaying: Boolean) {
                            _isAudioPlaying.value = isPlaying
                        }
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            if (playbackState == Player.STATE_ENDED) {
                                _isAudioPlaying.value = false
                            }
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            if (error.errorCode == PlaybackException.ERROR_CODE_AUDIO_TRACK_WRITE_FAILED) {
                                prepare()
                                play()
                            }
                        }
                    })
                }
            }
            return _audioPlayer!!
        }

    private val _currentPlayingAudio = MutableStateFlow<PlayingAudioInfo?>(null)
    val currentPlayingAudio: StateFlow<PlayingAudioInfo?> = _currentPlayingAudio.asStateFlow()

    private val _isAudioPlaying = MutableStateFlow(false)
    val isAudioPlaying: StateFlow<Boolean> = _isAudioPlaying.asStateFlow()

    fun prepareAndPlayVideo(videoUrl: String) {
        val player = videoPlayer
        val mediaItem = MediaItem.fromUri(videoUrl)
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun pauseVideo() {
        _videoPlayer?.pause()
    }

    fun stopVideo() {
        _videoPlayer?.stop()
        _videoPlayer?.clearMediaItems()
    }

    fun playAudio(uri: Uri, title: String, author: String?) {
        val player = audioPlayer
        val mediaMetadata = MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(author ?: "DownTik Audio")
            .build()

        val mediaItem = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(mediaMetadata)
            .build()

        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true

        _currentPlayingAudio.value = PlayingAudioInfo(
            uri = uri,
            title = title,
            author = author
        )
    }

    fun toggleAudioPlayPause() {
        _audioPlayer?.let { player ->
            if (player.isPlaying) {
                player.pause()
            } else {
                player.play()
            }
        }
    }

    fun closeAudio() {
        _audioPlayer?.stop()
        _audioPlayer?.clearMediaItems()
        _currentPlayingAudio.value = null
        _isAudioPlaying.value = false
    }

    fun releaseAll() {
        _videoPlayer?.release()
        _videoPlayer = null

        _audioPlayer?.release()
        _audioPlayer = null
        _currentPlayingAudio.value = null
        _isAudioPlaying.value = false
    }
}
