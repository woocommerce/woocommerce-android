package com.woocommerce.android.ui.aisupportchat.networking

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.endpoint.WPCOMV2
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject
import javax.inject.Named

class SupportChatRestClient @Inject constructor(
    private val wpComGsonRequestBuilder: WPComGsonRequestBuilder,
    appContext: Context?,
    dispatcher: Dispatcher,
    @Named("regular") requestQueue: RequestQueue,
    private val accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {

    suspend fun sendMessage(
        botSlug: String,
        message: String,
        context: JsonObject
    ): Response<SupportChatResponse> = wpComGsonRequestBuilder.syncPostRequest(
        restClient = this,
        url = chatUrl(botSlug),
        params = null,
        body = mapOf(MESSAGE_KEY to message, CONTEXT_KEY to context),
        clazz = SupportChatResponse::class.java,
        authenticatedRequest = isWpComAuthenticated()
    )

    suspend fun sendFollowUpMessage(
        botSlug: String,
        chatId: Long,
        sessionId: String?,
        message: String
    ): Response<SupportChatResponse> = wpComGsonRequestBuilder.syncPostRequest(
        restClient = this,
        url = chatUrl(botSlug, chatId),
        params = null,
        body = mapOf(MESSAGE_KEY to message, SESSION_ID_KEY to sessionId.orEmpty()),
        clazz = SupportChatResponse::class.java,
        authenticatedRequest = isWpComAuthenticated()
    )

    suspend fun fetchChat(
        botSlug: String,
        chatId: Long
    ): Response<SupportChatResponse> = wpComGsonRequestBuilder.syncGetRequest(
        restClient = this,
        url = chatUrl(botSlug, chatId),
        params = emptyMap(),
        clazz = SupportChatResponse::class.java,
        authenticatedRequest = isWpComAuthenticated()
    )

    private fun chatUrl(botSlug: String): String = WPCOMV2.odie.chat.bot_slug(botSlug).url

    private fun chatUrl(botSlug: String, chatId: Long): String = WPCOMV2.odie.chat.bot_slug(botSlug).chat(chatId).url

    private fun isWpComAuthenticated(): Boolean = accessToken.exists()

    companion object {
        private const val MESSAGE_KEY = "message"
        private const val CONTEXT_KEY = "context"
        private const val SESSION_ID_KEY = "session_id"
    }
}
