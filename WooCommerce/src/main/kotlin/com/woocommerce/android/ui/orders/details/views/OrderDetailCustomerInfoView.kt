package com.woocommerce.android.ui.orders.details.views

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.OrderDetailCustomerInfoBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderCustomerHelper
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.PhoneUtils
import com.woocommerce.android.widgets.AppRatingDialog

class OrderDetailCustomerInfoView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = OrderDetailCustomerInfoBinding.inflate(LayoutInflater.from(ctx), this)

    fun updateCustomerInfo(
        order: Order,
        isVirtualOrder: Boolean, // don't display shipping section for virtual products
        isReadOnly: Boolean
    ) {
        showCustomerNote(order, isReadOnly)
        showShippingAddress(order, isVirtualOrder)
        showBillingInfo(order)
    }

    private fun showBillingInfo(order: Order): String {
        val billingInfo = order.formatBillingInformationForDisplay()
        if (order.billingAddress.hasInfo()) {
            if (billingInfo.isNotEmpty()) {
                binding.customerInfoBillingAddr.visibility = VISIBLE
                binding.customerInfoBillingAddr.text = billingInfo
                binding.customerInfoDivider2.visibility = VISIBLE
            } else {
                binding.customerInfoBillingAddr.visibility = GONE
                binding.customerInfoDivider2.visibility = GONE
            }

            showBillingAddressPhoneInfo(order)
            showBillingAddressEmailInfo(order)
            binding.customerInfoViewMore.setOnClickListener { onViewMoreCustomerInfoClick() }
        } else {
            binding.customerInfoViewMore.hide()
            binding.customerInfoMorePanel.hide()
            binding.customerInfoViewMore.setOnClickListener(null)
        }

        val shippingAddress = order.formatShippingInformationForDisplay()
        if (shippingAddress.isEmpty() && billingInfo.isEmpty()) {
            hide()
        }

        if (FeatureFlag.ORDER_EDITING.isEnabled()) {
            binding.customerInfoBillingAddr.setTextIsSelectable(false)
            binding.customerInfoBillingAddr.setOnClickListener { navigateToBillingAddressEditingView() }
        } else {
            binding.customerInfoBillingAddr.setTextIsSelectable(true)
            binding.customerInfoBillingAddr.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            binding.customerInfoBillingAddr.setOnClickListener(null)
        }

        return billingInfo
    }

    private fun onViewMoreCustomerInfoClick() {
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

    private fun showBillingAddressEmailInfo(order: Order) {
        // display email address info only if available, otherwise, hide the view
        if (order.billingAddress.email.isNotEmpty()) {
            binding.customerInfoEmailAddr.text = order.billingAddress.email
            binding.customerInfoEmailAddr.visibility = VISIBLE
            binding.customerInfoEmailBtn.visibility - VISIBLE
            binding.customerInfoEmailBtn.setOnClickListener {
                AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_EMAIL_MENU_EMAIL_TAPPED)
                OrderCustomerHelper.createEmail(context, order, order.billingAddress.email)
                AppRatingDialog.incrementInteractions()
            }
        } else {
            binding.customerInfoEmailAddr.visibility = GONE
            binding.customerInfoEmailBtn.visibility = GONE
            binding.customerInfoDivider3.visibility = GONE
        }
    }

    private fun showBillingAddressPhoneInfo(order: Order) {
        // display phone only if available, otherwise, hide the view
        if (order.billingAddress.phone.isNotEmpty()) {
            binding.customerInfoPhone.text = PhoneUtils.formatPhone(order.billingAddress.phone)
            binding.customerInfoPhone.visibility = VISIBLE
            binding.customerInfoDivider3.visibility = VISIBLE
            binding.customerInfoCallOrMessageBtn.visibility = VISIBLE
            binding.customerInfoCallOrMessageBtn.setOnClickListener {
                showCallOrMessagePopup(order)
            }
        } else {
            binding.customerInfoPhone.visibility = GONE
            binding.customerInfoDivider3.visibility = GONE
            binding.customerInfoCallOrMessageBtn.visibility = GONE
        }
    }

    private fun showCustomerNote(order: Order, isReadOnly: Boolean) {
        val isEmpty = order.customerNote.isEmpty()
        val isEditable = FeatureFlag.ORDER_EDITING.isEnabled() && !isReadOnly

        if (isEmpty && !isEditable) {
            binding.customerInfoCustomerNoteSection.hide()
            return
        }

        binding.customerInfoCustomerNoteSection.show()
        val text = if (!isEmpty) {
            context.getString(
                R.string.orderdetail_customer_note,
                order.customerNote
            )
        } else {
            ""
        }
        binding.customerInfoCustomerNote.setText(text, R.string.order_detail_add_customer_note)

        // TODO right now we only make the text selectable when the feature flag is NOT enabled to
        // mimic the existing behavior. We can remove this when the feature flag is removed
        if (isEditable) {
            binding.customerInfoCustomerNote.setTextIsSelectable(false)
            binding.customerInfoCustomerNoteSection.setOnClickListener {
                val action =
                    OrderDetailFragmentDirections.actionOrderDetailFragmentToEditCustomerOrderNoteFragment()
                findNavController().navigateSafely(action)
            }
        } else {
            binding.customerInfoCustomerNote.setTextIsSelectable(true)
        }
    }

    private fun showShippingAddress(order: Order, isVirtualOrder: Boolean): String {
        val shippingAddress = order.formatShippingInformationForDisplay()
        when {
            isVirtualOrder -> {
                binding.customerInfoShippingSection.hide()
                binding.customerInfoViewMore.hide()
                binding.customerInfoMorePanel.show()
                binding.customerInfoMorePanel.expand()
                binding.customerInfoViewMore.setOnClickListener(null)
            }
            shippingAddress.isEmpty() -> {
                binding.customerInfoShippingAddr.text = context.getString(R.string.orderdetail_empty_shipping_address)
                binding.customerInfoShippingMethodSection.hide()
            }
            else -> {
                binding.customerInfoShippingAddr.text = shippingAddress
                binding.customerInfoShippingMethodSection.isVisible = order.shippingMethods.firstOrNull()?.let {
                    binding.customerInfoShippingMethod.text = it.title
                    true
                } ?: false
            }
        }

        if (FeatureFlag.ORDER_EDITING.isEnabled()) {
            binding.customerInfoShippingAddr.setTextIsSelectable(false)
            binding.customerInfoShippingAddr.setOnClickListener { navigateToShippingAddressEditingView() }
        } else {
            binding.customerInfoShippingAddr.setTextIsSelectable(true)
            binding.customerInfoShippingAddr.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            binding.customerInfoShippingAddr.setOnClickListener(null)
        }
        return shippingAddress
    }

    private fun navigateToShippingAddressEditingView() {
        OrderDetailFragmentDirections
            .actionOrderDetailFragmentToShippingAddressEditingFragment()
            .let { findNavController().navigateSafely(it) }
    }

    private fun navigateToBillingAddressEditingView() {
        OrderDetailFragmentDirections
            .actionOrderDetailFragmentToBillingAddressEditingFragment()
            .let { findNavController().navigateSafely(it) }
    }

    private fun showCallOrMessagePopup(order: Order) {
        val popup = PopupMenu(context, binding.customerInfoCallOrMessageBtn)
        popup.menuInflater.inflate(R.menu.menu_order_detail_phone_actions, popup.menu)

        popup.menu.findItem(R.id.menu_call)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_PHONE_TAPPED)
            OrderCustomerHelper.dialPhone(context, order, order.billingAddress.phone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.menu.findItem(R.id.menu_message)?.setOnMenuItemClickListener {
            AnalyticsTracker.track(Stat.ORDER_DETAIL_CUSTOMER_INFO_PHONE_MENU_SMS_TAPPED)
            OrderCustomerHelper.sendSms(context, order, order.billingAddress.phone)
            AppRatingDialog.incrementInteractions()
            true
        }

        popup.show()
    }
}
