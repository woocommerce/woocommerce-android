package com.woocommerce.android.ciab

import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject

class CIABSiteGateKeeper @Inject constructor(private val selectedSite: SelectedSite) {
    fun isFeatureSupported(
        feature: CIABAffectedFeature
    ): Boolean {
        return when (feature) {
            CIABAffectedFeature.POS -> true
            CIABAffectedFeature.InPersonPayments -> !isCurrentSiteCIAB() || isCurrentSiteProCIAB()
            else -> !isCurrentSiteCIAB()
        }
    }

    fun isFeatureUnsupported(feature: CIABAffectedFeature): Boolean {
        return !isFeatureSupported(feature)
    }

    fun isCurrentSiteCIAB(): Boolean =
        selectedSite.getOrNull()?.isCIABSite() ?: false

    private fun isCurrentSiteProCIAB(): Boolean {
        val site = selectedSite.getOrNull() ?: return false
        return site.isCIABSite() && CIAB_PRO_PLAN_SLUGS.contains(site.planProductSlug)
    }

    fun buildPlanUpgradeUrl(): String {
        val siteUrl = selectedSite.getOrNull()?.url
        val siteSlug = siteUrl
            ?.removePrefix("https://")
            ?.removePrefix("http://")
            ?.trimEnd('/')
        return if (siteSlug != null) {
            "$LEARN_MORE_BASE_URL?siteSlug=$siteSlug"
        } else {
            LEARN_MORE_BASE_URL
        }
    }

    companion object {
        val CIAB_PRO_PLAN_SLUGS = setOf(
            "woo_hosted_pro_plan_monthly",
            "woo_hosted_pro_plan_yearly",
        )
        private const val LEARN_MORE_BASE_URL = "https://wordpress.com/setup/woo-hosted-plans/"
    }
}
