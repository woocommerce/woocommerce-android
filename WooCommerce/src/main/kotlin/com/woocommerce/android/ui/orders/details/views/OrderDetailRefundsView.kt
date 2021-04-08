package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailRefundsInfoBinding
import java.math.BigDecimal

class OrderDetailRefundsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailRefundsInfoBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateRefundCount(
        refundsCount: BigDecimal,
        onTap: () -> Unit
    ) {
        with(binding.refundsInfoCount) {
            //  if the refunds count is integer 1, use "one" case from plural,
            //  otherwise use "other" case for plural.
            if (refundsCount.equals(BigDecimal.ONE)) {
                text = context.resources.getQuantityString(
                    R.plurals.order_refunds_refund_info_description,
                    refundsCount.toInt(),
                    refundsCount
                )
            } else {
                text = context.resources.getQuantityString(
                    R.plurals.order_refunds_refund_info_description,
                    0,
                    refundsCount
                )
                setOnClickListener { onTap() }
            }
        }
    }
}
