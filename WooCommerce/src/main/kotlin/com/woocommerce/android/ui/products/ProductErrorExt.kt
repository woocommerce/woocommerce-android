package com.woocommerce.android.ui.products

import org.wordpress.android.fluxc.store.WCProductStore
/**
 * Returns whether the error message is meaningful and can be displayed to the user.
 */
val WCProductStore.ProductError.canDisplayMessage: Boolean
    get() = this.type == WCProductStore.ProductErrorType.INVALID_MIN_MAX_QUANTITY &&
        this.message.isNotEmpty()
