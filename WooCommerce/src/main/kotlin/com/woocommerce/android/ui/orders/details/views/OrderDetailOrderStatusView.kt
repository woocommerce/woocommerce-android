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
import java.util.Date

typealias EditStatusClickListener = (View) -> Unit

class OrderDetailOrderStatusView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailOrderStatusBinding.inflate(LayoutInflater.from(ctx), this)

    private var mode: Mode = Mode.OrderEdit

    fun updateStatus(orderStatus: OrderStatus) {
        binding.orderStatusOrderTags.contentDescription =
            context.getString(R.string.orderstatus_contentDesc_withStatus, orderStatus.label)
        binding.orderStatusOrderTags.tag = OrderStatusTag(orderStatus)
    }

    fun updateOrder(order: Order) {
        binding.orderStatusSubtitle.text = getFormattedDate(order.dateCreated)

        when (mode) {
            Mode.OrderEdit -> {
                binding.orderStatusHeader.text =
                    order.getBillingName(context.getString(R.string.orderdetail_customer_name_default))
            }
            Mode.OrderCreation -> {
                binding.orderStatusHeader.isVisible = false
            }
        }
    }

    private fun getFormattedDate(date: Date): String {
        return when (mode) {
            Mode.OrderCreation -> {
                date.getMediumDate(context)
            }
            Mode.OrderEdit -> {
                "${date.getMediumDate(context)}, ${date.getTimeString(context)}"
            }
        }
    }

    fun initView(mode: Mode, editOrderStatusClickListener: EditStatusClickListener) {
        this.mode = mode
        when (mode) {
            Mode.OrderEdit -> {
                binding.orderStatusEditButton.isVisible = false
                binding.orderStatusEditImage.isVisible = true
                with(binding.orderStatusContainer) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener(editOrderStatusClickListener)
                }
            }
            Mode.OrderCreation -> {
                binding.orderStatusEditImage.isVisible = false
                with(binding.orderStatusEditButton) {
                    isVisible = true
                    setOnClickListener(editOrderStatusClickListener)
                }
            }
        }
    }

    enum class Mode {
        OrderCreation, OrderEdit
    }
}
