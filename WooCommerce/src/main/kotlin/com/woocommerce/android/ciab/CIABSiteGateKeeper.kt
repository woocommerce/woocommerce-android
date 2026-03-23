package com.woocommerce.android.ciab

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class CIABSiteGateKeeper @Inject constructor(private val selectedSite: SelectedSite) {
    fun isFeatureSupported(
        feature: CIABAffectedFeature
    ): Boolean {
        return when (feature) {
            CIABAffectedFeature.POS -> true

            else -> !isCurrentSiteCIAB()
        }
    }

    fun isFeatureUnsupported(feature: CIABAffectedFeature): Boolean {
        return !isFeatureSupported(feature)
    }

    fun isCurrentSiteCIAB(): Boolean =
        selectedSite.getOrNull()?.isCIABSite() ?: false
}
