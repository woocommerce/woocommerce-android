package com.woocommerce.android.ui.orders

import org.wordpress.android.fluxc.action.WCOrderAction

/**
 * Order-related error resolution functions. Error handlers responsible for processing errors
 * should extend this to cover any order-related errors.
 */
interface OrdersErrorResolution {
    /**
     * Handle FluxC order action related errors.
     */
    fun handleOrderError(actionType: WCOrderAction, errorMsg: String?)
}
