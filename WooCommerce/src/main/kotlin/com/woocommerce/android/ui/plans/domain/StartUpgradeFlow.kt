package com.woocommerce.android.ui.plans.domain

import androidx.navigation.NavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

class StartUpgradeFlow @AssistedInject constructor(
    @Assisted private val navController: NavController,
    private val selectedSite: SelectedSite,
) {

    operator fun invoke() {
        navController.navigateSafely(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                urlToLoad = "https://wordpress.com/plans/${selectedSite.get().siteId}",
                urlsToTriggerExit = arrayOf(
                    "my-plan/trial-upgraded"
                )
            )
        )
    }
}
