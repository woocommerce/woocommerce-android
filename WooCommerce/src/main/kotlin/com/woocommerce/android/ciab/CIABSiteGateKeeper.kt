package com.woocommerce.android.ciab

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class CIABSiteGateKeeper @Inject constructor(private val selectedSite: SelectedSite) {
    fun isFeatureSupported(
        @Suppress("unused")
        feature: CIABAffectedFeature
    ): Boolean {
        // For now, all affected features are unsupported in CIAB.
        // If there are exceptions in the future, we can handle them here.
        return !selectedSite.isCurrentSiteCIAB()
    }

    fun isFeatureUnsupported(feature: CIABAffectedFeature): Boolean {
        return !isFeatureSupported(feature)
    }

    companion object Companion {
        const val CIAB_GARDEN_NAME = "commerce"
    }
}
