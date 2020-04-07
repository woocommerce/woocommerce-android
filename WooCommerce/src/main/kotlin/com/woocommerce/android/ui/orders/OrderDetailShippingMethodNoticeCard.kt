package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R

/**
 * Order warning card that is displayed if an order contains more than one shipping
 * method. This can happen by installing shipping related plugins that provide this option
 */
class OrderDetailShippingMethodNoticeCard @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_shipping_warning, this)
    }
}
