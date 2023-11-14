package com.woocommerce.android.ui.login

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginAnalyticsListener.CreatedAccountSource
import java.util.HashMap
import javax.inject.Singleton

@Singleton
class LoginAnalyticsTracker(
    val accountStore: AccountStore,
    val siteStore: SiteStore,
    val unifiedLoginTracker: UnifiedLoginTracker
) : LoginAnalyticsListener {
    override fun trackAnalyticsSignIn(isWpcomLogin: Boolean) {
        AnalyticsTracker.refreshMetadata(accountStore.account?.userName)
        val properties = HashMap<String, Boolean>()
        properties["dotcom_user"] = isWpcomLogin
        AnalyticsTracker.track(AnalyticsEvent.SIGNED_IN, properties)
        if (!isWpcomLogin) {
            AnalyticsTracker.track(AnalyticsEvent.ADDED_SELF_HOSTED_SITE)
        }
    }

    override fun trackCreatedAccount(username: String?, email: String?, source: CreatedAccountSource) {
        // TODO: Account creation
    }

    override fun trackEmailFormViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_EMAIL_FORM_VIEWED)
        unifiedLoginTracker.track(step = Step.ENTER_EMAIL_ADDRESS)
    }

    override fun trackInsertedInvalidUrl() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_INSERTED_INVALID_URL)
    }

    override fun trackLoginAccessed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_ACCESSED)
    }

    override fun trackLoginAutofillCredentialsFilled() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_AUTOFILL_CREDENTIALS_FILLED)
    }

    override fun trackLoginAutofillCredentialsUpdated() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_AUTOFILL_CREDENTIALS_UPDATED)
    }

    override fun trackLoginFailed(errorContext: String?, errorType: String?, errorDescription: String?) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_FAILED, errorContext, errorType, errorDescription)
    }

    override fun trackLoginForgotPasswordClicked() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_FORGOT_PASSWORD_CLICKED)
        unifiedLoginTracker.trackClick(Click.FORGOTTEN_PASSWORD)
    }

    override fun trackLoginMagicLinkExited() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_EXITED)
    }

    override fun trackLoginMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_OPENED)
    }

    override fun trackLoginMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED)
        unifiedLoginTracker.track(step = Step.EMAIL_OPENED)
        unifiedLoginTracker.trackClick(Click.OPEN_EMAIL_CLIENT)
    }

    override fun trackLoginMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_SUCCEEDED)
    }

    override fun trackLoginSocial2faNeeded() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_2FA_NEEDED)
    }

    override fun trackLoginSocialSuccess() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_SUCCESS)
    }

    override fun trackMagicLinkFailed(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_FAILED, properties)
    }

    override fun trackMagicLinkRequested() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_REQUESTED)
    }

    override fun trackMagicLinkRequestFormViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_MAGIC_LINK_REQUEST_FORM_VIEWED)
        unifiedLoginTracker.track(Flow.LOGIN_MAGIC_LINK, Step.START)
    }

    override fun trackPasswordFormViewed(isSocialChallenge: Boolean) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_PASSWORD_FORM_VIEWED)
        unifiedLoginTracker.track(Flow.LOGIN_PASSWORD, Step.START)
    }

    override fun trackSignupCanceled() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_CANCELED)
    }

    override fun trackSignupEmailButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_EMAIL_BUTTON_TAPPED)
    }

    override fun trackSignupEmailToLogin() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_EMAIL_TO_LOGIN)
    }

    override fun trackSignupGoogleButtonTapped() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_GOOGLE_BUTTON_TAPPED)
    }

    override fun trackSignupMagicLinkFailed() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_MAGIC_LINK_FAILED)
    }

    override fun trackSignupMagicLinkOpened() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_MAGIC_LINK_OPENED)
    }

    override fun trackSignupMagicLinkOpenEmailClientClicked() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_MAGIC_LINK_OPEN_EMAIL_CLIENT_CLICKED)
    }

    override fun trackSignupMagicLinkSent() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_MAGIC_LINK_SENT)
    }

    override fun trackSignupMagicLinkSucceeded() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_MAGIC_LINK_SUCCEEDED)
    }

    override fun trackSignupSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_SOCIAL_ACCOUNTS_NEED_CONNECTING)
    }

    override fun trackSignupSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_SOCIAL_BUTTON_FAILURE)
    }

    override fun trackSignupSocialToLogin() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_SOCIAL_TO_LOGIN)
    }

    override fun trackSignupTermsOfServiceTapped() {
        AnalyticsTracker.track(AnalyticsEvent.SIGNUP_TERMS_OF_SERVICE_TAPPED)
    }

    override fun trackSocialAccountsNeedConnecting() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_ACCOUNTS_NEED_CONNECTING)
    }

    override fun trackSocialButtonClick() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_BUTTON_CLICK)
        unifiedLoginTracker.trackClick(Click.LOGIN_WITH_GOOGLE)
    }

    override fun trackSocialButtonFailure() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_BUTTON_FAILURE)
    }

    override fun trackSocialConnectFailure() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_CONNECT_FAILURE)
    }

    override fun trackSocialConnectSuccess() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_CONNECT_SUCCESS)
    }

    override fun trackSocialErrorUnknownUser() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_ERROR_UNKNOWN_USER)
    }

    override fun trackSocialFailure(errorContext: String?, errorType: String?, errorDescription: String?) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SOCIAL_FAILURE, errorContext, errorType, errorDescription)
    }

    override fun trackTwoFactorFormViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_TWO_FACTOR_FORM_VIEWED)
    }

    override fun trackUrlFormViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_URL_FORM_VIEWED)
        unifiedLoginTracker.track(Flow.LOGIN_SITE_ADDRESS, Step.START)
    }

    override fun trackUrlHelpScreenViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_URL_HELP_SCREEN_VIEWED)
    }

    override fun trackUsernamePasswordFormViewed() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_USERNAME_PASSWORD_FORM_VIEWED)
        unifiedLoginTracker.track(Flow.LOGIN_STORE_CREDS, Step.USERNAME_PASSWORD)
    }

    override fun trackWpComBackgroundServiceUpdate(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_WPCOM_BACKGROUND_SERVICE_UPDATE, properties)
    }

    override fun trackConnectedSiteInfoRequested(url: String?) {
        AnalyticsTracker.track(
            AnalyticsEvent.LOGIN_SITE_ADDRESS_SITE_INFO_REQUESTED,
            mapOf(AnalyticsTracker.KEY_URL to url)
        )
    }

    override fun trackConnectedSiteInfoFailed(
        url: String?,
        errorContext: String?,
        errorType: String?,
        errorDescription: String?
    ) {
        AnalyticsTracker.track(
            AnalyticsEvent.LOGIN_SITE_ADDRESS_SITE_INFO_FAILED,
            errorContext,
            errorType,
            errorDescription
        )
    }

    override fun trackConnectedSiteInfoSucceeded(properties: Map<String, *>) {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SITE_ADDRESS_SITE_INFO_SUCCEEDED, properties)
    }

    override fun trackSignupMagicLinkOpenEmailClientViewed() {
        TODO("Not yet implemented")
    }

    override fun trackLoginMagicLinkOpenEmailClientViewed() {
        AnalyticsTracker.track(LOGIN_MAGIC_LINK_OPEN_EMAIL_CLIENT_VIEWED)
        unifiedLoginTracker.track(step = Step.MAGIC_LINK_REQUESTED)
    }

    override fun trackSocialButtonStart() {
        unifiedLoginTracker.track(Flow.GOOGLE_LOGIN, Step.START)
    }

    override fun trackFailure(message: String?) {
        unifiedLoginTracker.trackFailure(message)
    }

    override fun trackSendCodeWithTextClicked() {
        unifiedLoginTracker.trackClick(Click.SEND_CODE_WITH_TEXT)
    }

    override fun trackSubmit2faCodeClicked() {
        unifiedLoginTracker.trackClick(Click.SUBMIT_2FA_CODE)
    }

    override fun trackSubmitClicked() {
        unifiedLoginTracker.trackClick(Click.SUBMIT)
    }

    override fun trackRequestMagicLinkClick() {
        unifiedLoginTracker.trackClick(Click.REQUEST_MAGIC_LINK)
    }

    override fun trackLoginWithPasswordClick() {
        unifiedLoginTracker.trackClick(Click.LOGIN_WITH_PASSWORD)
    }

    override fun trackShowHelpClick() {
        unifiedLoginTracker.trackClick(Click.SHOW_HELP)
        unifiedLoginTracker.track(step = Step.HELP)
    }

    override fun trackDismissDialog() {
        unifiedLoginTracker.trackClick(Click.DISMISS)
    }

    override fun trackSelectEmailField() {
        unifiedLoginTracker.trackClick(Click.SELECT_EMAIL_FIELD)
    }

    override fun trackPickEmailFromHint() {
        unifiedLoginTracker.trackClick(Click.PICK_EMAIL_FROM_HINT)
    }

    override fun trackShowEmailHints() {
        unifiedLoginTracker.track(step = Step.SHOW_EMAIL_HINTS)
    }

    override fun emailFormScreenResumed() {
        unifiedLoginTracker.setStep(Step.ENTER_EMAIL_ADDRESS)
    }

    override fun trackSocialSignupConfirmationViewed() {
        TODO("Not yet implemented")
    }

    override fun trackCreateAccountClick() {
        TODO("Not yet implemented")
    }

    override fun emailPasswordFormScreenResumed() {
        unifiedLoginTracker.setStep(Step.START)
    }

    override fun siteAddressFormScreenResumed() {
        unifiedLoginTracker.setStep(Step.START)
    }

    override fun magicLinkRequestScreenResumed() {
        unifiedLoginTracker.setStep(Step.START)
    }

    override fun magicLinkSentScreenResumed() {
        unifiedLoginTracker.setStep(Step.MAGIC_LINK_REQUESTED)
    }

    override fun usernamePasswordScreenResumed() {
        unifiedLoginTracker.setStep(Step.USERNAME_PASSWORD)
    }

    override fun trackLogin2faNeeded() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_2FA_NEEDED)
    }

    override fun trackLoginSecurityKeyFailure() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SECURITY_KEY_FAILURE)
    }

    override fun trackLoginSecurityKeySuccess() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_SECURITY_KEY_SUCCESS)
    }

    override fun trackUseSecurityKeyClicked() {
        AnalyticsTracker.track(AnalyticsEvent.LOGIN_USE_SECURITY_KEY_CLICKED)
    }
}
