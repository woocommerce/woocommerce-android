package com.woocommerce.android.ui.coupons.edit

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.common.texteditor.SimpleTextEditorStrategy
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditExcludedProductCategories
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditExcludedProducts
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProductCategories
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.EditIncludedProducts
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenCouponRestrictions
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel

object EditCouponNavigator {
    fun navigate(fragment: Fragment, target: EditCouponNavigationTarget) {
        val navController = fragment.findNavController()
        when (target) {
            is OpenDescriptionEditor -> {
                navController.navigateSafely(
                    NavGraphMainDirections.actionGlobalSimpleTextEditorFragment(
                        currentText = target.currentDescription,
                        screenTitle = fragment.getString(string.coupon_edit_description_editor_title),
                        hint = fragment.getString(string.coupon_edit_add_description_hint),
                        strategy = SimpleTextEditorStrategy.SEND_RESULT_ON_NAVIGATE_BACK
                    )
                )
            }
            is EditIncludedProducts -> {
                navController.navigateSafely(
                    EditCouponFragmentDirections.actionEditCouponFragmentToProductSelectorFragment(
                        selectedItems = target.selectedItems.toTypedArray(),
                        productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.CouponEdition
                    )
                )
            }
            is OpenCouponRestrictions -> {
                navController.navigateSafely(
                    EditCouponFragmentDirections.actionEditCouponFragmentToCouponRestrictionsFragment(
                        target.restrictions,
                        target.currencyCode,
                        target.showLimitUsageToXItems
                    )
                )
            }
            is EditIncludedProductCategories -> {
                navController.navigateSafely(
                    EditCouponFragmentDirections.actionEditCouponFragmentToProductCategorySelectorFragment(
                        categoryIds = target.categoryIds.toLongArray()
                    )
                )
            }
            is EditExcludedProducts -> {
                navController.navigateSafely(
                    CouponRestrictionsFragmentDirections.actionCouponRestrictionsFragmentToProductSelectorFragment(
                        selectedItems = target.excludedItems.toTypedArray(),
                        productSelectorFlow = ProductSelectorViewModel.ProductSelectorFlow.CouponEdition
                    )
                )
            }
            is EditExcludedProductCategories -> {
                navController.navigateSafely(
                    CouponRestrictionsFragmentDirections.actionCouponRestrictionsToProductCategorySelector(
                        categoryIds = target.excludedCategoryIds.toLongArray()
                    )
                )
            }
        }
    }
}
