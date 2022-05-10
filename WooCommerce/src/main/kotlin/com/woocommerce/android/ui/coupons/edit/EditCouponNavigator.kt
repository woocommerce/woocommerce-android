package com.woocommerce.android.ui.coupons.edit

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.ui.coupons.edit.EditCouponNavigationTarget.OpenDescriptionEditor

object EditCouponNavigator {
    fun navigate(fragment: Fragment, target: EditCouponNavigationTarget) {
        val navController = fragment.findNavController()
        when (target) {
            is OpenDescriptionEditor -> navController.navigate(
                NavGraphMainDirections.actionGlobalSimpleTextEditorFragment(target.currentDescription)
            )
        }
    }
}
