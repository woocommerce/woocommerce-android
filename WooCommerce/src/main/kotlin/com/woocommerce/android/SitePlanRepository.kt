package com.woocommerce.android

import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.Error
import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.ExpiryAt
import com.woocommerce.android.SitePlanRepository.FreeTrialExpiryDateResult.NotTrial
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.WPComGsonRequestBuilder
import org.wordpress.android.util.AppLog
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import javax.inject.Inject

class SitePlanRepository @Inject constructor(
    private val dispatchers: CoroutineDispatchers,
    private val sitePlanRestClient: SitePlanRestClient
) {

    companion object {
        const val FREE_TRIAL_PLAN_ID = 1052L
    }

    suspend fun fetchFreeTrialExpiryDate(site: SiteModel): FreeTrialExpiryDateResult = withContext(dispatchers.io) {
        sitePlanRestClient.fetchSitePlans(site).let { result ->
            when (result) {
                is WPComGsonRequestBuilder.Response.Success -> {
                    val freeTrialPlan: SitePlanRestClient.SitePlanDto? = result.data[FREE_TRIAL_PLAN_ID.toInt()]

                    if (freeTrialPlan == null || freeTrialPlan.expirationDate.isNullOrBlank()) {
                        NotTrial
                    } else {
                        ExpiryAt(LocalDate.parse(freeTrialPlan.expirationDate, ISO_OFFSET_DATE_TIME))
                    }
                }
                is WPComGsonRequestBuilder.Response.Error -> {
                    AppLog.e(AppLog.T.API, result.error.toString())
                    Error(result.error.message)
                }
            }
        }
    }

    sealed class FreeTrialExpiryDateResult {
        data class ExpiryAt(val date: LocalDate) : FreeTrialExpiryDateResult()
        object NotTrial : FreeTrialExpiryDateResult()
        data class Error(val message: String) : FreeTrialExpiryDateResult()
    }
}
