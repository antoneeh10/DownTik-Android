package com.example.downloader

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.network.NetworkClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer

sealed interface AudioExtractionState {
    data object Idle : AudioExtractionState
    data class Progress(
        val stage: AudioStage,
        val progress: Float, // 0.0 to 1.0
        val percentageText: String,
        val statusMessage: String
    ) : AudioExtractionState
    data class Completed(
        val uri: Uri,
        val fileName: String,
        val totalBytes: Long
    ) : AudioExtractionState
    data class Failed(val message: String) : AudioExtractionState
}

enum class AudioStage {
    DOWNLOADING_SOURCE,
    EXTRACTING_AUDIO,
    SAVING_AUDIO
}

class AudioExtractor(private val context: Context) {

    private val okHttpClient = NetworkClient.okHttpClient

    fun extractAudio(
        videoUrl: String,
        title: String,
        durationSeconds: Long
    ): Flow<AudioExtractionState> = flow {
        emit(
            AudioExtractionState.Progress(
                stage = AudioStage.DOWNLOADING_SOURCE,
                progress = 0.05f,
                percentageText = "5%",
                statusMessage = "Mengunduh sumber video..."
            )
        )

        val safeAudioName = DownTikStorage.sanitizeFileName(title, "m4a")
        val tempSourceFile = File(context.cacheDir, "downtik_src_${System.currentTimeMillis()}.mp4")
        val tempAudioFile = File(context.cacheDir, "downtik_audio_${System.currentTimeMillis()}.m4a")

        var targetUri: Uri? = null
        var legacyFile: File? = null

        try {
            // STEP 1: Download video source to temporary cache
            val request = Request.Builder()
                .url(videoUrl)
                .addHeader("Referer", "https://www.tiktok.com/")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                emit(AudioExtractionState.Failed("Gagal mengambil sumber video (HTTP ${response.code})"))
                return@flow
            }

            val body = response.body
            if (body == null) {
                emit(AudioExtractionState.Failed("Data video sumber kosong"))
                return@flow
            }

            val totalBytes = body.contentLength()
            val inputStream: InputStream = body.byteStream()
            val fileOut = FileOutputStream(tempSourceFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            var totalRead = 0L
            var lastUpdateMs = System.currentTimeMillis()

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                fileOut.write(buffer, 0, bytesRead)
                totalRead += bytesRead

                val now = System.currentTimeMillis()
                if (now - lastUpdateMs > 120 || totalRead == totalBytes) {
                    lastUpdateMs = now
                    val downloadFraction = if (totalBytes > 0) (totalRead.toFloat() / totalBytes.toFloat()) else 0.5f
                    // Download phase covers 0% to 50%
                    val overallProgress = (downloadFraction * 0.5f).coerceIn(0.05f, 0.50f)
                    val percent = "${(overallProgress * 100).toInt()}%"
                    emit(
                        AudioExtractionState.Progress(
                            stage = AudioStage.DOWNLOADING_SOURCE,
                            progress = overallProgress,
                            percentageText = percent,
                            statusMessage = "Mengunduh sumber video... ($percent)"
                        )
                    )
                }
            }
            fileOut.flush()
            fileOut.close()

            // STEP 2: Extract audio track with MediaExtractor & MediaMuxer
            emit(
                AudioExtractionState.Progress(
                    stage = AudioStage.EXTRACTING_AUDIO,
                    progress = 0.55f,
                    percentageText = "55%",
                    statusMessage = "Mengekstrak track audio..."
                )
            )

            val totalDurationUs = if (durationSeconds > 0) durationSeconds * 1_000_000L else 15_000_000L
            demuxAudioTrack(tempSourceFile, tempAudioFile, totalDurationUs) { extractProgress ->
                // Extraction covers 50% to 85%
                val overallProgress = (0.50f + (extractProgress * 0.35f)).coerceIn(0.50f, 0.85f)
                val percent = "${(overallProgress * 100).toInt()}%"
                emit(
                    AudioExtractionState.Progress(
                        stage = AudioStage.EXTRACTING_AUDIO,
                        progress = overallProgress,
                        percentageText = percent,
                        statusMessage = "Mengekstrak audio... ($percent)"
                    )
                )
            }

            // STEP 3: Save extracted audio to Download/DownTik/Audio/
            emit(
                AudioExtractionState.Progress(
                    stage = AudioStage.SAVING_AUDIO,
                    progress = 0.90f,
                    percentageText = "90%",
                    statusMessage = "Menyimpan file audio ke Download/DownTik/Audio..."
                )
            )

            val audioFileSize = tempAudioFile.length()
            val audioIn = FileInputStream(tempAudioFile)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, safeAudioName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "audio/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, DownTikStorage.AUDIO_FOLDER)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val collectionUri = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                targetUri = resolver.insert(collectionUri, contentValues)
                    ?: throw IllegalStateException("Tidak dapat membuat entri penyimpanan audio")

                val outStream = resolver.openOutputStream(targetUri)
                    ?: throw IllegalStateException("Tidak dapat membuka stream penyimpanan audio")

                audioIn.copyTo(outStream)
                outStream.flush()
                outStream.close()

                val finalizeValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_PENDING, 0)
                }
                resolver.update(targetUri, finalizeValues, null, null)
            } else {
                @Suppress("DEPRECATION")
                val audioDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "DownTik/Audio"
                )
                if (!audioDir.exists()) {
                    audioDir.mkdirs()
                }
                val destFile = File(audioDir, safeAudioName)
                legacyFile = destFile
                val outStream = FileOutputStream(destFile)
                audioIn.copyTo(outStream)
                outStream.flush()
                outStream.close()

                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(destFile.absolutePath),
                    arrayOf("audio/mp4"),
                    null
                )
                targetUri = Uri.fromFile(destFile)
            }
            audioIn.close()

            emit(
                AudioExtractionState.Completed(
                    uri = targetUri ?: Uri.EMPTY,
                    fileName = safeAudioName,
                    totalBytes = audioFileSize
                )
            )

        } catch (e: Exception) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetUri != null) {
                context.contentResolver.runCatching { delete(targetUri, null, null) }
            } else if (legacyFile != null && legacyFile.exists()) {
                legacyFile.delete()
            }
            emit(AudioExtractionState.Failed("Gagal mengekstrak audio: ${e.localizedMessage ?: "Terjadi kesalahan"}"))
        } finally {
            // Clean up temporary cache files
            tempSourceFile.delete()
            tempAudioFile.delete()
        }
    }.flowOn(Dispatchers.IO)

    /**
     * Natively demuxes the AAC audio track from an MP4 video into an M4A audio container
     * using MediaExtractor and MediaMuxer. Zero re-encoding loss.
     */
    private suspend fun demuxAudioTrack(
        sourceVideoFile: File,
        targetAudioFile: File,
        estimatedDurationUs: Long,
        onProgress: suspend (Float) -> Unit
    ) {
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null

        try {
            extractor.setDataSource(sourceVideoFile.absolutePath)
            var audioTrackIndex = -1
            var audioFormat: MediaFormat? = null

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndex = i
                    audioFormat = format
                    break
                }
            }

            if (audioTrackIndex == -1 || audioFormat == null) {
                throw IllegalStateException("Video tidak memiliki track audio yang dapat diekstrak")
            }

            extractor.selectTrack(audioTrackIndex)

            val maxBufferSize = if (audioFormat.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                audioFormat.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
            } else {
                512 * 1024
            }

            muxer = MediaMuxer(targetAudioFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerAudioTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var lastProgressEmitUs = 0L

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)

                if (bufferInfo.size < 0) {
                    bufferInfo.size = 0
                    break
                }

                bufferInfo.presentationTimeUs = extractor.sampleTime
                bufferInfo.flags = extractor.sampleFlags
                muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)

                if (bufferInfo.presentationTimeUs - lastProgressEmitUs > 500_000L) {
                    lastProgressEmitUs = bufferInfo.presentationTimeUs
                    val frac = if (estimatedDurationUs > 0) {
                        (bufferInfo.presentationTimeUs.toFloat() / estimatedDurationUs.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0.5f
                    }
                    onProgress(frac)
                }

                extractor.advance()
            }

            muxer.stop()
            onProgress(1f)
        } finally {
            try { muxer?.release() } catch (_: Exception) {}
            try { extractor.release() } catch (_: Exception) {}
        }
    }
}
