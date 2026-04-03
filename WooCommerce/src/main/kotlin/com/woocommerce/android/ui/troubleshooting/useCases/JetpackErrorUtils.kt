package com.woocommerce.android.ui.troubleshooting.useCases

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.store.WCOrderStore

private const val ERROR_CODE_UNKNOWN_TOKEN = "unknown_token"
private const val ERROR_CODE_INVALID_BLOG = "invalid_blog"

private fun isJetpackNotConnectedError(errorCode: String?): Boolean {
    return errorCode == ERROR_CODE_UNKNOWN_TOKEN || errorCode == ERROR_CODE_INVALID_BLOG
}

fun WooError.isJetpackNotConnectedError(): Boolean = isJetpackNotConnectedError(apiErrorCode)

fun WCOrderStore.OrderError.isJetpackNotConnectedError(): Boolean = isJetpackNotConnectedError(networkError?.errorCode)
