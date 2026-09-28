package com.example.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TikwmResponse(
    @Json(name = "code") val code: Int = -1,
    @Json(name = "msg") val msg: String? = null,
    @Json(name = "data") val data: TikwmData? = null
)

@JsonClass(generateAdapter = true)
data class TikwmData(
    @Json(name = "id") val id: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "cover") val cover: String? = null,
    @Json(name = "origin_cover") val originCover: String? = null,
    @Json(name = "duration") val duration: Long? = null,
    @Json(name = "play") val play: String? = null,
    @Json(name = "wmplay") val wmplay: String? = null,
    @Json(name = "hdplay") val hdplay: String? = null,
    @Json(name = "size") val size: Long? = null,
    @Json(name = "hd_size") val hdSize: Long? = null,
    @Json(name = "wm_size") val wmSize: Long? = null,
    @Json(name = "author") val author: TikwmAuthor? = null
)

@JsonClass(generateAdapter = true)
data class TikwmAuthor(
    @Json(name = "id") val id: String? = null,
    @Json(name = "unique_id") val uniqueId: String? = null,
    @Json(name = "nickname") val nickname: String? = null,
    @Json(name = "avatar") val avatar: String? = null
)

data class VideoInfo(
    val id: String,
    val title: String,
    val coverUrl: String?,
    val durationSeconds: Long,
    val standardPlayUrl: String,
    val hdPlayUrl: String?,
    val wmPlayUrl: String?,
    val standardSizeBytes: Long?,
    val hdSizeBytes: Long?,
    val authorNickname: String?,
    val authorUsername: String?,
    val authorAvatarUrl: String?,
    val originalSourceUrl: String
) {
    fun getBestDownloadUrl(preferHd: Boolean = true): String {
        return if (preferHd && !hdPlayUrl.isNullOrBlank()) {
            hdPlayUrl
        } else if (!standardPlayUrl.isBlank()) {
            standardPlayUrl
        } else {
            wmPlayUrl ?: ""
        }
    }

    fun getEstimatedSize(preferHd: Boolean = true): Long? {
        return if (preferHd && hdSizeBytes != null && hdSizeBytes > 0) {
            hdSizeBytes
        } else {
            standardSizeBytes
        }
    }
}
