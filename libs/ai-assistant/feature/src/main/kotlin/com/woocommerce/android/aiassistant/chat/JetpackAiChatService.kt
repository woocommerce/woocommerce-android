package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.chat.openai.toOpenAi
import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.Diagnostics
import com.woocommerce.android.aiassistant.di.AiAssistantJson
import com.woocommerce.android.aiassistant.di.AssistantBaseUrl
import com.woocommerce.android.aiassistant.di.AssistantOkHttpClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okio.BufferedSource
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [ChatService] implementation that calls the wpcom-hosted `jetpack-ai-query`
 * endpoint over SSE via OkHttp with manual SSE line parsing.
 *
 * On HTTP 401 received before any data has been emitted, the JWT cache is
 * invalidated and the request is retried exactly once. After any data has
 * been emitted, an `AUTH` failure is surfaced upward and the retry decision
 * belongs to the loop.
 */
@Singleton
internal class JetpackAiChatService @Inject constructor(
    @AssistantOkHttpClient private val httpClient: OkHttpClient,
    private val tokenProvider: JwtTokenProvider,
    private val streamParser: ChatStreamParser,
    @AiAssistantJson private val json: Json,
    @AssistantBaseUrl private val baseUrl: String,
    private val transportDiagnosticsFactory: TransportDiagnosticsFactory,
) : ChatService {

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
        var attempt = 0
        while (true) {
            val outcome = collectOnce(request, ::emit)

            if (outcome.retryableAuthFailure && attempt == 0 && outcome.eventsEmitted == 0) {
                tokenProvider.invalidate()
                attempt++
                continue
            }
            outcome.failure?.let { emit(it) }
            return@flow
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun collectOnce(
        request: ChatRequest,
        emitEvent: suspend (AssistantEvent) -> Unit,
    ): TurnOutcome {
        var eventsEmitted = 0
        var failed: AssistantEvent.Failed? = null
        try {
            streamParser.parse(openStream(request)).collect { event ->
                if (event is AssistantEvent.Failed) {
                    failed = event
                } else {
                    emitEvent(event)
                    eventsEmitted++
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val mapped = mapError(e)
            return TurnOutcome(
                eventsEmitted = eventsEmitted,
                failure = AssistantEvent.Failed(mapped.kind, mapped.cause, mapped.diagnostics),
                retryableAuthFailure = mapped.retryableAuthFailure,
            )
        }
        return TurnOutcome(
            eventsEmitted = eventsEmitted,
            failure = failed,
            retryableAuthFailure = false,
        )
    }

    private data class TurnOutcome(
        val eventsEmitted: Int,
        val failure: AssistantEvent.Failed?,
        val retryableAuthFailure: Boolean,
    )

    @Suppress("TooGenericExceptionCaught")
    private fun openStream(request: ChatRequest): Flow<String> = callbackFlow {
        val token = try {
            tokenProvider.provide()
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            close(mapError(e).toException())
            return@callbackFlow
        }
        val httpRequest = Request.Builder()
            .url(baseUrl.trimEnd('/') + JETPACK_AI_QUERY_PATH)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .post(json.encodeToString(request.toOpenAi()).toRequestBody(APPLICATION_JSON))
            .build()

        val call = httpClient.newCall(httpRequest)
        call.enqueue(
            object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (call.isCanceled()) {
                        close()
                    } else {
                        close(mapError(e).toException())
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    response.use {
                        val body = it.body
                        val contentType = body.contentType()
                        if (!it.isSuccessful || !contentType.isEventStream()) {
                            val bodyBytes = body.bytes()
                            close(mapResponseError(it, bodyBytes, contentType).toException())
                            return
                        }

                        try {
                            readEventStream(body.source()) { data ->
                                trySend(data).isSuccess
                            }
                            close()
                        } catch (e: IOException) {
                            if (call.isCanceled()) {
                                close()
                            } else {
                                close(mapError(e).toException())
                            }
                        } catch (ce: CancellationException) {
                            throw ce
                        } catch (e: Exception) {
                            close(mapError(e).toException())
                        }
                    }
                }
            }
        )

        awaitClose { call.cancel() }
    }.buffer(Channel.UNLIMITED)

    private fun readEventStream(
        source: BufferedSource,
        emitData: (String) -> Boolean,
    ) {
        val dataLines = mutableListOf<String>()
        var keepReading = true
        while (keepReading) {
            val line = source.readUtf8Line()
            keepReading = when {
                line == null -> false
                line.isEmpty() -> flushEventData(dataLines, emitData)
                else -> {
                    line.toSseDataValue()?.let(dataLines::add)
                    true
                }
            }
        }
        if (dataLines.isNotEmpty()) {
            emitData(dataLines.joinToString(separator = "\n"))
        }
    }

    private fun flushEventData(
        dataLines: MutableList<String>,
        emitData: (String) -> Boolean,
    ): Boolean {
        if (dataLines.isEmpty()) return true
        val keepReading = emitData(dataLines.joinToString(separator = "\n"))
        dataLines.clear()
        return keepReading
    }

    private fun String.toSseDataValue(): String? {
        if (startsWith(SSE_COMMENT_PREFIX)) return null

        val separatorIndex = indexOf(SSE_FIELD_SEPARATOR)
        val fieldName = if (separatorIndex == -1) this else substring(0, separatorIndex)
        if (fieldName != SSE_DATA_FIELD) return null

        val rawValue = if (separatorIndex == -1) "" else substring(separatorIndex + 1)
        return rawValue.removePrefix(SSE_VALUE_PREFIX)
    }

    private fun mapResponseError(
        response: Response,
        bodyBytes: ByteArray,
        contentType: MediaType?,
    ): StreamFailure {
        val logicalStatus = runCatching {
            json.parseToJsonElement(bodyBytes.decodeToString())
                .jsonObject["data"]
                ?.jsonObject
                ?.get(ERROR_STATUS_FIELD)
                ?.jsonPrimitive
                ?.contentOrNull
                ?.toIntOrNull()
        }.getOrNull()

        val code = logicalStatus ?: response.code

        val kind = when (code) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ChatStreamError.AUTH
            HTTP_REQUEST_TIMEOUT -> ChatStreamError.TIMEOUT
            HTTP_TOO_MANY_REQUESTS -> ChatStreamError.RATE_LIMIT
            HTTP_BAD_REQUEST -> ChatStreamError.BAD_REQUEST
            in HTTP_CLIENT_ERROR_RANGE -> ChatStreamError.BAD_REQUEST
            in HTTP_SERVER_ERROR_RANGE -> ChatStreamError.UPSTREAM_FAILURE
            else -> null
        } ?: when {
            // A JSON response means the SSE stream was not established, even when the proxy returns HTTP 200.
            response.isSuccessful && contentType.isJson() -> ChatStreamError.BAD_REQUEST
            response.isSuccessful -> ChatStreamError.INVALID_STREAM
            else -> ChatStreamError.UNKNOWN
        }

        return StreamFailure(
            kind = kind,
            cause = null,
            retryableAuthFailure = code == HTTP_UNAUTHORIZED,
            diagnostics = Diagnostics(
                transport = transportDiagnosticsFactory.fromRawHttp(
                    statusCode = code,
                    headers = response.headers.names()
                        .associateWith { response.header(it).orEmpty() },
                    bodyBytes = bodyBytes,
                )
            ),
        )
    }

    private fun MediaType?.isEventStream(): Boolean =
        this != null &&
            type.equals(SSE_CONTENT_TYPE, ignoreCase = true) &&
            subtype.equals(SSE_CONTENT_SUBTYPE, ignoreCase = true)

    private fun MediaType?.isJson(): Boolean {
        if (this == null) return false
        return subtype.equals(JSON_CONTENT_SUBTYPE, ignoreCase = true) ||
            subtype.endsWith(JSON_CONTENT_SUFFIX, ignoreCase = true)
    }

    private fun mapError(t: Throwable): StreamFailure = when (t) {
        is StreamFailureException -> t.failure
        is AssistantAuthException -> StreamFailure(ChatStreamError.AUTH, t)
        is UnknownHostException, is ConnectException -> StreamFailure(ChatStreamError.NETWORK, t)
        is SocketTimeoutException -> StreamFailure(ChatStreamError.TIMEOUT, t)
        is IOException -> StreamFailure(ChatStreamError.NETWORK, t)
        else -> StreamFailure(ChatStreamError.UNKNOWN, t)
    }

    private data class StreamFailure(
        val kind: ChatStreamError,
        val cause: Throwable?,
        val retryableAuthFailure: Boolean = false,
        val diagnostics: Diagnostics = Diagnostics(),
    ) {
        fun toException() = StreamFailureException(this)
    }

    private class StreamFailureException(
        val failure: StreamFailure,
    ) : RuntimeException(failure.kind.name, failure.cause)

    companion object {
        internal const val DEFAULT_BASE_URL = "https://public-api.wordpress.com"
        private const val JETPACK_AI_QUERY_PATH = "/wpcom/v2/jetpack-ai-query"
        private val APPLICATION_JSON = "application/json".toMediaType()

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
        private val HTTP_SERVER_ERROR_RANGE = 500..599
        private const val ERROR_STATUS_FIELD = "status"
        private const val SSE_COMMENT_PREFIX = ":"
        private const val SSE_DATA_FIELD = "data"
        private const val SSE_FIELD_SEPARATOR = ':'
        private const val SSE_VALUE_PREFIX = " "
        private const val SSE_CONTENT_TYPE = "text"
        private const val SSE_CONTENT_SUBTYPE = "event-stream"
        private const val JSON_CONTENT_SUBTYPE = "json"
        private const val JSON_CONTENT_SUFFIX = "+json"
    }
}
