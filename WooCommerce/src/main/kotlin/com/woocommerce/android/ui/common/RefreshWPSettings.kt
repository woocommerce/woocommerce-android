package com.woocommerce.android.ui.common

import com.woocommerce.android.model.UserRole
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WPSettingsStore
import javax.inject.Inject

class RefreshWPSettings @Inject constructor(
    private val userEligibilityFetcher: UserEligibilityFetcher,
    private val wpSettingsStore: WPSettingsStore
) {
    suspend operator fun invoke(site: SiteModel) {
        if (canFetchWPSettings()) {
            wpSettingsStore.fetchSiteSettings(site)
        }
    }

    private fun canFetchWPSettings(): Boolean {
        val roles = userEligibilityFetcher.getUser()?.roles ?: return true
        // /wp/v2/settings requires manage_options, which Shop Managers do not have.
        return UserRole.Administrator in roles || UserRole.Owner in roles
    }
}
