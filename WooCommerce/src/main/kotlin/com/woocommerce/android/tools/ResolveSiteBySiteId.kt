package com.woocommerce.android.tools

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

// Drop-in replacement for SiteStore.getSiteBySiteId that also supports application-password logins.
// App-password sites have no wpcom site id, so the payload siteId is unreliable; fall back to the
// selected site since multi-site isn't supported with app passwords.
class ResolveSiteBySiteId @Inject constructor(
    private val selectedSite: SelectedSite,
    private val siteStore: SiteStore
) {
    operator fun invoke(siteId: Long): SiteModel? =
        if (selectedSite.connectionType == SiteConnectionType.ApplicationPasswords) {
            selectedSite.getOrNull()
        } else {
            siteStore.getSiteBySiteId(siteId)
        }
}
