package com.woocommerce.android.ui.orders

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.common.InfoScreenFragment.InfoScreenLinkAction.LearnMoreAboutShippingLabels
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AIThankYouNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderNote
import com.woocommerce.android.ui.orders.OrderNavigationTarget.AddOrderShipmentTracking
import com.woocommerce.android.ui.orders.OrderNavigationTarget.EditOrder
import com.woocommerce.android.ui.orders.OrderNavigationTarget.IssueOrderRefund
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PrintShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartPaymentFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCustomFields
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
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelFragmentDirections
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragmentDirections
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.util.FeatureFlag
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OrderNavigator @Inject constructor() {
    fun navigate(fragment: Fragment, target: OrderNavigationTarget) {
        when (target) {
            is ViewOrderStatusSelector -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderStatusSelectorDialog(
                        currentStatus = target.currentStatus,
                        orderStatusList = target.orderStatusList
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
                    .actionOrderDetailFragmentToOrderFulfillFragment(orderId = target.orderId)
                fragment.findNavController().navigateSafely(action)
            }
            is RefundShippingLabel -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderShippingLabelRefundFragment(
                        orderId = target.remoteOrderId,
                        shippingLabelId = target.shippingLabelId
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is AddOrderShipmentTracking -> {
                val action = OrderDetailFragmentDirections
                    .actionGlobalAddOrderShipmentTrackingFragment(
                        orderId = target.orderId,
                        orderTrackingProvider = target.orderTrackingProvider,
                        isCustomProvider = target.isCustomProvider,
                    )
                fragment.findNavController().navigateSafely(action)
            }
            is ViewShipmentTrackingProviders -> {
                val action = AddOrderShipmentTrackingFragmentDirections
                    .actionAddOrderShipmentTrackingFragmentToAddOrderTrackingProviderListFragment(
                        orderId = target.orderId,
                        selectedProvider = target.selectedProvider
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
                (fragment.activity as? MainActivity)?.navigateToGlobalInfoScreenFragment(
                    screenTitle = R.string.shipping_label_more_information_title,
                    heading = R.string.shipping_label_more_information_heading,
                    message = R.string.shipping_label_more_information_message,
                    linkTitle = R.string.shipping_label_more_information_link,
                    imageResource = R.drawable.img_print_with_phone,
                    linkAction = LearnMoreAboutShippingLabels
                )
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
            is StartPaymentFlow -> {
                val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToCardReaderFlow(
                    CardReaderFlowParam.PaymentOrRefund.Payment(target.orderId, target.paymentTypeFlow)
                )
                fragment.findNavController().navigateSafely(directions = action, skipThrottling = true)
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
            is EditOrder -> {
                (fragment.activity as? MainActivity)?.showOrderCreation(
                    OrderCreateEditViewModel.Mode.Edit(target.orderId),
                    target.giftCard,
                    target.appliedDiscount
                )
            }
            is OrderNavigationTarget.ShowOrder -> {
                OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToOrderDetailFragment(
                        target.orderId,
                        target.allOrderIds
                    )
                    .let { fragment.findNavController().navigateSafely(it) }
            }

            is ViewCustomFields -> {
                val action = if (FeatureFlag.CUSTOM_FIELDS.isEnabled()) {
                    OrderDetailFragmentDirections.actionOrderDetailFragmentToCustomFieldsFragment(
                        parentItemId = target.orderId
                    )
                } else {
                    OrderDetailFragmentDirections.actionOrderDetailFragmentToCustomOrderFieldsFragment(
                        orderId = target.orderId
                    )
                }
                fragment.findNavController().navigateSafely(action)
            }
            is AIThankYouNote -> {
                val action = OrderDetailFragmentDirections
                    .actionOrderDetailFragmentToAIThankYouNoteBottomSheetFragment(
                        customerName = target.customerName,
                        productName = target.productName,
                        productDescription = target.productDescription
                    )
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
