package org.wordpress.android.fluxc.network.rest.wpapi.settings

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WPAPI
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpapi.CookieNonceWPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetwork
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkingMode
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsNetwork
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsErrorHandler
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.JetpackApplicationPasswordsSupport
import org.wordpress.android.fluxc.network.rest.wpcom.JetpackTunnelWPAPINetwork
import org.wordpress.android.util.AppLog
import java.security.GeneralSecurityException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WPSettingsRestClient @Inject constructor(
    private val cookieNonceWPAPINetwork: CookieNonceWPAPINetwork,
    private val applicationPasswordsConfiguration: ApplicationPasswordsConfiguration,
    private val applicationPasswordsNetwork: ApplicationPasswordsNetwork,
    private val jetpackTunnelWPAPINetwork: JetpackTunnelWPAPINetwork,
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport,
    private val jetpackApplicationPasswordsErrorHandler: JetpackApplicationPasswordsErrorHandler,
    private val applicationPasswordsStore: ApplicationPasswordsStore
) {
    suspend fun fetchSiteSettings(site: SiteModel): WPAPIResponse<SiteSettingsResponse> {
        return when (site.origin) {
            SiteModel.ORIGIN_WPCOM_REST -> fetchForJetpackSite(site)
            else -> getDirectNetwork(site).fetchSettings(site)
        }
    }

    private suspend fun fetchForJetpackSite(site: SiteModel): WPAPIResponse<SiteSettingsResponse> {
        val appPasswordsEnabled = applicationPasswordsConfiguration.isEnabledForJetpackAccess()
        if (!appPasswordsEnabled || !jetpackApplicationPasswordsSupport.supportsAppPasswords(site)) {
            return jetpackTunnelWPAPINetwork.fetchSettings(site)
                .copyWith(WPAPINetworkingMode.JetpackTunnel())
        }

        val applicationPasswordsResponse = try {
            applicationPasswordsNetwork.fetchSettings(site)
        } catch (e: GeneralSecurityException) {
            AppLog.e(AppLog.T.API, "Error setting up Application Passwords encryption", e)
            WPAPIResponse.Error(
                error = WPAPINetworkError(
                    baseError = BaseRequest.BaseNetworkError(BaseRequest.GenericErrorType.UNKNOWN),
                    errorCode = ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
                )
            )
        }

        return when (applicationPasswordsResponse) {
            is WPAPIResponse.Success -> applicationPasswordsResponse
                .copyWith(WPAPINetworkingMode.ApplicationPasswordsWithJetpack)

            is WPAPIResponse.Error -> {
                jetpackTunnelWPAPINetwork.fetchSettings(site).also { fallbackResponse ->
                    if (fallbackResponse is WPAPIResponse.Success) {
                        jetpackApplicationPasswordsErrorHandler.handleError(site, applicationPasswordsResponse.error)
                    }
                }.copyWith(
                    WPAPINetworkingMode.JetpackTunnel(
                        isFallback = true,
                        applicationPasswordsError = applicationPasswordsResponse.error
                    )
                )
            }
        }
    }

    private fun getDirectNetwork(site: SiteModel): WPAPINetwork {
        return when {
            applicationPasswordsStore.hasCredentials(site) ||
                site.username.isNullOrEmpty() ||
                site.password.isNullOrEmpty() -> applicationPasswordsNetwork
            else -> cookieNonceWPAPINetwork
        }
    }

    private suspend fun WPAPINetwork.fetchSettings(site: SiteModel): WPAPIResponse<SiteSettingsResponse> {
        return executeGetGsonRequest(
            site = site,
            path = WPAPI.settings.urlV2,
            clazz = SiteSettingsResponse::class.java,
            params = mapOf(START_OF_WEEK_FIELDS_PARAM to START_OF_WEEK_FIELD)
        )
    }

    private fun <T> WPAPIResponse<T>.copyWith(networkingMode: WPAPINetworkingMode?): WPAPIResponse<T> {
        return when (this) {
            is WPAPIResponse.Success -> copy(networkingMode = networkingMode)
            is WPAPIResponse.Error -> copy(networkingMode = networkingMode)
        }
    }

    private companion object {
        const val START_OF_WEEK_FIELDS_PARAM = "_fields"
        const val START_OF_WEEK_FIELD = "start_of_week"
    }
}

data class SiteSettingsResponse(
    @SerializedName("start_of_week") val startOfWeek: Int? = null
)
