package com.woocommerce.android.ui.login.auto

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsStore
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

internal class AutoLoginHandler @Inject constructor(
    private val selectedSite: SelectedSite,
    private val accountStore: AccountStore,
    private val applicationPasswordsStore: ApplicationPasswordsStore,
    private val siteMatcher: AutoLoginSiteMatcher,
    private val applicationPasswordStrategy: ApplicationPasswordAutoLoginStrategy,
    private val wpComStrategy: WPComAutoLoginStrategy
) : AutoLoginRequestHandler {
    override suspend fun login(request: AutoLoginRequest): AutoLoginResult {
        selectedSite.getIfExists()?.let { currentSite ->
            return if (matchesExistingSession(request, currentSite)) {
                AutoLoginResult.AlreadyActive
            } else {
                AutoLoginResult.Failure(AutoLoginStatus.CONFLICT)
            }
        }
        if (selectedSite.getSelectedSiteId() != NO_SELECTED_SITE_ID) {
            return AutoLoginResult.Failure(AutoLoginStatus.CONFLICT)
        }

        val hasToken = accountStore.hasAccessToken()
        val hasPrincipal = accountStore.account.userId > 0L
        return when (request.connection) {
            AutoLoginConnection.WP_API -> {
                if (hasToken || hasPrincipal) {
                    AutoLoginResult.Failure(AutoLoginStatus.CONFLICT)
                } else {
                    applicationPasswordStrategy.login(request).toAutoLoginResult()
                }
            }

            AutoLoginConnection.WPCOM -> {
                if (hasToken != hasPrincipal) {
                    AutoLoginResult.Failure(AutoLoginStatus.CONFLICT)
                } else {
                    wpComStrategy.login(
                        request = request,
                        reuseExistingSession = hasToken
                    ).toAutoLoginResult()
                }
            }
        }
    }

    private fun matchesExistingSession(request: AutoLoginRequest, site: SiteModel): Boolean =
        when (request.connection) {
            AutoLoginConnection.WP_API ->
                !accountStore.hasAccessToken() &&
                    accountStore.account.userId <= 0L &&
                    siteMatcher.matches(site, request.siteUrl, SiteModel.ORIGIN_WPAPI) &&
                    site.username == request.credentials.username &&
                    applicationPasswordsStore.hasCredentials(site)

            AutoLoginConnection.WPCOM ->
                accountStore.hasAccessToken() &&
                    accountStore.account.userId > 0L &&
                    siteMatcher.matches(site, request.siteUrl, SiteModel.ORIGIN_WPCOM_REST)
        }

    private fun ApplicationPasswordAutoLoginStrategy.Result.toAutoLoginResult(): AutoLoginResult =
        when (this) {
            ApplicationPasswordAutoLoginStrategy.Result.Success -> AutoLoginResult.Success
            is ApplicationPasswordAutoLoginStrategy.Result.Failure -> AutoLoginResult.Failure(status)
        }

    private fun WPComAutoLoginStrategy.Result.toAutoLoginResult(): AutoLoginResult =
        when (this) {
            WPComAutoLoginStrategy.Result.Success -> AutoLoginResult.Success
            is WPComAutoLoginStrategy.Result.Failure -> AutoLoginResult.Failure(status)
        }

    private companion object {
        const val NO_SELECTED_SITE_ID = -1
    }
}
