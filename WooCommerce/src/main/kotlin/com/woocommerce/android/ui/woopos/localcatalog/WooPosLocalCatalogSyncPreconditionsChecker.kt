package com.woocommerce.android.ui.woopos.localcatalog

import androidx.work.ListenableWorker
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.featureflags.WooPosLocalCatalogM1Enabled
import com.woocommerce.android.ui.woopos.util.datastore.WooPosPreferencesRepository
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

class WooPosLocalCatalogSyncPreconditionsChecker @Inject constructor(
    private val accountRepository: AccountRepository,
    private val selectedSite: SelectedSite,
    private val featureFlagM1Enabled: WooPosLocalCatalogM1Enabled,
    private val preferencesRepository: WooPosPreferencesRepository,
    private val logger: WooPosLogWrapper,
) {

    sealed class PreconditionResult {
        data class Proceed(val site: SiteModel) : PreconditionResult()
        data class Skip(val workerResult: ListenableWorker.Result, val reason: String) : PreconditionResult()
    }

    @Suppress("ReturnCount")
    suspend fun checkPreconditions(): PreconditionResult {
        if (!featureFlagM1Enabled.invoke()) {
            val reason = "Feature flag disabled, skipping local catalog sync"
            logger.d(reason)
            return PreconditionResult.Skip(ListenableWorker.Result.failure(), reason)
        }

        if (!accountRepository.isUserLoggedIn()) {
            val reason = "User not logged in, skipping local catalog sync"
            logger.d(reason)
            return PreconditionResult.Skip(ListenableWorker.Result.failure(), reason)
        }

        val site = selectedSite.getOrNull()
        if (site == null) {
            val reason = "No selected WooCommerce site found, skipping local catalog sync"
            logger.w(reason)
            return PreconditionResult.Skip(ListenableWorker.Result.failure(), reason)
        }

        if (!preferencesRepository.isPeriodicSyncEnabledForSite(site.siteId)) {
            val reason = "Periodic sync permanently disabled for site ${site.url}, skipping local catalog sync."
            logger.w(reason)
            return PreconditionResult.Skip(ListenableWorker.Result.failure(), reason)
        }

        val lastUsedTimestamp = preferencesRepository.getLastUsedTimestamp()
        if (lastUsedTimestamp != null) {
            val daysSinceLastUse = (System.currentTimeMillis() - lastUsedTimestamp).milliseconds.inWholeDays
            if (daysSinceLastUse > DAYS_SINCE_LAST_USE_THRESHOLD) {
                val reason = "POS not used in the last $DAYS_SINCE_LAST_USE_THRESHOLD days " +
                    "(last used $daysSinceLastUse days ago), skipping background full catalog sync."
                logger.d(reason)
                return PreconditionResult.Skip(ListenableWorker.Result.success(), reason)
            }
        }

        return PreconditionResult.Proceed(site)
    }

    private companion object {
        private const val DAYS_SINCE_LAST_USE_THRESHOLD = 30L
    }
}
