package com.woocommerce.android.ui.orders.wooshippinglabels.servicestatus

import com.woocommerce.android.WooException
import javax.inject.Inject

class IsServiceOutageError @Inject constructor() {
    operator fun invoke(throwable: Throwable): Boolean {
        val error = (throwable as? WooException)?.error ?: return false
        val upstreamStatus = error.errorData?.optInt(UPSTREAM_STATUS_KEY)?.takeIf { it > 0 }
        return when {
            upstreamStatus != null -> upstreamStatus in SERVER_ERROR_STATUS_RANGE
            else -> error.apiErrorCode in CONNECT_SERVER_ERROR_CODES
        }
    }

    private companion object {
        const val UPSTREAM_STATUS_KEY = "response_status_code"
        val SERVER_ERROR_STATUS_RANGE = 500..599
        val CONNECT_SERVER_ERROR_CODES = setOf(
            "wcc_server_error",
            "wcc_server_error_response",
            "wcc_server_empty_response"
        )
    }
}
