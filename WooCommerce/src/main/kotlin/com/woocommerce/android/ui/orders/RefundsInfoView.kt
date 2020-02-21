package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.model.Refund
import kotlinx.android.synthetic.main.order_detail_refunds_info.view.*

class RefundsInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_refunds_info, this)
    }

    /**
     * Initialize and format this view.
     *
     * @param [refunds] List of refunds order the order.
     * @param [onTap] Listener for refund details tap.
     */
    fun initView(
        refunds: List<Refund>,
        onTap: () -> Unit
    ) {
        val refundsCount = refunds.sumBy { refund -> refund.items.sumBy { it.quantity } }
        refundsInfo_count.text = context.resources.getQuantityString(
                R.plurals.order_refunds_refund_info_description,
                refundsCount,
                refundsCount
        )
        refundsInfo_count.setOnClickListener { onTap() }
    }
}
