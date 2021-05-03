package com.woocommerce.android.ui.orders

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.common.InfoScreenFragment.InfoScreenLinkAction.LearnMoreAboutShippingLabels
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewRefundedProducts
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShipmentTrackingProviders
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelFormatOptions
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelPaperSizes
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragmentDirections
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragmentDirections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderNavigationTarget) {
        when (target) {
            is ViewOrderStatusSelector -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderStatusSelectorDialog(
                        target.currentStatus, target.orderStatusList
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is IssueOrderRefund -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToIssueRefund(target.remoteOrderId)
                fragment.findNavController().navigateSafely(action)
            }
            is ViewRefundedProducts -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToRefundDetailFragment(target.remoteOrderId)
                fragment.findNavController().navigateSafely(action)
            }
            is AddOrderNote -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToAddOrderNoteFragment(target.orderIdentifier, target.orderNumber)
                fragment.findNavController().navigateSafely(action)
            }
            is RefundShippingLabel -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderShippingLabelRefundFragment(
                        target.remoteOrderId, target.shippingLabelId
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is AddOrderShipmentTracking -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToAddOrderShipmentTrackingFragment(
                        target.orderIdentifier, target.orderTrackingProvider, target.isCustomProvider
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShipmentTrackingProviders -> {
                val action = AddOrderShipmentTrackingFragmentDirections
                    .actionAddOrderShipmentTrackingFragmentToAddOrderTrackingProviderListFragment(
                        target.orderIdentifier, target.selectedProvider
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is PrintShippingLabel -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToPrintShippingLabelFragment(
                        target.remoteOrderId, target.shippingLabelId, isReprint = true
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShippingLabelPaperSizes -> {
                val action = PrintShippingLabelFragmentDirections
                    .actionPrintShippingLabelFragmentToShippingLabelPaperSizeSelectorDialog(
                        target.currentPaperSize
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewPrintShippingLabelInfo -> {
                val action = PrintShippingLabelFragmentDirections
                    .actionPrintShippingLabelFragmentToPrintShippingLabelInfoFragment()
                fragment.findNavController().navigateSafely(action)
            }
            is ViewCreateShippingLabelInfo -> {
                val action = NavGraphMainDirections.actionGlobalInfoScreenFragment(
                    R.string.shipping_label_more_information_title,
                    R.string.shipping_label_more_information_heading,
                    R.string.shipping_label_more_information_message,
                    R.string.shipping_label_more_information_link,
                    R.drawable.img_woo_desk_character,
                    LearnMoreAboutShippingLabels
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShippingLabelFormatOptions -> {
                val action = PrintShippingLabelFragmentDirections
                    .actionPrintShippingLabelFragmentToLabelFormatOptionsFragment()
                fragment.findNavController().navigateSafely(action)
            }
            is StartShippingLabelCreationFlow -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToCreateShippingLabelFragment(target.orderIdentifier)
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
