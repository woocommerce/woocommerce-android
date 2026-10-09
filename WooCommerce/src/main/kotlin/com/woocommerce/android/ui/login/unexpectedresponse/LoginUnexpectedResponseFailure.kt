package com.woocommerce.android.ui.login.unexpectedresponse

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.rest.wpapi.Nonce.CookieNonceLoginStep

@Parcelize
data class LoginUnexpectedResponseFailure(
    val flow: Flow,
    val step: Step,
    val response: UnexpectedStoreResponse
) : Parcelable {
    enum class Flow(val trackingValue: String) {
        SITE_CREDENTIALS("site_credentials"),
        APP_PASSWORD("app_password"),
        STORE_PICKER("store_picker")
    }

    enum class Step(val trackingValue: String) {
        LOGIN_PAGE("login_page"),
        CREDENTIALS_SUBMISSION("credentials_submission"),
        DASHBOARD_VERIFICATION("dashboard_verification"),
        NONCE_RETRIEVAL("nonce_retrieval"),
        APP_PASSWORD_GENERATION("app_password_generation"),
        USER_ROLE_CHECK("user_role_check"),
        WOO_PLUGIN_CHECK("woo_plugin_check"),
        APP_PASSWORD_AUTHORIZATION_URL("app_password_authorization_url");

        companion object {
            fun of(step: CookieNonceLoginStep) = when (step) {
                CookieNonceLoginStep.LOGIN_PAGE -> LOGIN_PAGE
                CookieNonceLoginStep.CREDENTIALS_SUBMISSION -> CREDENTIALS_SUBMISSION
                CookieNonceLoginStep.DASHBOARD_VERIFICATION -> DASHBOARD_VERIFICATION
                CookieNonceLoginStep.NONCE_RETRIEVAL -> NONCE_RETRIEVAL
            }
        }
    }

    companion object {
        fun siteCredentials(step: CookieNonceLoginStep, response: UnexpectedStoreResponse) =
            LoginUnexpectedResponseFailure(flow = Flow.SITE_CREDENTIALS, step = Step.of(step), response = response)
    }
}
