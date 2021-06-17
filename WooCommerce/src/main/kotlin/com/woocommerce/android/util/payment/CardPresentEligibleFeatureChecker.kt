package com.woocommerce.android.util.payment

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCPayStore
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardPresentEligibleFeatureChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wcPayStore: WCPayStore
) {
    suspend fun doCheck() {
        val selectedSite = selectedSite.getIfExists() ?: return

        isCardPresentEligible.set(false)
        val result = wcPayStore.loadAccount(selectedSite)
        if (!result.isError) isCardPresentEligible.set(result.model?.isCardPresentEligible ?: false)
    }

    companion object {
        val isCardPresentEligible = AtomicBoolean(false)

        const val CACHE_VALIDITY_TIME_S = 60 * 10 // 10 minutes
    }
}
