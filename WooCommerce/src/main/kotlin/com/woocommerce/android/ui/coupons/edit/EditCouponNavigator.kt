package com.woocommerce.android.ui.coupons.edit

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.NavigateToProductSelector
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.NavigateToVariationSelector
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenCouponRestrictions
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor

object EditCouponNavigator {
    fun navigate(fragment: Fragment, target: EditCouponNavigationTarget) {
        val navController = fragment.findNavController()
        when (target) {
            is OpenDescriptionEditor -> {
                navController.navigateSafely(
                    NavGraphMainDirections.actionGlobalSimpleTextEditorFragment(
                        currentText = target.currentDescription,
                        screenTitle = fragment.getString(R.string.coupon_edit_description_editor_title),
                        hint = fragment.getString(R.string.coupon_edit_add_description_hint)
                    )
                )
            }
            is NavigateToProductSelector -> {
                navController.navigateSafely(
                    EditCouponFragmentDirections.actionEditCouponFragmentToProductSelectorFragment(
                        target.selectedProductIds.toLongArray()
                    )
                )
            }
            is NavigateToVariationSelector -> {
            }
            is OpenCouponRestrictions -> {
                navController.navigate(
                    EditCouponFragmentDirections.actionEditCouponFragmentToCouponRestrictionsFragment(
                        target.restrictions,
                        target.currencyCode,
                        target.showLimitUsageToXItems
                    )
                )
            }
        }
    }
}
