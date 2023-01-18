package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApplicationPasswordsNotifier @Inject constructor(
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ApplicationPasswordsListener {
    private val _featureUnavailableEvents = MutableSharedFlow<WPAPINetworkError>(extraBufferCapacity = 1)
    val featureUnavailableEvents: Flow<WPAPINetworkError> = _featureUnavailableEvents.asSharedFlow()

    override fun onFeatureUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.APPLICATION_PASSWORDS_NOT_AVAILABLE,
            properties = mapOf(
                AnalyticsTracker.KEY_NETWORK_STATUS_CODE to networkError.volleyError?.networkResponse?.statusCode,
                AnalyticsTracker.KEY_ERROR_CODE to networkError.errorCode,
                AnalyticsTracker.KEY_ERROR_MESSAGE to networkError.message
            )
        )
        _featureUnavailableEvents.tryEmit(networkError)
    }

    override fun onNewPasswordCreated(isPasswordRegenerated: Boolean) {
        analyticsTrackerWrapper.track(stat = AnalyticsEvent.APPLICATION_PASSWORDS_NEW_PASSWORD_CREATED)
    }
}
