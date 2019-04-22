package com.woocommerce.android.ui.sitepicker

import com.woocommerce.android.ui.base.BasePresenter
import com.woocommerce.android.ui.base.BaseView
import org.wordpress.android.fluxc.model.SiteModel

interface SitePickerContract {
    interface Presenter : BasePresenter<View> {
        fun loadAndFetchSites()
        fun loadSites()
        fun getWooCommerceSites(): List<SiteModel>
        fun getSiteBySiteId(siteId: Long): SiteModel?
        fun getUserAvatarUrl(): String?
        fun getUserName(): String?
        fun getUserDisplayName(): String?
        fun logout()
        fun userIsLoggedIn(): Boolean
        fun getSitesForLocalIds(siteIdList: IntArray): List<SiteModel>
        fun verifySiteApiVersion(site: SiteModel)
        fun updateWooSiteSettings(site: SiteModel)
    }

    interface View : BaseView<Presenter> {
        fun showUserInfo()
        fun showStoreList(wcSites: List<SiteModel>)
        fun showNoStoresView()
        fun didLogout()
        fun siteSelected(site: SiteModel)
        fun siteVerificationPassed(site: SiteModel)
        fun siteVerificationFailed(site: SiteModel)
        fun siteVerificationError(site: SiteModel)
        fun showSkeleton(show: Boolean)
    }
}
