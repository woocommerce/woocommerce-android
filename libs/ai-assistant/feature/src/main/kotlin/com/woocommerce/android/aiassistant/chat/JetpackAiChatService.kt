package com.woocommerce.android.aiassistant.chat

import com.woocommerce.android.aiassistant.chat.openai.toOpenAi
import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import com.woocommerce.android.aiassistant.core.auth.JwtTokenProvider
import com.woocommerce.android.aiassistant.core.chat.ChatStreamError
import com.woocommerce.android.aiassistant.core.chat.AssistantEvent
import com.woocommerce.android.aiassistant.core.chat.ChatRequest
import com.woocommerce.android.aiassistant.core.chat.ChatService
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
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
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
                failure = AssistantEvent.Failed(mapped.kind, mapped.cause),
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

        val listener = object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                trySend(data)
            }

            override fun onClosed(eventSource: EventSource) {
                close()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                close(toAssistantException(t, response).toException())
            }
        }

        val eventSource = EventSources.createFactory(httpClient).newEventSource(httpRequest, listener)
        awaitClose { eventSource.cancel() }
    }.buffer(Channel.UNLIMITED)

    private fun toAssistantException(t: Throwable?, response: Response?): MappedError {
        val code = response?.code
        val kind = when {
            code == HTTP_UNAUTHORIZED || code == HTTP_FORBIDDEN -> ChatStreamError.AUTH
            code == HTTP_REQUEST_TIMEOUT -> ChatStreamError.TIMEOUT
            code == HTTP_TOO_MANY_REQUESTS -> ChatStreamError.RATE_LIMIT
            code != null && code in HTTP_SERVER_ERROR_RANGE -> ChatStreamError.UPSTREAM_FAILURE
            t is UnknownHostException || t is ConnectException -> ChatStreamError.NETWORK
            t is SocketTimeoutException -> ChatStreamError.TIMEOUT
            t is IOException -> ChatStreamError.NETWORK
            else -> ChatStreamError.UNKNOWN
        }
        return MappedError(kind, t, retryableAuthFailure = code == HTTP_UNAUTHORIZED)
    }

    private fun mapError(t: Throwable): MappedError = when (t) {
        is MappedException -> MappedError(t.kind, t.cause, t.retryableAuthFailure)
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
    ) {
        fun toException(): MappedException = MappedException(kind, cause, retryableAuthFailure)
    }

    private class MappedException(
        val kind: ChatStreamError,
        cause: Throwable?,
        val retryableAuthFailure: Boolean,
    ) : RuntimeException(kind.name, cause)

    companion object {
        internal const val DEFAULT_BASE_URL = "https://public-api.wordpress.com"
        private const val JETPACK_AI_QUERY_PATH = "/wpcom/v2/jetpack-ai-query"
        private val APPLICATION_JSON = "application/json".toMediaType()

        private const val HTTP_UNAUTHORIZED = 401
        private const val HTTP_FORBIDDEN = 403
        private const val HTTP_REQUEST_TIMEOUT = 408
        private const val HTTP_TOO_MANY_REQUESTS = 429
        private val HTTP_SERVER_ERROR_RANGE = 500..599
    }
}
