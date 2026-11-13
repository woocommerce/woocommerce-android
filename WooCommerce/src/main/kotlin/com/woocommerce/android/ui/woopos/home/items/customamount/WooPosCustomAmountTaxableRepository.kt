package com.woocommerce.android.ui.woopos.home.items.customamount

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class WooPosCustomAmountTaxableRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val appPrefs: AppPrefs,
) {
    fun getLastIsTaxable(): Boolean {
        val site = selectedSite.getOrNull() ?: return false
        return appPrefs.getPOSCustomAmountLastTaxableForSite(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
        )
    }

    fun saveLastIsTaxable(isTaxable: Boolean) {
        val site = selectedSite.getOrNull() ?: return
        appPrefs.setPOSCustomAmountLastTaxableForSite(
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId,
            isTaxable = isTaxable,
        )
    }
}
