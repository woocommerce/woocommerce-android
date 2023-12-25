package com.woocommerce.android.ui.plans.repository

import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.plans.domain.FREE_TRIAL_PLAN_ID
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.Error
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.ExpiryAt
import com.woocommerce.android.ui.plans.domain.FreeTrialExpiryDateResult.NotTrial
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.networking.SitePlanRestClient
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder.Response.Success
import org.wordpress.android.util.AppLog
import javax.inject.Inject

class SitePlanRepository @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val sitePlanRestClient: SitePlanRestClient
) {

    suspend fun fetchCurrentPlanDetails(site: SiteModel): SitePlan? {
        if (site.connectionType == SiteConnectionType.ApplicationPasswords) return null

        return withContext(dispatchers.io) {
            sitePlanRestClient.fetchCurrentPlanDetails(site).let { result: Response<SitePlan?> ->
                when (result) {
                    is Success -> result.data
                    is Response.Error -> null
                }
            }
        }
    }

    suspend fun fetchFreeTrialExpiryDate(site: SiteModel): FreeTrialExpiryDateResult = withContext(dispatchers.io) {
        sitePlanRestClient.fetchSitePlans(site).let { result ->
            when (result) {
                is Success -> {
                    val freeTrialPlan: SitePlan? = result.data[FREE_TRIAL_PLAN_ID.toInt()]

                    if (freeTrialPlan?.expirationDate == null) {
                        NotTrial
                    } else {
                        ExpiryAt(freeTrialPlan.expirationDate)
                    }
                }

                is Response.Error -> {
                    AppLog.e(AppLog.T.API, result.error.toString())
                    Error(result.error.message)
                }
            }
        }
    }
}
