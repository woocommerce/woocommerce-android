package com.woocommerce.android.ui.orders.creation.navigation

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.ui.orders.creation.OrderCreateEditFormFragmentDirections
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.creation.configuration.Flow
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.AddCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCoupon
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.SelectItems
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.util.FeatureFlag

object OrderCreateEditNavigator {
    @Suppress("LongMethod", "ComplexMethod")
    fun navigate(fragment: Fragment, target: OrderCreateEditNavigationTarget) {
        val navController = fragment.findNavController()
        val action = when (target) {
            is EditCustomer ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerFragment(
                    editingOfAddedCustomer = true
                )

            is AddCustomer ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToCustomerListFragment(
                    allowCustomerCreation = true,
                    allowGuests = true
                )

            is EditCustomerNote ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCustomerNoteFragment()

            is SelectItems -> {
                val flow = when (target.mode) {
                    is OrderCreateEditViewModel.Mode.Creation ->
                        ProductSelectorViewModel.ProductSelectorFlow.OrderCreation
                    is OrderCreateEditViewModel.Mode.Edit -> ProductSelectorViewModel.ProductSelectorFlow.OrderEditing
                }
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToProductSelectorFragment(
                    selectedItems = target.selectedItems.toTypedArray(),
                    productSelectorFlow = flow
                )
            }

            is EditFee ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationEditFeeFragment(
                    orderSubTotal = target.orderSubTotal,
                    currentFeeValue = target.currentFeeValue
                )

            is ShowCreatedOrder -> {
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToOrderDetailFragment(target.orderId, target.startPaymentFlow)
            }

            is EditShipping -> {
                if (FeatureFlag.EOSL_M1.isEnabled()) {
                    OrderCreateEditFormFragmentDirections
                        .actionOrderCreationFragmentToOrderCreateEditUpdateShippingFragment(
                            target.currentShippingLine
                        )
                } else {
                    OrderCreateEditFormFragmentDirections
                        .actionOrderCreationFragmentToOrderCreationShippingFragment(target.currentShippingLine)
                }
            }

            is EditCoupon ->
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCouponEditFragment(
                    orderCreationMode = target.orderCreationMode,
                    couponCode = target.couponCode
                )

            is OrderCreateEditNavigationTarget.CouponList -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToOrderCreationCouponListFragment(
                    orderCreationMode = target.orderCreationMode,
                    couponLines = target.couponLines.toTypedArray()
                )
            }

            is OrderCreateEditNavigationTarget.AddCoupon -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToCouponSelectorFragment()
            }

            is OrderCreateEditNavigationTarget.TaxRatesInfoDialog -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToTaxRatesInfoDialogFragment(
                    target.state
                )
            }

            is OrderCreateEditNavigationTarget.TaxRateSelector -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToTaxRateSelectorFragment(
                    target.state
                )
            }

            is OrderCreateEditNavigationTarget.AutoTaxRateSettingDetails -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToAutoTaxRateDetailsFragment(
                    target.state
                )
            }

            is OrderCreateEditNavigationTarget.SimplePaymentsMigrationBottomSheet -> {
                OrderCreateEditFormFragmentDirections
                    .actionOrderCreationFragmentToSimplePaymentsMigrationBottomSheetFragment()
            }

            is OrderCreateEditNavigationTarget.EditDiscount -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationToOrderCreationProductDiscountFragment(
                    target.item,
                    target.currency
                )
            }

            is OrderCreateEditNavigationTarget.CustomAmountDialog -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToCustomAmountsDialog(
                    target.customAmountUIModel,
                    target.orderTotal
                )
            }
            is OrderCreateEditNavigationTarget.EditOrderCreationProductConfiguration -> {
                val flow = Flow.Edit(
                    itemId = target.itemId,
                    productID = target.productId,
                    configuration = target.configuration
                )
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToEditConfiguration(flow)
            }
            is OrderCreateEditNavigationTarget.AddGiftCard -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToGiftCardFragment(giftCard = null)
            }
            is OrderCreateEditNavigationTarget.EditGiftCard -> {
                OrderCreateEditFormFragmentDirections.actionOrderCreationFragmentToGiftCardFragment(target.giftCard)
            }
        }
        navController.navigate(action)
    }
}
