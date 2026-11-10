package com.woocommerce.android.aiassistant.appfunctions

import android.content.Context
import android.os.Build
import androidx.appfunctions.AppFunctionManager
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_FIND_ORDERS
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_FIND_PRODUCTS
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_GET_ORDER_DETAILS
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_GET_SALES_SUMMARY
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_OPEN_ORDER_IN_APP
import com.woocommerce.android.aiassistant.appfunctions.StoreAppFunctionService.Companion.FUNCTION_ID_REQUEST_STOCK_CHANGE
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class StoreAppFunctionsAvailability @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun setEnabled(enabled: Boolean) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return
        val functionIds = listOf(
            FUNCTION_ID_GET_SALES_SUMMARY,
            FUNCTION_ID_FIND_ORDERS,
            FUNCTION_ID_GET_ORDER_DETAILS,
            FUNCTION_ID_FIND_PRODUCTS,
            FUNCTION_ID_REQUEST_STOCK_CHANGE,
            FUNCTION_ID_OPEN_ORDER_IN_APP,
        )
        val manager = AppFunctionManager.getInstance(context) ?: return
        val state = if (enabled) {
            AppFunctionManager.APP_FUNCTION_STATE_ENABLED
        } else {
            AppFunctionManager.APP_FUNCTION_STATE_DISABLED
        }
        functionIds.forEach { functionId ->
            try {
                manager.setAppFunctionEnabled(functionId, state)
            } catch (e: IllegalArgumentException) {
                WooLog.w(WooLog.T.AI, "AppFunction $functionId is not indexed yet: ${e.message}")
            }
        }
    }
}
