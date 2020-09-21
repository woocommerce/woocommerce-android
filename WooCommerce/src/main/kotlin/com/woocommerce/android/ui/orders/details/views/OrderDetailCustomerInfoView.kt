package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import androidx.core.view.isVisible
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.PhoneUtils
import kotlinx.android.synthetic.main.order_detail_customer_info.view.*

class OrderDetailCustomerInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun updateCustomerInfo(
        order: Order,
        isVirtualOrder: Boolean, // don't display shipping section for virtual products
        displayBilling: Boolean = true // don't display billing section
    ) {
        // shipping address info
        val shippingAddress = order.formatShippingInformationForDisplay()
        when {
            isVirtualOrder -> {
                customerInfo_shippingSection.hide()
                customerInfo_viewMore.hide()
                customerInfo_morePanel.show()
                customerInfo_morePanel.expand()
                customerInfo_viewMore.setOnClickListener(null)
            }
            shippingAddress.isEmpty() -> {
                customerInfo_shippingAddr.text = context.getString(R.string.orderdetail_empty_shipping_address)
                customerInfo_shippingMethodSection.hide()
            }
            else -> {
                customerInfo_shippingAddr.text = shippingAddress
                customerInfo_shippingMethodSection.isVisible = order.shippingMethodList.firstOrNull()?.let {
                    customerInfo_shippingMethod.text = it
                    true
                } ?: false
            }
        }

        // customer note
        if (order.customerNote.isNotEmpty()) {
            customerInfo_customerNoteSection.show()
            customerInfo_customerNote.text = context.getString(R.string.orderdetail_customer_note, order.customerNote)
        } else {
            customerInfo_customerNoteSection.hide()
        }

        // billing info
        val billingInfo = order.formatBillingInformationForDisplay()
        if (displayBilling) {
            if (billingInfo.isNotEmpty()) {
                customerInfo_billingAddr.visibility = View.VISIBLE
                customerInfo_billingAddr.text = billingInfo
                customerInfo_divider2.visibility = View.VISIBLE
            } else {
                customerInfo_billingAddr.visibility = View.GONE
                customerInfo_divider2.visibility = View.GONE
            }

            // display phone only if available, otherwise, hide the view
            if (order.billingAddress.phone.isNotEmpty()) {
                customerInfo_phone.text = PhoneUtils.formatPhone(order.billingAddress.phone)
                customerInfo_phone.visibility = View.VISIBLE
                customerInfo_divider3.visibility = View.VISIBLE
                customerInfo_callOrMessageBtn.visibility = View.VISIBLE
            } else {
                customerInfo_phone.visibility = View.GONE
                customerInfo_divider3.visibility = View.GONE
                customerInfo_callOrMessageBtn.visibility = View.GONE
            }

            // display email address info only if available, otherwise, hide the view
            if (order.billingAddress.email.isNotEmpty()) {
                customerInfo_emailAddr.text = order.billingAddress.email
                customerInfo_emailAddr.visibility = View.VISIBLE
                customerInfo_emailBtn.visibility - View.VISIBLE
                customerInfo_emailBtn.setOnClickListener {
                    // TODO: add click event to open email here
                }
            } else {
                customerInfo_emailAddr.visibility = View.GONE
                customerInfo_emailBtn.visibility = View.GONE
                customerInfo_divider3.visibility = View.GONE
            }

            customerInfo_viewMore.setOnClickListener {
                val isChecked = customerInfo_viewMoreButtonImage.rotation == 0F
                if (isChecked) {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED)
                    customerInfo_morePanel.expand()
                    customerInfo_viewMoreButtonImage.animate().rotation(180F).setDuration(200).start()
                    customerInfo_viewMoreButtonTitle.text = context.getString(R.string.orderdetail_hide_billing)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED)
                    customerInfo_morePanel.collapse()
                    customerInfo_viewMoreButtonImage.animate().rotation(0F).setDuration(200).start()
                    customerInfo_viewMoreButtonTitle.text = context.getString(R.string.orderdetail_show_billing)
                }
            }
        } else {
            customerInfo_viewMore.hide()
            customerInfo_morePanel.hide()
            customerInfo_viewMore.setOnClickListener(null)
        }
    }
}
