package com.woocommerce.android.ui.orders

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.getBillingName
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.widgets.tags.TagView
import kotlinx.android.synthetic.main.order_detail_order_status.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel

class OrderDetailOrderStatusView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_order_status, this)
    }

    interface OrderStatusListener {
        fun openOrderStatusSelector()
    }

    fun initView(orderModel: WCOrderModel, orderStatus: WCOrderStatusModel, listener: OrderStatusListener) {
        val dateStr = if (DateUtils().isToday(orderModel.dateCreated)) {
            DateUtils().getTimeString(context, orderModel.dateCreated)
        } else {
            DateUtils().getMediumDateFromString(context, orderModel.dateCreated)
        }
        orderStatus_dateAndOrderNum.text = context.getString(
            R.string.orderdetail_orderstatus_date_and_ordernum,
            dateStr,
            orderModel.number
        )

        orderStatus_name.text = orderModel.getBillingName(context.getString(R.string.orderdetail_customer_name_default))

        orderStatus_orderTags.removeAllViews()
        orderStatus_orderTags.addView(getTagView(orderStatus))

        orderStatus_edit.setOnClickListener {
            AnalyticsTracker.track(
                    Stat.ORDER_DETAIL_ORDER_STATUS_EDIT_BUTTON_TAPPED, mapOf("status" to orderModel.status))
            listener.openOrderStatusSelector()
        }
    }

    fun updateStatus(orderStatus: WCOrderStatusModel) {
        orderStatus_orderTags.removeAllViews()
        orderStatus_orderTags.addView(getTagView(orderStatus))
    }

    private fun getTagView(orderStatus: WCOrderStatusModel): TagView {
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
}
