package com.woocommerce.android.ui.plans.domain

import androidx.navigation.NavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.login.storecreation.dispatcher.PlanUpgradeStartFragment
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class StartUpgradeFlow @AssistedInject constructor(
    @Assisted private val navController: NavController
) {

    operator fun invoke(source: PlanUpgradeStartFragment.PlanUpgradeStartSource) {
        navController.navigateSafely(
            NavGraphMainDirections.actionGlobalPlanUpgradeStartFragment(
                source = source
            )
        )
    }
}
