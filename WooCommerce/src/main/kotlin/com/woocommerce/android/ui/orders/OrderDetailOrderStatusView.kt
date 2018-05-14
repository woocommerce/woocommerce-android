package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.RelativeLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.widgets.tags.TagView
import kotlinx.android.synthetic.main.order_detail_order_status.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailOrderStatusView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : RelativeLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_order_status, this)
    }

    fun initView(orderModel: WCOrderModel) {
        orderStatus_orderNum.text = context.getString(
                R.string.orderdetail_orderstatus_heading,
                orderModel.number, orderModel.billingFirstName, orderModel.billingLastName)
        val dateStr = DateUtils.getFriendlyShortDateAtTimeString(context, orderModel.dateCreated)
        orderStatus_created.text = context.getString(R.string.orderdetail_orderstatus_created, dateStr)
        orderStatus_orderTags.removeAllViews()
        orderStatus_orderTags.addView(getTagView(orderModel.status))
    }

    private fun getTagView(text: String): TagView {
        val orderTag = OrderStatusTag(text)
        val tagView = TagView(context)
        tagView.tag = orderTag
        return tagView
    }
}
