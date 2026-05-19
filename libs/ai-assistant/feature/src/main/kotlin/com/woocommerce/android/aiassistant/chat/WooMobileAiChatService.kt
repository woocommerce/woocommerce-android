package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.auth.WpComOAuthTokenProvider
import com.woocommerce.android.aiassistant.chat.woomobileai.WooMobileAiRequestBuilder
import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
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

/**
 * Inactive wrapper chat service for `woo-mobile-ai`.
 *
 * This class is intentionally not bound as [ChatService] yet; production traffic
 * stays on [JetpackAiChatService] until the wrapper endpoint is ready to enable.
 */
@Suppress("LongParameterList", "TooManyFunctions")
internal class WooMobileAiChatService @Inject constructor(
    @AssistantOkHttpClient private val httpClient: OkHttpClient,
    private val tokenProvider: WpComOAuthTokenProvider,
    private val streamParser: ChatStreamParser,
    @AiAssistantJson private val json: Json,
    @AssistantBaseUrl private val baseUrl: String,
    private val transportDiagnosticsFactory: TransportDiagnosticsFactory,
    private val requestBuilder: WooMobileAiRequestBuilder,
    private val wrapperStreamErrorMapper: WrapperStreamErrorMapper,
) : ChatService {

    override fun streamTurn(request: ChatRequest): Flow<AssistantEvent> = flow {
        val outcome = collectOnce(request, ::emit)
        outcome.failure?.let { emit(it) }
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun collectOnce(
        request: ChatRequest,
        emitEvent: suspend (AssistantEvent) -> Unit,
    ): TurnOutcome {
        var failed: AssistantEvent.Failed? = null
        try {
            streamParser.parse(openStream(request)).collect { event ->
                if (event is AssistantEvent.Failed) {
                    failed = event
                } else {
                    emitEvent(event)
                }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            val mapped = mapError(e)
            return TurnOutcome(
                failure = AssistantEvent.Failed(mapped.kind, mapped.cause, mapped.diagnostics),
            )
        }
        return TurnOutcome(failure = failed)
    }

    private data class TurnOutcome(
        val failure: AssistantEvent.Failed?,
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
            .url(baseUrl.trimEnd('/') + WOO_MOBILE_AI_CHAT_COMPLETIONS_PATH)
            .header("Authorization", "Bearer $token")
            .header("Accept", "text/event-stream")
            .post(json.encodeToString(requestBuilder.build(request)).toRequestBody(APPLICATION_JSON))
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
                            val headers = it.headers.names().associateWith { name -> it.header(name).orEmpty() }
                            readEventStream(body.source()) { data ->
                                val mappedWrapperError = wrapperStreamErrorMapper.map(
                                    payload = data,
                                    fallbackHttpStatus = it.code,
                                    headers = headers,
                                    transportDiagnosticsFactory = transportDiagnosticsFactory,
                                )
                                if (mappedWrapperError != null) {
                                    close(
                                        StreamFailure(
                                            kind = mappedWrapperError.kind,
                                            cause = null,
                                            diagnostics = mappedWrapperError.diagnostics,
                                        ).toException()
                                    )
                                    false
                                } else {
                                    trySend(data).isSuccess
                                }
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
        val body = bodyBytes.decodeToString()
        val headers = response.headers.names().associateWith { response.header(it).orEmpty() }
        val mappedWrapperError = wrapperStreamErrorMapper.map(
            payload = body,
            fallbackHttpStatus = response.code,
            headers = headers,
            transportDiagnosticsFactory = transportDiagnosticsFactory,
        )
        if (mappedWrapperError != null) {
            return StreamFailure(
                kind = mappedWrapperError.kind,
                cause = null,
                diagnostics = mappedWrapperError.diagnostics,
            )
        }

        val kind = when (response.code) {
            HTTP_UNAUTHORIZED, HTTP_FORBIDDEN -> ChatStreamError.AUTH
            HTTP_REQUEST_TIMEOUT -> ChatStreamError.TIMEOUT
            HTTP_TOO_MANY_REQUESTS -> ChatStreamError.RATE_LIMIT
            HTTP_BAD_REQUEST -> ChatStreamError.BAD_REQUEST
            in HTTP_CLIENT_ERROR_RANGE -> ChatStreamError.BAD_REQUEST
            in HTTP_SERVER_ERROR_RANGE -> ChatStreamError.UPSTREAM_FAILURE
            else -> null
        } ?: when {
            response.isSuccessful && contentType.isJson() -> ChatStreamError.BAD_REQUEST
            response.isSuccessful -> ChatStreamError.INVALID_STREAM
            else -> ChatStreamError.UNKNOWN
        }

        return StreamFailure(
            kind = kind,
            cause = null,
            diagnostics = Diagnostics(
                transport = transportDiagnosticsFactory.fromRawHttp(
                    statusCode = response.code,
                    headers = headers,
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
        val diagnostics: Diagnostics = Diagnostics(),
    ) {
        fun toException() = StreamFailureException(this)
    }

    private class StreamFailureException(
        val failure: StreamFailure,
    ) : RuntimeException(failure.kind.name, failure.cause)

    private companion object {
        private const val WOO_MOBILE_AI_CHAT_COMPLETIONS_PATH = "/wpcom/v2/woo-mobile-ai/chat/completions"
        private val APPLICATION_JSON = "application/json".toMediaType()

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_BAD_REQUEST = 400
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_CLIENT_ERROR_RANGE = 400..499
        private val HTTP_SERVER_ERROR_RANGE = 500..599
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
