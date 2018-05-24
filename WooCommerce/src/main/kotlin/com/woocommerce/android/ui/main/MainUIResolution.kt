package com.woocommerce.android.ui.main

import com.woocommerce.android.R
import com.woocommerce.android.ui.base.GenericErrorResolution
import com.woocommerce.android.ui.base.UIMessageResolver
import org.wordpress.android.fluxc.action.WCOrderAction
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDERS
import org.wordpress.android.fluxc.action.WCOrderAction.FETCH_ORDER_NOTES
import org.wordpress.android.fluxc.action.WCOrderAction.UPDATE_ORDER_STATUS
import javax.inject.Inject

class MainUIResolution @Inject constructor(uiResolver: UIMessageResolver)
    : MainErrorResolution, GenericErrorResolution(uiResolver) {
    /**
     * Handle FluxC order action related errors
     */
    override fun handleOrderError(actionType: WCOrderAction, errorMsg: String?) {
        when(actionType) {
            FETCH_ORDERS -> uiResolver.showSnack(R.string.orderlist_error_fetch_generic, errorMsg)
            UPDATE_ORDER_STATUS -> uiResolver.showSnack(R.string.order_error_update_general, errorMsg)
            FETCH_ORDER_NOTES -> uiResolver.showSnack(R.string.order_error_fetch_notes_generic, errorMsg)
            else -> uiResolver.showSnack(R.string.error_generic, errorMsg)
        }
    }
}
