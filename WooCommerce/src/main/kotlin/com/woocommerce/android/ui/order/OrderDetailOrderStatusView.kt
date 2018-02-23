package com.woocommerce.android.ui.order

import android.content.Context
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.woocommerce.android.R
import com.woocommerce.android.ui.model.TimeGroup
import com.woocommerce.android.ui.orderlist.OrderStatusTag
import com.woocommerce.android.widgets.tags.TagView
import kotlinx.android.synthetic.main.order_detail_order_status.view.*
import org.wordpress.android.fluxc.model.WCOrderModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

class OrderDetailOrderStatusView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_order_status, this)
    }

    fun initView(orderModel: WCOrderModel) {
        orderStatus_orderNum.text = context.getString(
                R.string.order_orderstatus_ordernum, orderModel.remoteOrderId)
        orderStatus_created.text = getFriendlyDateString(orderModel.dateCreated)
        orderStatus_orderTags.removeAllViews()
        orderModel.status.split(",").sorted().forEach { i -> orderStatus_orderTags.addView(getTagView(i)) }
    }

    private fun getTagView(text: String): TagView {
        val orderTag = OrderStatusTag(text)
        val tagView = TagView(context)
        tagView.tag = orderTag
        return tagView
    }

    private fun getFriendlyDateString(rawDate: String): String {
        val date = DateTimeUtils.dateFromIso8601(rawDate) ?: Date()
        val timeGroup = TimeGroup.getTimeGroupForDate(date)
        val dateLabel = when (timeGroup) {
            TimeGroup.GROUP_TODAY -> {
                context.getString(R.string.date_timeframe_today).toLowerCase()
            }
            TimeGroup.GROUP_YESTERDAY -> {
                context.getString(R.string.date_timeframe_yesterday).toLowerCase()
            }
            else -> {
                DateFormat.getDateFormat(context).format(date)
            }
        }
        val timeString = DateFormat.getTimeFormat(context).format(date.time)
        return context.getString(R.string.order_orderstatus_created, dateLabel, timeString)
    }
}
