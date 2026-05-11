package com.woocommerce.android.ui.aisupportchat.networking

import com.android.volley.RequestQueue
import com.android.volley.VolleyError
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequest.WPComGsonNetworkError
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken

@ExperimentalCoroutinesApi
class SupportChatRestClientTest : BaseUnitTest() {
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder = mock()
    private val dispatcher: Dispatcher = mock()
    private val requestQueue: RequestQueue = mock()
    private val accessToken: AccessToken = mock()
    private val userAgent: UserAgent = mock()

    private lateinit var urlCaptor: KArgumentCaptor<String>
    private lateinit var bodyCaptor: KArgumentCaptor<Map<String, Any>>
    private lateinit var restClient: SupportChatRestClient

    @Before
    fun setUp() {
        urlCaptor = argumentCaptor()
        bodyCaptor = argumentCaptor()
        restClient = SupportChatRestClient(
            wpComGsonRequestBuilder = wpComGsonRequestBuilder,
            appContext = null,
            dispatcher = dispatcher,
            requestQueue = requestQueue,
            accessToken = accessToken,
            userAgent = userAgent
        )
    }

    @Test
    fun `given new chat, when sendMessage, then posts to slug-only URL with message and context`() =
        testBlocking {
            stubPostResponse()

            restClient.sendMessage(
                botSlug = BOT_SLUG,
                message = MESSAGE,
                context = JsonObject().apply {
                    addProperty("site_id", 1L)
                    addProperty("app_version", "0.1")
                }
            )

            assertThat(urlCaptor.firstValue)
                .isEqualTo("https://public-api.wordpress.com/wpcom/v2/odie/chat/$BOT_SLUG/")
            assertThat(bodyCaptor.firstValue).containsOnlyKeys("message", "context")
            assertThat(bodyCaptor.firstValue["message"]).isEqualTo(MESSAGE)
            val sentContext = bodyCaptor.firstValue["context"] as JsonObject
            assertThat(sentContext["site_id"].asLong).isEqualTo(1L)
            assertThat(sentContext["app_version"].asString).isEqualTo("0.1")
        }

    @Test
    fun `given existing chat, when sendFollowUpMessage, then posts to slug-and-id URL with message only`() =
        testBlocking {
            stubPostResponse()

            val result = restClient.sendFollowUpMessage(
                botSlug = BOT_SLUG,
                chatId = CHAT_ID,
                message = MESSAGE
            )

            assertThat(urlCaptor.firstValue)
                .isEqualTo("https://public-api.wordpress.com/wpcom/v2/odie/chat/$BOT_SLUG/$CHAT_ID/")
            assertThat(bodyCaptor.firstValue).containsOnlyKeys("message")
            assertThat(bodyCaptor.firstValue["message"]).isEqualTo(MESSAGE)
            assertThat(result).isInstanceOf(Response.Success::class.java)
            assertThat((result as Response.Success).data.chatId).isEqualTo(CHAT_ID)
        }

    @Test
    fun `given chat id, when fetchChat, then GETs the slug-and-id URL`() = testBlocking {
        stubGetResponse()

        val result = restClient.fetchChat(botSlug = BOT_SLUG, chatId = CHAT_ID)

        assertThat(urlCaptor.firstValue)
            .isEqualTo("https://public-api.wordpress.com/wpcom/v2/odie/chat/$BOT_SLUG/$CHAT_ID/")
        assertThat(result).isInstanceOf(Response.Success::class.java)
        assertThat((result as Response.Success).data.chatId).isEqualTo(CHAT_ID)
    }

    @Test
    fun `given successful response, when sendMessage, then Success is propagated`() = testBlocking {
        val data = supportChatResponse()
        stubPostResponse(data = data)

        val result = restClient.sendMessage(BOT_SLUG, MESSAGE, JsonObject())

        assertThat(result).isInstanceOf(Response.Success::class.java)
        assertThat((result as Response.Success).data.chatId).isEqualTo(CHAT_ID)
    }

    @Test
    fun `given network error, when sendMessage, then Error is propagated`() = testBlocking {
        val error = WPComGsonNetworkError(
            BaseNetworkError(BaseRequest.GenericErrorType.TIMEOUT, VolleyError())
        )
        stubPostResponse(error = error)

        val result = restClient.sendMessage(BOT_SLUG, MESSAGE, JsonObject())

        assertThat(result).isInstanceOf(Response.Error::class.java)
        assertThat((result as Response.Error).error.type).isEqualTo(BaseRequest.GenericErrorType.TIMEOUT)
    }

    @Test
    fun `given network error, when sendFollowUpMessage, then Error is propagated`() = testBlocking {
        val error = WPComGsonNetworkError(
            BaseNetworkError(BaseRequest.GenericErrorType.TIMEOUT, VolleyError())
        )
        stubPostResponse(error = error)

        val result = restClient.sendFollowUpMessage(BOT_SLUG, CHAT_ID, MESSAGE)

        assertThat(result).isInstanceOf(Response.Error::class.java)
        assertThat((result as Response.Error).error.type).isEqualTo(BaseRequest.GenericErrorType.TIMEOUT)
    }

    @Test
    fun `given network error, when fetchChat, then Error is propagated`() = testBlocking {
        val error = WPComGsonNetworkError(
            BaseNetworkError(BaseRequest.GenericErrorType.NOT_FOUND, VolleyError())
        )
        stubGetResponse(error = error)

        val result = restClient.fetchChat(BOT_SLUG, CHAT_ID)

        assertThat(result).isInstanceOf(Response.Error::class.java)
        assertThat((result as Response.Error).error.type).isEqualTo(BaseRequest.GenericErrorType.NOT_FOUND)
    }

    private suspend fun stubPostResponse(
        data: SupportChatResponse = supportChatResponse(),
        error: WPComGsonNetworkError? = null
    ) {
        val response: Response<SupportChatResponse> = if (error != null) {
            Response.Error(error)
        } else {
            Response.Success(data, emptyList())
        }
        whenever(
            wpComGsonRequestBuilder.syncPostRequest(
                restClient = eq(restClient),
                url = urlCaptor.capture(),
                params = anyOrNull(),
                body = bodyCaptor.capture(),
                clazz = eq(SupportChatResponse::class.java),
                retryPolicy = anyOrNull(),
                headers = any()
            )
        ).thenReturn(response)
    }

    private suspend fun stubGetResponse(
        data: SupportChatResponse = supportChatResponse(),
        error: WPComGsonNetworkError? = null
    ) {
        val response: Response<SupportChatResponse> = if (error != null) {
            Response.Error(error)
        } else {
            Response.Success(data, emptyList())
        }
        whenever(
            wpComGsonRequestBuilder.syncGetRequest(
                restClient = eq(restClient),
                url = urlCaptor.capture(),
                params = eq(emptyMap()),
                clazz = eq(SupportChatResponse::class.java),
                enableCaching = any(),
                cacheTimeToLive = any(),
                forced = any(),
                customGsonBuilder = anyOrNull(),
                authenticatedRequest = any()
            )
        ).thenReturn(response)
    }

    private fun supportChatResponse() = SupportChatResponse(
        chatId = CHAT_ID,
        sessionId = "session-abc-123",
        botSlug = BOT_SLUG,
        botVersion = "v1.0.0"
    )

    private companion object {
        const val BOT_SLUG = "woo-workflow-support_mobile_inapp"
        const val CHAT_ID = 4242L
        const val MESSAGE = "I can't load my orders"
    }
}
