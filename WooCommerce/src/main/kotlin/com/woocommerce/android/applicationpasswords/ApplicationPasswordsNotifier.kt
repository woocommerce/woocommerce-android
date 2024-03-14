package com.woocommerce.android.applicationpasswords

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val selectedSite: SelectedSite
) : ApplicationPasswordsListener {
    companion object {
        private const val UNAUTHORIZED_STATUS_CODE = 401
    }

    private val _featureUnavailableEvents = MutableSharedFlow<WPAPINetworkError>(extraBufferCapacity = 1)
    val featureUnavailableEvents: Flow<WPAPINetworkError> = _featureUnavailableEvents.asSharedFlow()

    private val _passwordGenerationFailures =
        MutableSharedFlow<ApplicationPasswordGenerationException>(extraBufferCapacity = 1)
    val passwordGenerationFailures: Flow<ApplicationPasswordGenerationException> =
        _passwordGenerationFailures.asSharedFlow()

    override fun onFeatureUnavailable(siteModel: SiteModel, networkError: WPAPINetworkError) {
        trackGenerationFailure(
            cause = GenerationFailureCause.FEATURE_DISABLED,
            networkError = networkError
        )
        _featureUnavailableEvents.tryEmit(networkError)
    }

    override fun onPasswordGenerationFailed(networkError: WPAPINetworkError) {
        val statusCode = networkError.volleyError?.networkResponse?.statusCode
        trackGenerationFailure(
            cause = if (statusCode == UNAUTHORIZED_STATUS_CODE) {
                GenerationFailureCause.AUTHORIZATION_FAILED
            } else {
                GenerationFailureCause.OTHER
            },
            networkError = networkError
        )
        _passwordGenerationFailures.tryEmit(ApplicationPasswordGenerationException(networkError))
    }

    override fun onNewPasswordCreated(isPasswordRegenerated: Boolean) {
        val scenario = if (isPasswordRegenerated) {
            GenerationFailureScenario.REGENERATION
        } else {
            GenerationFailureScenario.GENERATION
        }
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.APPLICATION_PASSWORDS_NEW_PASSWORD_CREATED,
            properties = mapOf(AnalyticsTracker.KEY_SCENARIO to scenario.name.lowercase())
        )
    }

    private fun trackGenerationFailure(
        cause: GenerationFailureCause,
        networkError: WPAPINetworkError
    ) {
        val scenario = if (selectedSite.exists()) {
            GenerationFailureScenario.REGENERATION
        } else {
            GenerationFailureScenario.GENERATION
        }

        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.APPLICATION_PASSWORDS_GENERATION_FAILED,
            properties = mapOf(
                AnalyticsTracker.KEY_SCENARIO to scenario.name.lowercase(),
                AnalyticsTracker.KEY_CAUSE to cause.name.lowercase()
            ),
            errorContext = networkError.javaClass.simpleName,
            errorType = networkError.type.name,
            errorDescription = networkError.message
        )
    }

    private enum class GenerationFailureCause {
        AUTHORIZATION_FAILED, FEATURE_DISABLED, OTHER
    }

    private enum class GenerationFailureScenario {
        GENERATION, REGENERATION
    }
}

data class ApplicationPasswordGenerationException(val networkError: WPAPINetworkError) : Exception(networkError.message)
