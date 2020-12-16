package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailRefundsInfoBinding

class OrderDetailRefundsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailRefundsInfoBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateRefundCount(
        refundsCount: Int,
        onTap: () -> Unit
    ) {
        with(binding.refundsInfoCount) {
            text = context.resources.getQuantityString(
                R.plurals.order_refunds_refund_info_description,
                refundsCount,
                refundsCount
            )
            setOnClickListener { onTap() }
        }
    }
}
