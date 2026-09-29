package com.woocommerce.android.ui.jetpack

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.JetpackStore
import javax.inject.Inject

/**
 * Checks whether the site's Jetpack connection is in Offline Mode via the public
 * `/jetpack/v4/connection` endpoint.
 *
 * Offline Mode is commonly left behind by `define( 'WP_ENVIRONMENT_TYPE', 'local' )` in `wp-config.php`
 * when a site is cloned from a local or staging environment. While it's active, Jetpack revokes the
 * connection capabilities for every user, so `/connection/data` returns 403 for administrators too. The
 * bare `/connection` endpoint is not gated by that capability and still reports the Offline Mode state.
 */
class IsJetpackInOfflineMode @Inject constructor(
    private val jetpackStore: JetpackStore
) {
    suspend operator fun invoke(
        site: SiteModel,
        useApplicationPasswords: Boolean
    ): Boolean {
        val result = jetpackStore.fetchJetpackConnection(
            site = site,
            useApplicationPasswords = useApplicationPasswords
        )
        return result.data?.offlineMode?.isActive == true
    }
}
