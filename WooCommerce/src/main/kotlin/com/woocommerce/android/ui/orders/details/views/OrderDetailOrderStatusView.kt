package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderDetailOrderStatusBinding
import com.woocommerce.android.extensions.getMediumDate
import com.woocommerce.android.extensions.getTimeString
import com.woocommerce.android.extensions.isToday
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Order.OrderStatus
import com.woocommerce.android.ui.orders.OrderStatusTag
import com.woocommerce.android.widgets.tags.TagView

typealias EditStatusClickListener = (View) -> Unit

class OrderDetailOrderStatusView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailOrderStatusBinding.inflate(LayoutInflater.from(ctx), this)

    private var mode: Mode = Mode.OrderEdit

    fun updateStatus(orderStatus: OrderStatus) {
        binding.orderStatusOrderTags.removeAllViews()
        binding.orderStatusOrderTags.addView(getTagView(orderStatus))
    }

    fun updateOrder(order: Order) {
        with(order.dateCreated) {
            when (isToday()) {
                true -> getTimeString(context)
                false -> getMediumDate(context)
                null -> ""
            }.let { dateStr ->
                binding.orderStatusSubtitle.text =
                    when (mode) {
                        Mode.OrderEdit -> context.getString(
                            R.string.orderdetail_orderstatus_date_and_ordernum,
                            dateStr,
                            order.number
                        )
                        Mode.OrderCreation -> dateStr
                    }
            }
        }

        when (mode) {
            Mode.OrderEdit -> {
                binding.orderStatusHeader.text =
                    order.getBillingName(context.getString(R.string.orderdetail_customer_name_default))
            }
            Mode.OrderCreation -> {
                // TODO
                binding.orderStatusHeader.isVisible = false
            }
        }
    }

    private fun getTagView(orderStatus: OrderStatus): TagView {
        val orderTag = OrderStatusTag(orderStatus)
        val tagView = TagView(context)
        tagView.tag = orderTag
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            tagView.isFocusableInTouchMode = true
        } else {
            tagView.focusable = View.FOCUSABLE
        }
        return tagView
    }

    fun initView(mode: Mode, editOrderStatusClickListener: EditStatusClickListener) {
        this.mode = mode
        when (mode) {
            Mode.OrderEdit -> {
                binding.orderStatusEditButton.isVisible = false
                binding.orderStatusEditImage.isVisible = true
                with(binding.orderStatusEdit) {
                    isClickable = true
                    isFocusable = true
                    setOnClickListener(editOrderStatusClickListener)
                }
            }
            Mode.OrderCreation -> {
                binding.orderStatusEditImage.isVisible = false
                with(binding.orderStatusEditButton) {
                    isVisible = true
                    text = context.getString(R.string.edit)
                    setOnClickListener(editOrderStatusClickListener)
                }
            }
        }
    }

    enum class Mode {
        OrderCreation, OrderEdit
    }
}
