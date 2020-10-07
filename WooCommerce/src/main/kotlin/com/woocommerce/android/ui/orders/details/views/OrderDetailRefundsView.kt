package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_detail_refunds_info.view.*

class OrderDetailRefundsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_refunds_info, this)
    }

    fun updateRefundCount(
        refundsCount: Int,
        onTap: () -> Unit
    ) {
        with(refundsInfo_count) {
            text = context.resources.getQuantityString(
                R.plurals.order_refunds_refund_info_description,
                refundsCount,
                refundsCount
            )
            setOnClickListener { onTap() }
        }
    }
}
