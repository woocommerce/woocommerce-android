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
import com.woocommerce.android.ui.orders.OrderNavigationTarget.OpenTrackingBarcodeScanning
import com.woocommerce.android.ui.orders.OrderNavigationTarget.PreviewReceipt
import com.woocommerce.android.ui.orders.OrderNavigationTarget.RefundShippingLabel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartPaymentFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.StartWooShippingLabelCreationFlow
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCreateShippingLabelInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewCustomFields
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderFulfillInfo
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderedAddons
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewPrintingInstructions
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewRefundedProducts
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShipmentTrackingProviders
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingFragmentDirections
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
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
                        orderStatusList = target.orderStatusList,
                        positiveButtonLabel = R.string.apply
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
                    .actionOrderDetailFragmentToWooShippingLabelRefundRequestFragment(
                        orderId = target.remoteOrderId,
                        labelId = target.shippingLabelId
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

            is OpenTrackingBarcodeScanning -> {
                val action = AddOrderShipmentTrackingFragmentDirections
                    .actionAddOrderShipmentTrackingFragmentToBarcodeScanningFragment()
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
            is StartPaymentFlow -> {
                val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToCardReaderFlow(
                    CardReaderFlowParam.PaymentOrRefund.Payment(target.orderId, target.paymentTypeFlow)
                )
                fragment.findNavController().navigateSafely(directions = action)
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
                    target.appliedDiscount,
                    target.orderCurrency
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
                val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToCustomFieldsFragment(
                    parentItemId = target.orderId
                )
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

            is StartWooShippingLabelCreationFlow -> {
                val action = OrderDetailFragmentDirections.actionOrderDetailFragmentToWooShippingLabelCreationFragment(
                    target.orderId,
                    target.shipmentId ?: 0
                )
                fragment.findNavController().navigateSafely(action)
            }
        }
    }
}
