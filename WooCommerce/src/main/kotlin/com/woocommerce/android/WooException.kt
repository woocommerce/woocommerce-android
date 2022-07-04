package com.woocommerce.android

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError

class WooException(val error: WooError) : Exception(error.message.orEmpty())
