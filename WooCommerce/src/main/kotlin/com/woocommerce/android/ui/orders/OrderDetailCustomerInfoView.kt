package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.util.AttributeSet
import android.view.View
import android.widget.PopupMenu
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.PhoneUtils
import com.woocommerce.android.widgets.AppRatingDialog
import kotlinx.android.synthetic.main.order_detail_customer_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerInfoView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun initView(order: WCOrderModel, shippingOnly: Boolean, listener: OrderCustomerActionListener? = null) {
        // Populate Shipping & Billing information
        val billingName = context
                .getString(R.string.customer_full_name, order.billingFirstName, order.billingLastName)
        val billingAddr = AddressUtils.getEnvelopeAddress(order.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)
        val billingAddrFull = getFullAddress(billingName, billingAddr, billingCountry)

        if (order.hasSeparateShippingDetails()) {
            val shippingName = context
                    .getString(R.string.customer_full_name, order.shippingFirstName, order.shippingLastName)

            val shippingAddr = AddressUtils.getEnvelopeAddress(order.getShippingAddress())
            val shippingCountry = AddressUtils.getCountryLabelByCountryCode(order.shippingCountry)
            val shippingAddrFull = getFullAddress(shippingName, shippingAddr, shippingCountry)
            customerInfo_shippingAddr.text = shippingAddrFull
        } else {
            customerInfo_shippingAddr.text = billingAddrFull
        }

        if (shippingOnly) {
            // Only display the shipping information in this card and hide everything else.
            formatViewAsShippingOnly()
        } else {
            // Populate Billing Information
            customerInfo_billingAddr.text = billingAddrFull

            // display email address info
            customerInfo_emailAddr.text = order.billingEmail

            // display phone
            if (!order.billingPhone.isEmpty()) {
                customerInfo_phone.text = PhoneUtils.formatPhone(order.billingPhone)
                customerInfo_phone.visibility = View.VISIBLE
                customerInfo_callOrMessageBtn.visibility = View.VISIBLE
                customerInfo_callOrMessageBtn.setOnClickListener {
                    showCallOrMessagePopup(order, listener)
                }
            } else {
                customerInfo_phone.visibility = View.GONE
                customerInfo_callOrMessageBtn.visibility = View.GONE
            }

            // configure more/less button
            customerInfo_viewMore.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED)
                    customerInfo_morePanel.visibility = View.VISIBLE
                } else {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED)
                    customerInfo_morePanel.visibility = View.GONE
                }
            }

            // Set action button listeners
            customerInfo_emailBtn.setOnClickListener {
                AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED)

                listener?.createEmail(order, order.billingEmail)
                AppRatingDialog.incrementInteractions()
            }
        }
    }

    private fun getFullAddress(name: String, address: String, country: String): String {
        var fullAddr = if (!name.isBlank()) "$name\n" else ""
        if (!address.isBlank()) fullAddr += "$address\n"
        if (!country.isBlank()) fullAddr += country
        return fullAddr
    }

    private fun showCallOrMessagePopup(order: WCOrderModel, listener: OrderCustomerActionListener?) {
        val popup = PopupMenu(context, customerInfo_callOrMessageBtn)
        popup.menuInflater.inflate(R.menu.menu_order_detail_phone_actions, popup.menu)

        popup.menu.findItem(R.id.menu_call)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED)
            listener?.dialPhone(order, order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.menu.findItem(R.id.menu_message)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED)
            listener?.sendSms(order, order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.show()
    }

    private fun formatViewAsShippingOnly() {
        customerInfo_viewMore.visibility = View.GONE
    }
}
