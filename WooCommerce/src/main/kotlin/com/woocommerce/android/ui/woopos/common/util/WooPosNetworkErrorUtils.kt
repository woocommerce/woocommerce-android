package com.woocommerce.android.ui.woopos.common.util

import com.woocommerce.android.WooException
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType

fun Throwable.isNetworkError(): Boolean {
    return this is WooException &&
        (error.type == WooErrorType.TIMEOUT || error.type == WooErrorType.NO_CONNECTION)
}
