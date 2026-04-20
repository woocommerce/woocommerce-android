package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailOrderStatusBinding
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.extensions.getTimeString
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderStatusTag
import com.woocommerce.android.ui.orders.SalesChannelTag
import java.util.Date

typealias EditStatusClickListener = (View) -> Unit

class OrderDetailOrderStatusView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailOrderStatusBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateStatus(orderStatus: OrderStatus) {
        binding.orderStatusOrderTags.contentDescription =
            context.getString(R.string.orderstatus_contentDesc_withStatus, orderStatus.label)
        binding.orderStatusOrderTags.tag = OrderStatusTag(orderStatus)
    }

    fun updateOrder(order: Order) {
        binding.orderStatusSubtitle.text = getFormattedDate(order.dateCreated)
        binding.orderStatusHeader.text =
            order.getBillingName(context.getString(R.string.orderdetail_customer_name_default))

        updatePosTag(order)
    }

    private fun updatePosTag(order: Order) {
        if (order.salesChannel == Order.SalesChannel.POS) {
            binding.orderStatusPosTag.isVisible = true
            binding.orderStatusPosTag.tag = SalesChannelTag(context.getString(R.string.pos_badge))
        } else {
            binding.orderStatusPosTag.isVisible = false
        }
    }

    private fun getFormattedDate(date: Date): String {
        return "${date.getMediumDate(context)}, ${date.getTimeString(context)}"
    }

    fun initView(mode: Mode, editOrderStatusClickListener: EditStatusClickListener? = null) {
        when (mode) {
            Mode.OrderEdit -> {
                val listener = requireNotNull(editOrderStatusClickListener) {
                    "editOrderStatusClickListener must be provided when mode is OrderEdit"
                }
                binding.orderStatusEditImage.isVisible = true
                with(binding.orderStatusContainer) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener(listener)
                }
            }
            Mode.ReadOnly -> {
                binding.orderStatusEditImage.isVisible = false
                with(binding.orderStatusContainer) {
                    isClickable = false
                    isFocusable = false
                    setOnClickListener(null)
                }
            }
        }
    }

    enum class Mode {
        OrderEdit, ReadOnly
    }
}
