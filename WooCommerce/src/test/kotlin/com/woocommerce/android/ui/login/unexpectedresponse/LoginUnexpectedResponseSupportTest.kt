package com.woocommerce.android.ui.login.unexpectedresponse

import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.UnexpectedStoreResponse
import org.wordpress.android.fluxc.network.UnexpectedStoreResponseKind

class LoginUnexpectedResponseSupportTest {
    private val featureFlagRepository: FeatureFlagRepository = mock()
    private val support = LoginUnexpectedResponseSupport(featureFlagRepository)

    @Test
    fun `given the AI chat is available, when contacting support, then open the AI chat`() {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT)).thenReturn(true)

        // WHEN
        val event = support.contactSupportEvent(FAILURE, SITE_URL, HelpOrigin.LOGIN_USERNAME_PASSWORD)

        // THEN
        assertThat(event).isEqualTo(LoginUnexpectedResponseSupport.OpenAiSupportChat(FAILURE, SITE_URL))
    }

    @Test
    fun `given the AI chat is not available, when contacting support, then open the support form`() {
        // GIVEN
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT)).thenReturn(false)

        // WHEN
        val event = support.contactSupportEvent(FAILURE, SITE_URL, HelpOrigin.LOGIN_USERNAME_PASSWORD)

        // THEN
        assertThat(event).isEqualTo(
            LoginUnexpectedResponseSupport.OpenSupportForm(FAILURE, SITE_URL, HelpOrigin.LOGIN_USERNAME_PASSWORD)
        )
    }

    @Test
    fun `when describing the failure for support, then include the login step and the response details`() {
        // THEN
        assertThat(FAILURE.supportDetails).isEqualTo(
            "Unexpected store response during login: login_flow=site_credentials, step=nonce_retrieval, " +
                "kind=unacceptable_status_code, status=400, content_type=text/html, " +
                "request=GET /wp-admin/admin-ajax.php, excerpt=0"
        )
    }

    private companion object {
        const val SITE_URL = "https://example.com"
        val FAILURE = LoginUnexpectedResponseFailure(
            flow = LoginUnexpectedResponseFailure.Flow.SITE_CREDENTIALS,
            step = LoginUnexpectedResponseFailure.Step.NONCE_RETRIEVAL,
            response = UnexpectedStoreResponse(
                kind = UnexpectedStoreResponseKind.UNACCEPTABLE_STATUS_CODE,
                statusCode = 400,
                contentType = "text/html",
                requestType = "GET /wp-admin/admin-ajax.php",
                excerpt = "0"
            )
        )
    }
}
