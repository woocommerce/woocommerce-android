package com.woocommerce.android.ui.orders

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.text.TextUtils
import android.util.AttributeSet
import android.view.View
import com.google.i18n.addressinput.common.AddressData
import com.google.i18n.addressinput.common.FormOptions
import com.google.i18n.addressinput.common.FormatInterpreter
import com.woocommerce.android.R
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.PhoneUtils
import kotlinx.android.synthetic.main.order_detail_customer_info.view.*
import org.wordpress.android.fluxc.model.WCOrderModel

class OrderDetailCustomerInfoView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : ConstraintLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.order_detail_customer_info, this)
    }

    fun initView(order: WCOrderModel, listener: OrderActionListener) {
        customerInfo_custName.text = order.billingFirstName + " " + order.billingLastName

        // display billing address info
        val formatInterpreter = FormatInterpreter(FormOptions().createSnapshot())
        val billingAddressData: AddressData = AddressData.builder()
                .setAddressLines(mutableListOf(order.billingAddress1, order.billingAddress2))
                .setLocality(order.billingCity)
                .setAdminArea(order.billingState)
                .setPostalCode(order.billingPostcode)
                .setCountry(order.billingCountry)
                .setOrganization(order.billingCompany)
                .build()
        val billingAddressFrags = formatInterpreter.getEnvelopeAddress(billingAddressData)
        customerInfo_billingAddr.text = TextUtils.join(System.getProperty("line.separator"), billingAddressFrags)
        customerInfo_billingCountry.text = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)

        // display shipping address info
        if (order.hasSeparateShippingDetails()) {
            val shippingAddressData: AddressData = AddressData.builder()
                    .setAddressLines(mutableListOf(order.shippingAddress1, order.shippingAddress2))
                    .setLocality(order.shippingCity)
                    .setAdminArea(order.shippingState)
                    .setPostalCode(order.shippingPostcode)
                    .setCountry(order.shippingCountry)
                    .setOrganization(order.shippingCompany)
                    .build()
            val addressFrags = formatInterpreter.getEnvelopeAddress(shippingAddressData)
            customerInfo_shippingAddr.text = TextUtils.join(System.getProperty("line.separator"), addressFrags)
            customerInfo_shippingCountry.text = AddressUtils.getCountryLabelByCountryCode(order.shippingCountry)
        } else {
            customerInfo_shippingAddr.text = TextUtils.join(System.getProperty("line.separator"), billingAddressFrags)
            customerInfo_shippingCountry.text = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)
        }

        // display email address info
        customerInfo_emailAddr.text = order.billingEmail

        // display phone
        if (!order.billingPhone.isNullOrEmpty()) {
            customerInfo_phone.text = PhoneUtils.formatPhone(context, order.billingPhone)
        }

        // configure more/less button
        customerInfo_viewMore.setOnCheckedChangeListener { _, isChecked ->
            customerInfo_morePanel.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        // Set action button listeners
        customerInfo_emailBtn.setOnClickListener {
            listener.createEmail(order.billingEmail)
        }

        customerInfo_phoneBtn.setOnClickListener {
            listener.dialPhone(order.billingPhone)
        }

        customerInfo_hangoutsBtn.setOnClickListener {
            listener.sendSms(order.billingPhone)
        }
    }
}
