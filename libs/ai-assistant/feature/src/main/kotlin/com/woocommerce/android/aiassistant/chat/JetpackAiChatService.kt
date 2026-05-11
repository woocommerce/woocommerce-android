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
 * endpoint over SSE via OkHttp's `EventSource`.
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

            if (shouldRetryAuth(outcome, attempt)) {
                tokenProvider.invalidate()
                attempt++
                continue
            }
            outcome.failure?.let { emit(it) }
            return@flow
        }
    }

    private fun shouldRetryAuth(outcome: TurnOutcome, attempt: Int): Boolean =
        outcome.retryableAuthFailure &&
            attempt == 0 &&
            outcome.eventsEmitted == 0

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
            .post(buildRequestBody(request).toRequestBody(APPLICATION_JSON))
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
                            close(toAssistantException(null, it, bodyBytes, contentType).toException())
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

    private fun toAssistantException(
        t: Throwable?,
        response: Response?,
        bodyBytes: ByteArray? = null,
        contentType: MediaType? = null,
    ): MappedError {
        val logicalStatus = bodyBytes.logicalErrorStatus()
        val code = logicalStatus ?: response?.code
        val kind = code.toStreamError()
            ?: if (bodyBytes != null && response?.isSuccessful == true && contentType.isJson()) {
                // A JSON response means the SSE stream was not established, even when the proxy returns HTTP 200.
                ChatStreamError.BAD_REQUEST
            } else if (bodyBytes != null && response?.isSuccessful == true) {
                ChatStreamError.INVALID_STREAM
            } else {
                response.toStreamError() ?: t.toStreamError()
            }
        val diagnostics = Diagnostics(
            transport = if (bodyBytes == null) {
                transportDiagnosticsFactory.from(response)
            } else {
                transportDiagnosticsFactory.fromRawHttp(
                    statusCode = code,
                    headers = response?.headersMap(),
                    bodyBytes = bodyBytes,
                )
            }
        )
        return MappedError(kind, t, retryableAuthFailure = code == HTTP_UNAUTHORIZED, diagnostics = diagnostics)
    }

    private fun Response?.toStreamError(): ChatStreamError? = this?.code.toStreamError()

    private fun Int?.toStreamError(): ChatStreamError? = when (this) {
        HTTP_UNAUTHORIZED,
        HTTP_FORBIDDEN -> ChatStreamError.AUTH
        HTTP_REQUEST_TIMEOUT -> ChatStreamError.TIMEOUT
        HTTP_TOO_MANY_REQUESTS -> ChatStreamError.RATE_LIMIT
        HTTP_BAD_REQUEST -> ChatStreamError.BAD_REQUEST
        in HTTP_CLIENT_ERROR_RANGE -> ChatStreamError.BAD_REQUEST
        in HTTP_SERVER_ERROR_RANGE -> ChatStreamError.UPSTREAM_FAILURE
        else -> null
    }

    private fun ByteArray?.logicalErrorStatus(): Int? =
        this
            ?.decodeToString()
            ?.let { body ->
                runCatching {
                    json.parseToJsonElement(body)
                        .jsonObject["data"]
                        ?.jsonObject
                        ?.get(ERROR_STATUS_FIELD)
                        ?.jsonPrimitive
                        ?.contentOrNull
                        ?.toIntOrNull()
                }.getOrNull()
            }

    private fun Response.headersMap(): Map<String, String> =
        headers.names().associateWith { name -> header(name).orEmpty() }

    private fun MediaType?.isEventStream(): Boolean =
        this != null &&
            type.equals(SSE_CONTENT_TYPE, ignoreCase = true) &&
            subtype.equals(SSE_CONTENT_SUBTYPE, ignoreCase = true)

    private fun MediaType?.isJson(): Boolean =
        this != null && subtype.hasJsonSubtype()

    private fun String.hasJsonSubtype(): Boolean =
        equals(JSON_CONTENT_SUBTYPE, ignoreCase = true) ||
            endsWith(JSON_CONTENT_SUFFIX, ignoreCase = true)

    private fun Throwable?.toStreamError(): ChatStreamError = when (this) {
        is UnknownHostException,
        is ConnectException -> ChatStreamError.NETWORK
        is SocketTimeoutException -> ChatStreamError.TIMEOUT
        is IOException -> ChatStreamError.NETWORK
        else -> ChatStreamError.UNKNOWN
    }

    private fun mapError(t: Throwable): MappedError = when (t) {
        is MappedException -> MappedError(t.kind, t.cause, t.retryableAuthFailure, t.diagnostics)
        is AssistantAuthException -> MappedError(ChatStreamError.AUTH, t)
        is UnknownHostException, is ConnectException -> MappedError(ChatStreamError.NETWORK, t)
        is SocketTimeoutException -> MappedError(ChatStreamError.TIMEOUT, t)
        is IOException -> MappedError(ChatStreamError.NETWORK, t)
        else -> MappedError(ChatStreamError.UNKNOWN, t)
    }

    private fun buildRequestBody(request: ChatRequest): String = json.encodeToString(request.toOpenAi())

    private data class MappedError(
        val kind: ChatStreamError,
        val cause: Throwable?,
        val retryableAuthFailure: Boolean = false,
        val diagnostics: Diagnostics = Diagnostics(),
    ) {
        fun toException(): MappedException = MappedException(kind, cause, retryableAuthFailure, diagnostics)
    }

    private class MappedException(
        val kind: ChatStreamError,
        cause: Throwable?,
        val retryableAuthFailure: Boolean,
        val diagnostics: Diagnostics,
    ) : RuntimeException(kind.name, cause)

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
