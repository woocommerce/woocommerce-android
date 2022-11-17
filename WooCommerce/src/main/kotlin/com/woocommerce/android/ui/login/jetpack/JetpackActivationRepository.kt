package com.woocommerce.android.ui.login.jetpack

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.util.SiteUtils
import javax.inject.Inject

class JetpackActivationRepository @Inject constructor(private val siteStore: SiteStore) {
    suspend fun getSiteByUrl(url: String): SiteModel? = withContext(Dispatchers.IO) {
        SiteUtils.getSiteByMatchingUrl(siteStore, url)
            ?.takeIf {
                val hasCredentials = it.username.isNotNullOrEmpty() && it.password.isNotNullOrEmpty()
                if (!hasCredentials) {
                    WooLog.w(WooLog.T.LOGIN, "The found site for jetpack activation doesn't have credentials")
                }
                hasCredentials
            }
    }
}
