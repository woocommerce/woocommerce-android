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
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartCardReaderConnectFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartCardReaderPaymentFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderFulfillInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderedAddons
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintCustomsForm
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintingInstructions
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
                        currentStatus = target.currentStatus, orderStatusList = target.orderStatusList
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
                    .actionOrderDetailFragmentToRefundDetailFragment(target.orderId)
                fragment.findNavController().navigateSafely(action)
            }
            is AddOrderNote -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToAddOrderNoteFragment(
                        orderId = target.orderId,
                        orderNumber = target.orderNumber
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewOrderFulfillInfo -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderFulfillFragment(
                        orderId = target.orderId,
                        orderLocalId = target.localOrderId
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is RefundShippingLabel -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderShippingLabelRefundFragment(
                        orderId = target.remoteOrderId, shippingLabelId = target.shippingLabelId
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is AddOrderShipmentTracking -> {
                val action = OrderDetailFragmentDirections
                    .actionGlobalAddOrderShipmentTrackingFragment(
                        orderId = target.orderId,
                        orderLocalId = target.orderLocalId,
                        orderTrackingProvider = target.orderTrackingProvider,
                        isCustomProvider = target.isCustomProvider,
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShipmentTrackingProviders -> {
                val action = AddOrderShipmentTrackingFragmentDirections
                    .actionAddOrderShipmentTrackingFragmentToAddOrderTrackingProviderListFragment(
                        orderId = target.orderId, selectedProvider = target.selectedProvider
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is PrintShippingLabel -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToPrintShippingLabelFragment(
                        orderId = target.remoteOrderId,
                        shippingLabelIds = longArrayOf(target.shippingLabelId),
                        isReprint = true
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
                    screenTitle = R.string.shipping_label_more_information_title,
                    heading = R.string.shipping_label_more_information_heading,
                    message = R.string.shipping_label_more_information_message,
                    linkTitle = R.string.shipping_label_more_information_link,
                    imageResource = R.drawable.img_print_with_phone,
                    linkAction = LearnMoreAboutShippingLabels
                )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShippingLabelFormatOptions -> {
                val action = PrintShippingLabelFragmentDirections
                    .actionPrintShippingLabelFragmentToLabelFormatOptionsFragment()
                fragment.findNavController().navigateSafely(action)
            }
            is ViewPrintCustomsForm -> {
                val action = if (target.isReprint) {
                    OrderDetailFragmentDirections
                        .actionOrderDetailFragmentToPrintShippingLabelCustomsFormFragment(
                            invoices = target.invoices.toTypedArray(),
                            isReprint = target.isReprint
                        )
                } else {
                    PrintShippingLabelFragmentDirections
                        .actionPrintShippingLabelFragmentToPrintShippingLabelCustomsFormFragment(
                            invoices = target.invoices.toTypedArray(),
                            isReprint = target.isReprint
                        )
                }
                fragment.findNavController().navigateSafely(action)
            }
            is StartShippingLabelCreationFlow -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToCreateShippingLabelFragment(target.orderId)
                fragment.findNavController().navigateSafely(action)
            }
            is OrderNavigationTarget.ShowCardReaderWelcomeDialog -> {
                val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToCardReaderWelcomeDialog()
                fragment.findNavController().navigateSafely(action)
            }
            is StartCardReaderConnectFlow -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToCardReaderConnectDialog(target.skipOnboarding)
                fragment.findNavController().navigateSafely(action)
            }
            is StartCardReaderPaymentFlow -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToCardReaderPaymentDialog(target.orderId)
                fragment.findNavController().navigateSafely(action)
            }
            is ViewPrintingInstructions -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToPrintingInstructionsFragment()
                fragment.findNavController().navigateSafely(action)
            }
            is PreviewReceipt -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToReceiptPreviewFragment(
                        receiptUrl = target.receiptUrl,
                        billingEmail = target.billingEmail,
                        orderId = target.orderId
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewOrderedAddons -> {
                OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderedAddonFragment(
                        orderId = target.remoteOrderID,
                        orderItemId = target.orderItemID,
                        addonsProductId = target.addonsProductID
                    ).let { fragment.findNavController().navigateSafely(it) }
            }
        }
    }
}
