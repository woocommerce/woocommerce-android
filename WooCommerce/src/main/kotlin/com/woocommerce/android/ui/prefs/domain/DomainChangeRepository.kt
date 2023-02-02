package com.woocommerce.android.ui.prefs.domain

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

class DomainChangeRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore
) {
    suspend fun fetchSiteDomains(): Result<List<Domain>> {
        val result = withContext(Dispatchers.Default) {
            siteStore.fetchSiteDomains(selectedSite.get())
        }
        return if (result.isError) Result.failure(Exception(result.error.message))
        else Result.success(result.domains ?: emptyList())
    }

    suspend fun fetchActiveSitePlan(): Result<PlanModel> {
        val result = withContext(Dispatchers.Default) {
            siteStore.fetchSitePlans(selectedSite.get())
        }
        val plan = result.plans?.firstOrNull { it.isCurrentPlan }
        return when {
            result.isError -> Result.failure(Exception(result.error.message))
            plan == null -> Result.failure(Exception("No active plan found"))
            else -> Result.success(plan)
        }
    }
}
