package com.woocommerce.android.ui.plans

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.PlansStore
import javax.inject.Inject

class PlansRepository @Inject constructor(
    private val plansStore: PlansStore,
    private val selectedSite: SelectedSite,
) {

    fun observeCurrentPlan(): Flow<PlanType> {
        return selectedSite.observe().map { site ->
            val availablePlans = plansStore.fetchPlans().plans

            availablePlans?.firstOrNull { plan ->
                plan.productId?.toLong() == site?.planId
            }
        }.map { plan ->
            val slug = plan?.productSlug

            if (slug == "ecommerce-trial-bundle-monthly") {
                PlanType.ECOMMERCE_FREE_TRIAL
            } else if (slug?.contains("ecommerce") == true) {
                PlanType.ECOMMERCE
            } else {
                PlanType.NONE
            }
        }
    }
}
