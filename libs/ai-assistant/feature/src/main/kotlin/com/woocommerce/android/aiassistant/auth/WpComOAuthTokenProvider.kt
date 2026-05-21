package com.woocommerce.android.aiassistant.auth

import com.woocommerce.android.aiassistant.core.auth.AssistantAuthException
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import javax.inject.Inject

internal interface WpComOAuthTokenProvider {
    suspend fun provide(): String
}

internal class AccessTokenWpComOAuthTokenProvider @Inject constructor(
    private val accessToken: AccessToken,
) : WpComOAuthTokenProvider {
    override suspend fun provide(): String =
        accessToken.takeIf { it.exists() }?.get()?.takeIf { it.isNotBlank() }
            ?: throw AssistantAuthException("Missing WPCOM OAuth bearer")
}
