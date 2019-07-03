package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.PopupMenu
import androidx.constraintlayout.widget.ConstraintLayout
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

    fun initView(
        order: WCOrderModel,
        shippingOnly: Boolean,
        billingOnly: Boolean = false
    ) {
        // Populate Shipping & Billing information
        val billingAddrFull = getBillingInformation(order)
        val isShippingInfoEmpty = !order.hasSeparateShippingDetails()
        val isBillingInfoEmpty = billingAddrFull.trim().isEmpty() &&
                order.billingEmail.isEmpty() && order.billingPhone.isEmpty()

        // display empty message if no shipping and billing details are available
        if (isShippingInfoEmpty && isBillingInfoEmpty) {
            formatViewAsShippingOnly()
            customerInfo_shippingAddr.text = context.getString(R.string.orderdetail_empty_shipping_address)
            return
        }

        // show shipping section only for non virtual products or if shipping info available
        val showShipping = !billingOnly && !isShippingInfoEmpty
        showShippingSection(order, showShipping)

        // if only shipping is to be displayed or if billing details are not available, hide the billing section
        if (shippingOnly || isBillingInfoEmpty) {
            formatViewAsShippingOnly()
        } else {
            // if billing address is available, populte billing info, if not available, hide the address view
            if (billingAddrFull.trim().isEmpty()) {
                customerInfo_billingLabel.visibility = View.GONE
                customerInfo_billingAddr.visibility = View.GONE
                customerInfo_divider.visibility = View.GONE
            } else {
                customerInfo_billingLabel.visibility = View.VISIBLE
                customerInfo_billingAddr.visibility = View.VISIBLE
                customerInfo_billingAddr.text = billingAddrFull
            }

            // display phone only if available, otherwise, hide the view
            if (!order.billingPhone.isEmpty()) {
                customerInfo_phone.text = PhoneUtils.formatPhone(order.billingPhone)
                customerInfo_phone.visibility = View.VISIBLE
                customerInfo_divider2.visibility = View.VISIBLE
                customerInfo_callOrMessageBtn.visibility = View.VISIBLE
                customerInfo_callOrMessageBtn.setOnClickListener {
                    showCallOrMessagePopup(order)
                }
            } else {
                customerInfo_phone.visibility = View.GONE
                customerInfo_divider2.visibility = View.GONE
                customerInfo_callOrMessageBtn.visibility = View.GONE
            }

            // display email address info only if available, otherwise, hide the view
            if (!order.billingEmail.isEmpty()) {
                customerInfo_emailAddr.text = order.billingEmail
                customerInfo_emailAddr.visibility = View.VISIBLE
                customerInfo_emailBtn.visibility - View.VISIBLE
                customerInfo_divider3.visibility = View.VISIBLE
                customerInfo_emailBtn.setOnClickListener {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED)
                    OrderCustomerHelper.createEmail(context, order, order.billingEmail)
                    AppRatingDialog.incrementInteractions()
                }
            } else {
                customerInfo_emailAddr.visibility = View.GONE
                customerInfo_emailBtn.visibility = View.GONE
                customerInfo_divider3.visibility = View.GONE
            }
        }
    }

    fun showShippingSection(order: WCOrderModel, show: Boolean) {
        if (show) {
            val shippingName = context
                    .getString(R.string.customer_full_name, order.shippingFirstName, order.shippingLastName)

            val shippingAddr = AddressUtils.getEnvelopeAddress(order.getShippingAddress())
            val shippingCountry = AddressUtils.getCountryLabelByCountryCode(order.shippingCountry)
            val shippingAddrFull = getFullAddress(shippingName, shippingAddr, shippingCountry)
            customerInfo_shippingAddr.text = shippingAddrFull
            customerInfo_viewMore.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED)
                    customerInfo_morePanel.visibility = View.VISIBLE
                } else {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED)
                    customerInfo_morePanel.visibility = View.GONE
                }
            }
        } else {
            customerInfo_divider.visibility = View.GONE
            customerInfo_shippingAddr.visibility = View.GONE
            customerInfo_shippingLabel.visibility = View.GONE
            customerInfo_morePanel.visibility = View.VISIBLE
            formatViewAsShippingOnly()
            customerInfo_viewMore.setOnCheckedChangeListener(null)
        }
    }

    private fun getBillingInformation(order: WCOrderModel): String {
        val billingName = context
                .getString(R.string.customer_full_name, order.billingFirstName, order.billingLastName)
        val billingAddr = AddressUtils.getEnvelopeAddress(order.getBillingAddress())
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)
        return getFullAddress(billingName, billingAddr, billingCountry)
    }

    private fun getFullAddress(name: String, address: String, country: String): String {
        var fullAddr = if (!name.isBlank()) "$name\n" else ""
        if (!address.isBlank()) fullAddr += "$address\n"
        if (!country.isBlank()) fullAddr += country
        return fullAddr
    }

    private fun showCallOrMessagePopup(order: WCOrderModel) {
        val popup = PopupMenu(context, customerInfo_callOrMessageBtn)
        popup.menuInflater.inflate(R.menu.menu_order_detail_phone_actions, popup.menu)

        popup.menu.findItem(R.id.menu_call)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED)
            OrderCustomerHelper.dialPhone(context, order, order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.menu.findItem(R.id.menu_message)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED)
            OrderCustomerHelper.sendSms(context, order, order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.show()
    }

    private fun formatViewAsShippingOnly() {
        customerInfo_viewMore.visibility = View.GONE
    }
}
