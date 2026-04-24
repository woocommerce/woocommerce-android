package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

/**
 * Glue layer that turns a scanned QR ticket into a logged-in [SelectedSite].
 *
 * Steps:
 *   1. Exchange the QR ticket for an Application Password (unauthenticated POST to the merchant site).
 *   2. Discover and persist the [SiteModel] using the AP credentials.
 *   3. Save the AP into encrypted shared preferences so subsequent REST calls authenticate.
 *   4. Verify the user is eligible to use the app, then promote the site to the selected site.
 *
 * Returns the local site id so the caller can drive `loggedInViaUsernamePassword(localSiteId)`.
 */
class QrLoginAuthenticator @Inject constructor(
    private val exchangeClient: QrLoginExchangeClient,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite
) {
    suspend fun authenticate(ticket: QrLoginPayload.Ticket): Result<Int> {
        val credentials = exchangeClient.exchange(ticket.siteUrl, ticket.token)
            .getOrElse { return Result.failure(it) }

        return try {
            Result.success(authenticateWithCredentials(ticket.siteUrl, credentials))
        } catch (ce: CancellationException) {
            throw ce
        } catch (t: Throwable) {
            Result.failure(t)
        }
    }

    private suspend fun authenticateWithCredentials(siteUrl: String, credentials: QrLoginCredentials): Int {
        val site = fetchAndValidateSite(siteUrl, credentials)
        wpApiSiteRepository.saveApplicationPassword(
            localSiteId = site.id,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        )
        ensureUserEligible(site)
        selectedSite.set(site)
        return site.id
    }

    private suspend fun fetchAndValidateSite(
        siteUrl: String,
        credentials: QrLoginCredentials
    ): SiteModel {
        val site = wpApiSiteRepository.fetchSite(
            url = siteUrl,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        ).getOrThrow()

        if (!site.hasWooCommerce) {
            WooLog.w(WooLog.T.LOGIN, "QR login: site ${site.url} does not have WooCommerce installed")
            throw QrLoginAuthenticationException.NotAWooSite
        }
        return site
    }

    private suspend fun ensureUserEligible(site: SiteModel) {
        val isEligible = wpApiSiteRepository.checkIfUserIsEligible(site)
            .getOrElse { throw QrLoginAuthenticationException.UserNotEligible(it) }
        if (!isEligible) throw QrLoginAuthenticationException.UserNotEligible(original = null)
    }
}

sealed class QrLoginAuthenticationException(message: String) : Exception(message) {
    data object NotAWooSite : QrLoginAuthenticationException("Site is not a WooCommerce store")
    data class UserNotEligible(val original: Throwable?) :
        QrLoginAuthenticationException("User is not eligible to use the app")
}
