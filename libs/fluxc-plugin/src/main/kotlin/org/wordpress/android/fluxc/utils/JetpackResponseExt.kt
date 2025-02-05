package org.wordpress.android.fluxc.utils

import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackError
import org.wordpress.android.fluxc.network.rest.wpcom.jetpacktunnel.JetpackTunnelGsonRequestBuilder.JetpackResponse.JetpackSuccess
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.toWooError

fun <T> JetpackResponse<T>.handleResult() =
        when (this) {
            is JetpackSuccess -> {
                WooPayload(data)
            }
            is JetpackError -> {
                WooPayload(error.toWooError())
            }
        }
