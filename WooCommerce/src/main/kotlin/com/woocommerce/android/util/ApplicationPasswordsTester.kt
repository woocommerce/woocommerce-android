package com.woocommerce.android.util

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import javax.inject.Inject
import javax.inject.Singleton

/**
 * A temporary class to launch a background request for checking for application passwords support,
 * this will allow us to collect data about the support of the feature for current active users
 */
@Singleton
class ApplicationPasswordsTester @Inject constructor(
    @AppCoroutineScope private val coroutineScope: CoroutineScope,
    private val network: ApplicationPasswordsNetwork,
    private val selectedSite: SelectedSite,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) {
    fun testApi() {
        if (!selectedSite.exists()) return
        coroutineScope.launch {
            analyticsTrackerWrapper.track(stat = AnalyticsEvent.APPLICATION_PASSWORDS_TEST_INITIATED)
            network.executeGetGsonRequest(
                site = selectedSite.get(),
                path = "/wc/v3/settings",
                params = mapOf("_fields" to ""),
                clazz = Any::class.java
            ).let {
                if (it is WPAPIResponse.Success<*>) {
                    analyticsTrackerWrapper.track(stat = AnalyticsEvent.APPLICATION_PASSWORDS_AVAILABLE)
                }
            }
        }
    }
}
