package com.woocommerce.android.ui.main

import org.wordpress.android.fluxc.action.WCOrderAction

/**
 * Order-related error message handling
 */
interface OrdersErrorResolution {
    fun handleOrderError(actionType: WCOrderAction, errorMsg: String?)
}

/**
 * This interface is extended by the [MainActivity]. Interface should extend any new error
 * resolution types the main activity is expected to handle.
 */
interface MainErrorResolution : OrdersErrorResolution
