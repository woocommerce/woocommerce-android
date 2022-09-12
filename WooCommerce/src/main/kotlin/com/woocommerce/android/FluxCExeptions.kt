package com.woocommerce.android

import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.store.Store.OnChangedError

class WooException(val error: WooError) : Exception(error.message.orEmpty())

class OnChangedException(val error: OnChangedError, override val message: String? = null) : Exception()
