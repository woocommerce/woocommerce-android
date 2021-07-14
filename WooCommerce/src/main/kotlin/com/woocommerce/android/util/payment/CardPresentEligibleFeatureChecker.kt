package com.woocommerce.android.util.payment

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardPresentEligibleFeatureChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wcPayStore: WCPayStore
) {
    suspend fun doCheck() {
        val selectedSite = selectedSite.getIfExists() ?: return

        val result = wcPayStore.loadAccount(selectedSite)
        if (!result.isError) AppPrefs.setIsCardPresentEligible(result.model?.isCardPresentEligible ?: false)
        else AppPrefs.setIsCardPresentEligible(false)
    }

    companion object {
        val isCardPresentEligible = AppPrefs.isCardPresentEligible()

        const val CACHE_VALIDITY_TIME_S = 60 * 10 // 10 minutes
    }
}
