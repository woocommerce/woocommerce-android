package com.woocommerce.android.ui.orders

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.PopupMenu
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.OrderDetailCustomerInfoBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.util.AddressUtils
import com.woocommerce.android.util.PhoneUtils
import com.woocommerce.android.widgets.AppRatingDialog
import org.wordpress.android.fluxc.model.WCOrderModel

// TODO: soon to be removed
class OrderDetailCustomerInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailCustomerInfoBinding.inflate(LayoutInflater.from(ctx), this)

    @SuppressLint("SetTextI18n")
    fun initView(
        order: WCOrderModel,
        shippingOnly: Boolean,
        billingOnly: Boolean = false
    ) {
        // Populate Shipping & Billing information
        val billingAddressFull = getBillingInformation(order)
        val isShippingInfoEmpty = !isShippingAvailable(order)
        val isBillingInfoEmpty = billingAddressFull.trim().isEmpty() &&
            order.billingEmail.isEmpty() && order.billingPhone.isEmpty()

        if (order.customerNote.isEmpty()) {
            binding.customerInfoCustomerNoteSection.hide()
        } else {
            binding.customerInfoCustomerNoteSection.show()
            binding.customerInfoCustomerNote.text = "\"${order.customerNote}\""
        }

        // show shipping section only for non virtual products or if shipping info available
        initShippingSection(order, false)

        // if only shipping is to be displayed or if billing details are not available, hide the billing section
        if (shippingOnly || isBillingInfoEmpty) {
            formatViewAsShippingOnly()
        } else {
            // if billing address is available, populate billing info, if not available, hide the address view
            if (billingAddressFull.trim().isEmpty()) {
                binding.customerInfoBillingAddr.visibility = View.GONE
                binding.customerInfoDivider2.visibility = View.GONE
            } else {
                binding.customerInfoBillingAddr.visibility = View.VISIBLE
                binding.customerInfoBillingAddr.text = billingAddressFull
                binding.customerInfoDivider2.visibility = View.VISIBLE
            }

            // display phone only if available, otherwise, hide the view
            if (order.billingPhone.isNotEmpty()) {
                binding.customerInfoPhone.text = PhoneUtils.formatPhone(order.billingPhone)
                binding.customerInfoPhone.visibility = View.VISIBLE
                binding.customerInfoDivider3.visibility = View.VISIBLE
                binding.customerInfoCallOrMessageBtn.visibility = View.VISIBLE
                binding.customerInfoCallOrMessageBtn.setOnClickListener {
                    showCallOrMessagePopup(order)
                }
            } else {
                binding.customerInfoPhone.visibility = View.GONE
                binding.customerInfoDivider3.visibility = View.GONE
                binding.customerInfoCallOrMessageBtn.visibility = View.GONE
            }

            // display email address info only if available, otherwise, hide the view
            if (order.billingEmail.isNotEmpty()) {
                binding.customerInfoEmailAddr.text = order.billingEmail
                binding.customerInfoEmailAddr.visibility = View.VISIBLE
                binding.customerInfoEmailBtn.visibility = View.VISIBLE
                binding.customerInfoEmailBtn.setOnClickListener {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED)
                    OrderCustomerHelper.createEmail(context, order.toAppModel(), order.billingEmail)
                    AppRatingDialog.incrementInteractions()
                }
            } else {
                binding.customerInfoEmailAddr.visibility = View.GONE
                binding.customerInfoEmailBtn.visibility = View.GONE
            }

            binding.customerInfoViewMore.setOnClickListener {
                val isChecked = binding.customerInfoViewMoreButtonImage.rotation == 0F
                if (isChecked) {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_SHOW_BILLING_TAPPED)
                    binding.customerInfoMorePanel.expand()
                    binding.customerInfoViewMoreButtonImage.animate().rotation(180F).setDuration(200).start()
                    binding.customerInfoViewMoreButtonTitle.text = context.getString(R.string.orderdetail_hide_billing)
                } else {
                    AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_HIDE_BILLING_TAPPED)
                    binding.customerInfoMorePanel.collapse()
                    binding.customerInfoViewMoreButtonImage.animate().rotation(0F).setDuration(200).start()
                    binding.customerInfoViewMoreButtonTitle.text = context.getString(R.string.orderdetail_show_billing)
                }
            }
        }
    }

    fun isShippingAvailable(order: WCOrderModel) =
        order.toAppModel().shippingAddress.getEnvelopeAddress().isNotEmpty()

    fun initShippingSection(order: WCOrderModel, hide: Boolean) {
        if (hide) {
            binding.customerInfoShippingSection.hide()
        } else {
            if (!isShippingAvailable(order)) {
                binding.customerInfoShippingAddr.text = context.getString(R.string.orderdetail_empty_shipping_address)
                binding.customerInfoShippingMethodSection.hide()
            } else {
                val shippingName = context
                    .getString(R.string.customer_full_name, order.shippingFirstName, order.shippingLastName)
                val shippingAddress = order.toAppModel().shippingAddress.getEnvelopeAddress()
                val shippingCountry = AddressUtils.getCountryLabelByCountryCode(order.shippingCountry)
                val shippingAddressFull = getFullAddress(shippingName, shippingAddress, shippingCountry)
                binding.customerInfoShippingAddr.text = shippingAddressFull

                val shippingMethodList = order.getShippingLineList()
                if (shippingMethodList.isNullOrEmpty()) {
                    binding.customerInfoShippingMethodSection.hide()
                } else {
                    binding.customerInfoShippingMethodSection.show()
                    binding.customerInfoShippingMethod.text = shippingMethodList.first().methodTitle
                }
            }
        }
    }

    private fun getBillingInformation(order: WCOrderModel): String {
        val billingName = context
            .getString(R.string.customer_full_name, order.billingFirstName, order.billingLastName)
        val billingAddress = order.toAppModel().billingAddress.getEnvelopeAddress()
        val billingCountry = AddressUtils.getCountryLabelByCountryCode(order.billingCountry)
        return getFullAddress(billingName, billingAddress, billingCountry)
    }

    private fun getFullAddress(name: String, address: String, country: String): String {
        var fullAddr = if (!name.isBlank()) "$name\n" else ""
        if (!address.isBlank()) fullAddr += "$address\n"
        if (!country.isBlank()) fullAddr += country
        return fullAddr
    }

    private fun showCallOrMessagePopup(order: WCOrderModel) {
        val popup = PopupMenu(context, binding.customerInfoCallOrMessageBtn)
        popup.menuInflater.inflate(R.menu.menu_order_detail_phone_actions, popup.menu)

        popup.menu.findItem(R.id.menu_call)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED)
            OrderCustomerHelper.dialPhone(context, order.toAppModel(), order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.menu.findItem(R.id.menu_message)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED)
            OrderCustomerHelper.sendSms(context, order.toAppModel(), order.billingPhone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.show()
    }

    private fun formatViewAsShippingOnly() {
        binding.customerInfoViewMore.hide()
        binding.customerInfoMorePanel.hide()
        binding.customerInfoViewMore.setOnClickListener(null)
    }
}
