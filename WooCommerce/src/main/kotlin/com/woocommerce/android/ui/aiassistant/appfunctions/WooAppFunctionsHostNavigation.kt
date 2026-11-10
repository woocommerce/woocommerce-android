package com.woocommerce.android.ui.aiassistant.appfunctions

import android.content.Context
import android.content.Intent
import com.woocommerce.android.aiassistant.appfunctions.AppFunctionsHostNavigation
import com.woocommerce.android.ui.main.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class WooAppFunctionsHostNavigation @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppFunctionsHostNavigation {
    override fun orderDetailsIntent(orderId: Long): Intent =
        Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.FIELD_APP_FUNCTIONS_ORDER_ID, orderId)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
}
