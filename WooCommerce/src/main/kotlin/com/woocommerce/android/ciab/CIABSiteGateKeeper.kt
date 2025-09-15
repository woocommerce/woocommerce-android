package com.woocommerce.android.ciab

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class CIABSiteGateKeeper @Inject constructor(private val selectedSite: SelectedSite) {
    fun isFeatureSupported(
        @Suppress("unused")
        feature: CIABAffectedFeature
    ): Boolean {
        // For now, all affected features are unsupported in CIAB.
        // If there are exceptions in the future, we can handle them here.
        return !isCurrentSiteCIAB()
    }

    fun isFeatureUnsupported(feature: CIABAffectedFeature): Boolean {
        return !isFeatureSupported(feature)
    }

    private fun isCurrentSiteCIAB(): Boolean {
        val site = selectedSite.getOrNull() ?: return false
        return site.isGardenSite && site.gardenName == CIAB_GARDEN_NAME
    }

    companion object Companion {
        @VisibleForTesting
        const val CIAB_GARDEN_NAME = "commerce"
    }
}
