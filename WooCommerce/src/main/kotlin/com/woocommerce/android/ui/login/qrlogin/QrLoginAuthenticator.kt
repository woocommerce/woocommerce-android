package com.woocommerce.android.ui.login.qrlogin

import com.woocommerce.android.network.qrlogin.QrLoginCredentials
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.AccountRepository
import com.woocommerce.android.ui.login.WPApiSiteRepository
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CancellationException
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import javax.inject.Inject

/**
 * Glue layer that completes a QR-driven sign-in once the ViewModel has finished the
 * scan / approve / exchange protocol and obtained valid [QrLoginCredentials].
 *
 * Steps:
 *   1. Discover and persist the [SiteModel] using the AP credentials.
 *   2. Save the AP into encrypted shared preferences so subsequent REST calls authenticate.
 *   3. Verify the user is eligible to use the app, then promote the site to the selected site.
 *
 * Returns the local site id so the caller can drive `loggedInViaUsernamePassword(localSiteId)`.
 *
 * The exchange call itself lives in the ViewModel because it's tightly coupled to the
 * number-matching state machine (must come after `/qr-login-approve` returns a grant), so
 * the authenticator stays focused on the post-credentials WP login + site setup.
 */
class QrLoginAuthenticator @Inject constructor(
    private val wpApiSiteRepository: WPApiSiteRepository,
    private val siteStore: SiteStore,
    private val selectedSite: SelectedSite,
    private val accountRepository: AccountRepository
) {
    suspend fun completeLogin(
        ticket: QrLoginPayload.Ticket,
        credentials: QrLoginCredentials,
    ): Result<Int> = try {
        Result.success(authenticateWithCredentials(ticket.siteUrl, credentials))
    } catch (ce: CancellationException) {
        throw ce
    } catch (e: QrLoginAuthenticationException) {
        Result.failure(e)
    } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
        WooLog.e(WooLog.T.LOGIN, "QR login authentication failed", t)
        Result.failure(t)
    }

    private suspend fun authenticateWithCredentials(siteUrl: String, credentials: QrLoginCredentials): Int {
        val site = fetchAndValidateSite(siteUrl, credentials)
        wpApiSiteRepository.saveApplicationPassword(
            localSiteId = site.id,
            username = credentials.userLogin,
            password = credentials.applicationPassword
        )
        try {
            ensureUserEligible(site)
        } catch (ce: CancellationException) {
            revokeAndLogOut(site)
            throw ce
        } catch (@Suppress("TooGenericExceptionCaught") t: Throwable) {
            revokeAndLogOut(site)
            throw t
        }
        selectedSite.set(site)
        return site.id
    }

    /**
     * On post-exchange QR login failure, revoke the just-minted Application Password and tear down
     * any existing session. The app does not support multi-login, so a failed QR attempt must leave
     * the user fully logged out rather than in a half-state.
     */
    private suspend fun revokeAndLogOut(site: SiteModel) {
        val result = siteStore.deleteApplicationPassword(site)
        if (result.isError) {
            WooLog.e(
                WooLog.T.LOGIN,
                "QR login: failed to revoke application password server-side: " +
                    "${result.error?.errorCode} ${result.error?.message}"
            )
        }
        accountRepository.logout()
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
            .getOrElse { cause ->
                WooLog.e(WooLog.T.LOGIN, "QR login: eligibility check failed for ${site.url}", cause)
                throw QrLoginAuthenticationException.UserNotEligible(cause)
            }
        if (!isEligible) throw QrLoginAuthenticationException.UserNotEligible(original = null)
    }
}

sealed class QrLoginAuthenticationException(message: String) : Exception(message) {
    data object NotAWooSite : QrLoginAuthenticationException("Site is not a WooCommerce store")
    data class UserNotEligible(val original: Throwable?) :
        QrLoginAuthenticationException("User is not eligible to use the app")
}
