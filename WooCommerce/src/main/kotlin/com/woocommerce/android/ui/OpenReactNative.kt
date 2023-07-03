package com.woocommerce.android.ui

import android.app.Activity
import android.content.Intent
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.shared.library.ReactActivity
import dagger.hilt.android.scopes.ActivityScoped
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import javax.inject.Inject
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken

@ActivityScoped
class OpenReactNative @Inject constructor(
    private val activity: Activity,
    private val accessToken: AccessToken,
    private val selectedSite: SelectedSite,
    private val applicationPasswordsStore: ApplicationPasswordsStore,
) {

    operator fun invoke() {
        val site = selectedSite.get()
        val intent = Intent(activity, ReactActivity::class.java).apply {
            putExtra(ReactActivity.PROPERTY_TOKEN, accessToken.get())
            putExtra(ReactActivity.PROPERTY_BLOG_ID, site.siteId.toString())
            putExtra(ReactActivity.PROPERTY_SITE_URL, site.url.toString())
            putExtra(
                ReactActivity.PROPERTY_APP_PASSWORD,
                applicationPasswordsStore.getApplicationPasswordAuthHeader(site).removePrefix("Basic ")
            )
        }
        activity.startActivity(intent)
    }
}
