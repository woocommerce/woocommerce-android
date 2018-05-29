package com.woocommerce.android.ui.main

import com.woocommerce.android.R
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import javax.inject.Inject

/**
 * Class responsible for processing error messaging for any fragments hosted by the MainActivity. This
 * ensures a single consistent message capable of surviving fragment lifecycles.
 */
class MainErrorHandler @Inject constructor(val uiResolver: MainUIMessageResolver) : MainContract.ErrorHandler {
    override fun handleOrderError(actionType: WCOrderAction, errorMsg: String?) {
        when (actionType) {
            FETCH_ORDERS -> uiResolver.showSnack(R.string.orderlist_error_fetch_generic, errorMsg)
            UPDATE_ORDER_STATUS -> uiResolver.showSnack(R.string.order_error_update_general, errorMsg)
            FETCH_ORDER_NOTES -> uiResolver.showSnack(R.string.order_error_fetch_notes_generic, errorMsg)
            else -> uiResolver.showSnack(R.string.error_generic, errorMsg)
        }
    }

    override fun handleGenericError(errorMsg: String) {
        uiResolver.showSnack(errorMsg)
    }
}
