package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailRefundsInfoBinding
import com.woocommerce.android.databinding.RefundShippingListItemBinding
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.getRefundedShippingLines
import com.woocommerce.android.util.StringUtils
import java.math.BigDecimal

class OrderDetailRefundsView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailRefundsInfoBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateRefunds(
        refunds: List<Refund>,
        formatCurrency: (BigDecimal) -> String,
        onTap: () -> Unit
    ) {
        val refundsCount = refunds.sumOf { refund -> refund.items.sumOf { it.quantity } }
        val shippingLines = refunds.getRefundedShippingLines()
        isVisible = refundsCount > 0 || shippingLines.isNotEmpty()
        binding.refundsInfoLblTitle.setText(
            when {
                shippingLines.isEmpty() -> R.string.order_refunds_refund_info_title
                else -> R.string.order_refunds_refunded_items
            }
        )
        with(binding.refundsInfoCount) {
            isVisible = refundsCount > 0
            text = StringUtils.getQuantityString(
                context = context,
                quantity = refundsCount,
                one = R.string.order_refunds_refund_info_description_one,
                default = R.string.order_refunds_refund_info_description_many,
            )
            setOnClickListener { onTap() }
        }
        binding.refundsInfoShippingLines.isVisible = shippingLines.isNotEmpty()
        binding.refundsInfoShippingLines.removeAllViews()
        shippingLines.forEachIndexed { index, shippingLine ->
            val shippingBinding = RefundShippingListItemBinding.inflate(
                LayoutInflater.from(context),
                binding.refundsInfoShippingLines,
                false
            )
            shippingBinding.issueRefundShippingName.text = shippingLine.methodTitle
            shippingBinding.issueRefundShippingPrice.text = formatCurrency(-shippingLine.total)
            shippingBinding.issueRefundShippingDivider.isVisible = index < shippingLines.lastIndex
            binding.refundsInfoShippingLines.addView(shippingBinding.root)
        }
    }
}
