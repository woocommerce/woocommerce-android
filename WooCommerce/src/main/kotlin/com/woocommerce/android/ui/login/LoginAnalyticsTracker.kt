package com.woocommerce.android.ui.login

import com.woocommerce.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginAnalyticsListener.CreatedAccountSource
import java.util.HashMap
import javax.inject.Singleton

@Singleton
class LoginAnalyticsTracker(val accountStore: AccountStore, val siteStore: SiteStore) : LoginAnalyticsListener {
    override fun trackAnalyticsSignIn(isWpcomLogin: Boolean) {
        AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
        val properties = HashMap<String, Boolean>()
        properties["dotcom_user"] = isWpcomLogin // checkstyle ignore
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_IN, properties)
        if (!isWpcomLogin) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.ADDED_SELF_HOSTED_SITE)
        }
    }

    override fun trackCreatedAccount(username: String?, email: String?, source: CreatedAccountSource) {
        // TODO: Account creation
    }

    override fun trackEmailFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_EMAIL_FORM_VIEWED)
    }

    override fun trackInsertedInvalidUrl() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_INSERTED_INVALID_URL)
    }

    override fun trackLoginAccessed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_ACCESSED)
    }

    override fun trackLoginAutofillCredentialsFilled() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_FILLED)
    }

    override fun trackLoginAutofillCredentialsUpdated() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_AUTOFILL_CREDENTIALS_UPDATED)
    }

    override fun trackLoginFailed(errorContext: String?, errorType: String?, errorDescription: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FAILED, errorContext, errorType, errorDescription)
    }

    override fun trackLoginForgotPasswordClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_FORGOT_PASSWORD_CLICKED)
    }

    override fun trackLoginMagicLinkExited() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_EXITED)
    }

    override fun trackLoginMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPENED)
    }

    override fun trackLoginMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED)
    }

    override fun trackLoginMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED)
    }

    override fun trackLoginSocial2faNeeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_2FA_NEEDED)
    }

    override fun trackLoginSocialSuccess() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_SUCCESS)
    }

    override fun trackMagicLinkFailed(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_FAILED, properties)
    }

    override fun trackMagicLinkRequested() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUESTED)
    }

    override fun trackMagicLinkRequestFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED)
    }

    override fun trackPasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_PASSWORD_FORM_VIEWED)
    }

    override fun trackSignupCanceled() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_CANCELED)
    }

    override fun trackSignupEmailButtonTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_BUTTON_TAPPED)
    }

    override fun trackSignupEmailToLogin() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_EMAIL_TO_LOGIN)
    }

    override fun trackSignupGoogleButtonTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_GOOGLE_BUTTON_TAPPED)
    }

    override fun trackSignupMagicLinkFailed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_FAILED)
    }

    override fun trackSignupMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_OPENED)
    }

    override fun trackSignupMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED)
    }

    override fun trackSignupMagicLinkSent() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_SENT)
    }

    override fun trackSignupMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_MAGIC_LINK_SUCCEEDED)
    }

    override fun trackSignupSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING)
    }

    override fun trackSignupSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_BUTTON_FAILURE)
    }

    override fun trackSignupSocialToLogin() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_SOCIAL_TO_LOGIN)
    }

    override fun trackSignupTermsOfServiceTapped() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNUP_TERMS_OF_SERVICE_TAPPED)
    }

    override fun trackSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING)
    }

    override fun trackSocialButtonClick() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_CLICK)
    }

    override fun trackSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_BUTTON_FAILURE)
    }

    override fun trackSocialConnectFailure() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_FAILURE)
    }

    override fun trackSocialConnectSuccess() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_CONNECT_SUCCESS)
    }

    override fun trackSocialErrorUnknownUser() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_ERROR_UNKNOWN_USER)
    }

    override fun trackSocialFailure(errorContext: String?, errorType: String?, errorDescription: String?) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_SOCIAL_FAILURE, errorContext, errorType, errorDescription)
    }

    override fun trackTwoFactorFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_TWO_FACTOR_FORM_VIEWED)
    }

    override fun trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_FORM_VIEWED)
    }

    override fun trackUrlHelpScreenViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_URL_HELP_SCREEN_VIEWED)
    }

    override fun trackUsernamePasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_USERNAME_PASSWORD_FORM_VIEWED)
    }

    override fun trackWpComBackgroundServiceUpdate(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE, properties)
    }

    override fun trackConnectedSiteInfoRequested(url: String?) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_REQUESTED,
                mapOf(AnalyticsTracker.KEY_URL to url))
    }

    override fun trackConnectedSiteInfoFailed(
        url: String?,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?
    ) {
        AnalyticsTracker.track(
                AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_FAILED,
                errorContext,
                errorType,
                errorDescription)
    }

    override fun trackConnectedSiteInfoSucceeded(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_CONNECTED_SITE_INFO_SUCCEEDED, properties)
    }
}
