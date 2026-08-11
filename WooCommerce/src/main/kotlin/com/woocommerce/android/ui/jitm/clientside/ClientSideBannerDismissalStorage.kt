package com.woocommerce.android.ui.jitm.clientside

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import dagger.Reusable
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

@Reusable
class ClientSideBannerDismissalStorage @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val selectedSite: SelectedSite,
) {
    fun isBannerHidden(bannerId: String, site: SiteModel): Boolean {
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
