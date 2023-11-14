package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.MultipleLinesContext
import com.woocommerce.android.viewmodel.ResourceProvider
import javax.inject.Inject

class DetermineMultipleLinesContext @Inject constructor(private val resourceProvider: ResourceProvider) {
    operator fun invoke(order: Order) =
        when {
            order.hasMultipleShippingLines -> MultipleLinesContext.Warning(
                header = resourceProvider.getString(
                    R.string.lines_incomplete,
                    resourceProvider.getString(R.string.orderdetail_shipping_details)
                ),
                explanation = resourceProvider.getString(
                    R.string.lines_incomplete_explanation,
                    resourceProvider.getString(R.string.orderdetail_shipping_details).lowercase(),
                ),
            )
            else -> MultipleLinesContext.None
        }
}
