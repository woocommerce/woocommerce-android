package com.woocommerce.android.ui.login.accountmismatch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class AccountMismatchRepository @Inject constructor(
    private val jetpackStore: JetpackStore,
    private val siteStore: SiteStore
) {
    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
    }

    suspend fun fetchJetpackConnectionUrl(site: SiteModel): Result<String> {
        val result = jetpackStore.fetchJetpackConnectionUrl(site)
        return when {
            result.isError -> Result.failure(Exception(result.error.message))
            else -> Result.success(result.url)
        }
    }
}
