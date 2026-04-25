package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.network.qrlogin.QrLoginRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import javax.inject.Inject

/**
 * Glue layer that turns a scanned QR ticket into a logged-in [SelectedSite].
 *
 * Steps:
 *   1. Exchange the QR ticket for an Application Password (unauthenticated POST to the merchant site).
 *   2. Discover and persist the [SiteModel] using the AP credentials.
 *   3. Save the AP into encrypted shared preferences so subsequent REST calls authenticate.
 *   4. Promote the site to the selected site.
 *
 * Returns the local site id so the caller can drive `loggedInViaUsernamePassword(localSiteId)`.
 */
class QrLoginAuthenticator @Inject constructor(
    private val exchangeClient: QrLoginRestClient,
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val selectedSite: SelectedSite
) {
    suspend fun authenticate(ticket: QrLoginPayload.Ticket): Result<Int> {
        val credentials = exchangeClient.exchange(ticket.siteUrl, ticket.token)
            .getOrElse { return Result.failure(it) }

        return try {
            val site = wpApiSiteRepository.fetchSite(
                url = ticket.siteUrl,
                username = credentials.userLogin,
                password = credentials.applicationPassword.reveal()
            ).getOrThrow()
            wpApiSiteRepository.saveApplicationPassword(
                localSiteId = site.id,
                username = credentials.userLogin,
                password = credentials.applicationPassword.reveal()
            )
            selectedSite.set(site)
            Result.success(site.id)
        } catch (ce: CancellationException) {
            throw ce
        } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
            WooLog.e(WooLog.T.LOGIN, "QR login authentication failed", t)
            Result.failure(t)
        }
    }
}
