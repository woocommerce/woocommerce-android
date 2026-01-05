package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ClientSideBannerDismissalStorage @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    fun isBannerHidden(bannerId: String): Boolean {
        val site = selectedSite.getIfExists() ?: return false
        return appPrefsWrapper.isClientSideBannerHidden(
            bannerId = bannerId,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }

    fun hideBanner(bannerId: String) {
        val site = selectedSite.getIfExists() ?: return
        appPrefsWrapper.setClientSideBannerHidden(
            bannerId = bannerId,
            isHidden = true,
            localSiteId = site.id,
            remoteSiteId = site.siteId,
            selfHostedSiteId = site.selfHostedSiteId
        )
    }
}
