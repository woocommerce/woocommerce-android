package com.woocommerce.android.util.payment

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CardPresentEligibleFeatureChecker @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wcPayStore: WCPayStore
) {
    fun doCheck() {
        val selectedSite = selectedSite.getIfExists() ?: return

        GlobalScope.launch {
            val result = wcPayStore.loadAccount(selectedSite)
            if (!result.isError) isCardPresentEligible = result.model?.isCardPresentEligible ?: false
        }
    }

    companion object {
        var isCardPresentEligible = false

        const val CACHE_VALIDITY_TIME_S = 60 * 10 // 10 minutes
    }
}
