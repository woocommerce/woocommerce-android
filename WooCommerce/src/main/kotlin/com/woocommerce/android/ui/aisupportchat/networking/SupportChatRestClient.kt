package com.woocommerce.android.ui.aisupportchat.networking

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.JsonObject
import com.woocommerce.android.ui.aisupportchat.networking.model.SupportChatResponse
import org.wordpress.android.fluxc.Dispatcher
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
    accessToken: AccessToken,
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
        clazz = SupportChatResponse::class.java
    )

    suspend fun sendFollowUpMessage(
        botSlug: String,
        chatId: Long,
        message: String
    ): Response<SupportChatResponse> = wpComGsonRequestBuilder.syncPostRequest(
        restClient = this,
        url = chatUrl(botSlug, chatId),
        params = null,
        body = mapOf(MESSAGE_KEY to message),
        clazz = SupportChatResponse::class.java
    )

    suspend fun fetchChat(
        botSlug: String,
        chatId: Long
    ): Response<SupportChatResponse> = wpComGsonRequestBuilder.syncGetRequest(
        restClient = this,
        url = chatUrl(botSlug, chatId),
        params = emptyMap(),
        clazz = SupportChatResponse::class.java
    )

    private fun chatUrl(botSlug: String): String = "$ODIE_CHAT_BASE_URL/$botSlug"

    private fun chatUrl(botSlug: String, chatId: Long): String = "${chatUrl(botSlug)}/$chatId"

    companion object {
        private const val ODIE_CHAT_BASE_URL = "https://public-api.wordpress.com/wpcom/v2/odie/chat"
        private const val MESSAGE_KEY = "message"
        private const val CONTEXT_KEY = "context"
    }
}
