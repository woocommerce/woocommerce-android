package org.wordpress.android.fluxc.network.rest.wpapi.media

import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONException
import org.json.JSONObject
import org.wordpress.android.fluxc.annotations.endpoint.WPAPIEndpoint
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest
import org.wordpress.android.fluxc.store.MediaStore.FetchMediaListResponsePayload
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaPayload
import org.wordpress.android.fluxc.store.MediaStore.ProgressPayload
import org.wordpress.android.fluxc.utils.MimeType
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.MEDIA
import java.io.IOException
import javax.inject.Named

abstract class BaseWPV2MediaRestClient(
    @Named("regular") private val okHttpClient: OkHttpClient,
    private val gson: Gson
) {
    protected abstract fun WPAPIEndpoint.getFullUrl(site: SiteModel): String

    protected abstract suspend fun getAuthorizationHeader(site: SiteModel): AuthorizationHeaderResult

    protected abstract suspend fun <T : Any> executeGetGsonRequest(
        site: SiteModel,
        endpoint: WPAPIEndpoint,
        params: Map<String, String>,
        clazz: Class<T>
    ): WPAPIResponse<T>

    @Suppress("TooGenericExceptionCaught", "SwallowedException")
    fun uploadMedia(site: SiteModel, media: MediaModel): Flow<ProgressPayload> {
        fun ProducerScope<ProgressPayload>.handleFailure(media: MediaModel, error: MediaError) {
            val payload = ProgressPayload(media, 1f, false, error)
            trySendBlocking(payload)
            close()
        }

        return callbackFlow {
            val authorizationHeader = when (val result = getAuthorizationHeader(site)) {
                is AuthorizationHeaderResult.Success -> result.header
                is AuthorizationHeaderResult.Failure -> {
                    handleFailure(media, result.error)
                    return@callbackFlow
                }
            }

            val url = WPAPI.media.getFullUrl(site)
            val body = WPRestUploadRequestBody(media) { media, progress ->
                val payload = ProgressPayload(media, progress, false, null)
                trySend(payload)
            }

            val request = Request.Builder()
                .url(url)
                .post(body = body)
                .header(WPComGsonRequest.REST_AUTHORIZATION_HEADER, authorizationHeader)
                .build()

            val call = okHttpClient.newCall(request)

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    val message = "media upload failed: $e"
                    AppLog.w(MEDIA, message)
                    val error = MediaError.fromIOException(e)
                    error.logMessage = message
                    handleFailure(media, error)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        try {
                            val res = gson.fromJson(response.body!!.string(), MediaWPRESTResponse::class.java)
                            val uploadedMedia = res.toMediaModel(site.id)
                            uploadedMedia.id = media.id
                            val payload = ProgressPayload(uploadedMedia, 1f, true, false)
                            trySendBlocking(payload)
                            close()
                        } catch (e: JsonSyntaxException) {
                            AppLog.e(MEDIA, e)
                            val error = MediaError(MediaErrorType.PARSE_ERROR)
                            handleFailure(media, error)
                        } catch (e: NullPointerException) {
                            AppLog.e(MEDIA, e)
                            val error = MediaError(MediaErrorType.PARSE_ERROR)
                            handleFailure(media, error)
                        }
                    } else {
                        val error = response.parseUploadError()
                        handleFailure(media, error)
                    }
                }
            })

            awaitClose {
                call.cancel()
            }
        }
    }

    protected sealed class AuthorizationHeaderResult {
        data class Success(val header: String) : AuthorizationHeaderResult()
        data class Failure(val error: MediaError) : AuthorizationHeaderResult()
    }

    suspend fun fetchMedia(site: SiteModel, mediaId: Long): MediaPayload {
        val url = WPAPI.media.id(mediaId)
        val response = executeGetGsonRequest(
            site,
            url,
            emptyMap(),
            MediaWPRESTResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Error -> {
                val errorMessage = "Failed to fetch media. Response: $response"
                AppLog.w(MEDIA, errorMessage)
                val error = MediaError(MediaErrorType.fromBaseNetworkError(response.error))
                error.statusCode = response.error.volleyError?.networkResponse?.statusCode ?: 0
                error.apiErrorCode = response.error.errorCode
                error.logMessage = errorMessage
                MediaPayload(site, null, error)
            }

            is WPAPIResponse.Success -> {
                val fetchedMedia = response.data?.toMediaModel(site.id)
                when {
                    fetchedMedia != null -> {
                        AppLog.v(MEDIA, "Fetched media successfully for mediaId: $mediaId")
                        MediaPayload(site, fetchedMedia)
                    }

                    else -> {
                        AppLog.w(
                            MEDIA,
                            "Request successful but fetched media is null for mediaId: $mediaId"
                        )
                        MediaPayload(site, null, MediaError(MediaErrorType.NULL_MEDIA_ARG))
                    }
                }
            }
        }
    }

    suspend fun fetchMediaList(
        site: SiteModel,
        perPage: Int,
        offset: Int,
        mimeType: MimeType.Type?
    ): FetchMediaListResponsePayload {
        val params = mutableMapOf(
            "per_page" to perPage.toString()
        )
        if (offset > 0) {
            params["offset"] = offset.toString()
        }
        if (mimeType != null) {
            params["media_type"] = mimeType.value
        }
        val response = executeGetGsonRequest(
            site,
            WPAPI.media,
            params,
            Array<MediaWPRESTResponse>::class.java
        )

        return when (response) {
            is WPAPIResponse.Error -> {
                val errorMessage = "Failed to fetch media list. Response: $response"
                AppLog.w(MEDIA, errorMessage)
                val error = MediaError(MediaErrorType.fromBaseNetworkError(response.error))
                error.statusCode = response.error.volleyError?.networkResponse?.statusCode ?: 0
                error.apiErrorCode = response.error.errorCode
                error.logMessage = errorMessage
                FetchMediaListResponsePayload(site, error, mimeType)
            }

            is WPAPIResponse.Success -> {
                val mediaList = response.data.orEmpty().map { it.toMediaModel(site.id) }
                AppLog.v(MEDIA, "Fetched media list for site with size: " + mediaList.size)
                val canLoadMore = mediaList.size == perPage
                FetchMediaListResponsePayload(site, mediaList, offset > 0, canLoadMore, mimeType)
            }
        }
    }

    private fun Response.parseUploadError(): MediaError {
        val mediaError = MediaError(MediaErrorType.fromHttpStatusCode(code))
        mediaError.statusCode = code
        mediaError.logMessage = message
        if (mediaError.type == MediaErrorType.REQUEST_TOO_LARGE) {
            // 413 (Request too large) errors are coming from the web server and are not an API response like the rest
            mediaError.message = message
            return mediaError
        }
        try {
            val responseBody = body
            if (responseBody == null) {
                AppLog.e(MEDIA, "error uploading media, response body was empty $this")
                mediaError.type = MediaErrorType.PARSE_ERROR
                return mediaError
            }
            val jsonBody = JSONObject(responseBody.string())
            jsonBody.optString("message").takeIf { it.isNotEmpty() }?.let {
                mediaError.message = it
            }
            jsonBody.optString("code").takeIf { it.isNotEmpty() }?.let {
                mediaError.apiErrorCode = it
                mediaError.logMessage = it
            }
        } catch (e: JSONException) {
            // no op
            mediaError.logMessage = e.message
        } catch (e: IOException) {
            mediaError.logMessage = e.message
        }
        return mediaError
    }
}
