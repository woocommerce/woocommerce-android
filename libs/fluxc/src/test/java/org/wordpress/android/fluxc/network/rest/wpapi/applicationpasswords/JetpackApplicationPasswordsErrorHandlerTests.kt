package org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords

import com.android.volley.NetworkResponse
import com.android.volley.VolleyError
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.utils.AppLogWrapper
import java.util.Optional

class JetpackApplicationPasswordsErrorHandlerTests {
    private val jetpackApplicationPasswordsSupport: JetpackApplicationPasswordsSupport = mock {
        on { supportsAppPasswords(any()) } doReturn true
    }
    private val appLogWrapper: AppLogWrapper = mock()
    private val listener: ApplicationPasswordsListener = mock()

    private val errorHandler: JetpackApplicationPasswordsErrorHandler = JetpackApplicationPasswordsErrorHandler(
        jetpackApplicationPasswordsSupport = jetpackApplicationPasswordsSupport,
        appLogWrapper = appLogWrapper,
        listener = Optional.of(listener)
    )

    private val testSite = SiteModel().apply {
        siteId = 123L
    }

    @Test
    fun `given HTTP 401 error, when handling error, then flag site as unsupported`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(401, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError)

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }

    @Test
    fun `given HTTP 403 error, when handling error, then flag site as unsupported`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(403, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError)

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }

    @Test
    fun `given HTTP 429 error, when handling error, then flag site as unsupported`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(429, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError)

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }

    @Test
    fun `given incorrect_password error code, when handling error, then flag site as unsupported`() {
        // Given
        val baseError = BaseNetworkError(mock<VolleyError>())
        val networkError = WPAPINetworkError(baseError, "incorrect_password")

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }

    @Test
    fun `given application_passwords_disabled_for_user error code, when handling error, then flag site as unsupported`() {
        // Given
        val baseError = BaseNetworkError(mock<VolleyError>())
        val networkError = WPAPINetworkError(baseError, "application_passwords_disabled_for_user")

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }

    @Test
    fun `given generic error, when handling error once, then do not flag site as unsupported`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(500, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError, "generic_error")

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport, never()).flagAsUnsupported(testSite)
    }

    @Test
    fun `given generic error, when handling error 10 times, then flag site as unsupported`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(500, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError, "generic_error")

        // When
        repeat(10) {
            errorHandler.handleError(testSite, networkError)
        }

        // Then
        verify(jetpackApplicationPasswordsSupport, times(1)).flagAsUnsupported(testSite)
    }

    @Test
    fun `given app passwords generation error, when handling error, then notify listener with correct flow`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(500, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(
            baseError,
            ApplicationPasswordsNetwork.APP_PASSWORDS_GENERATION_FAILURE_ERROR_CODE_PREFIX +
                "application_passwords_disabled_for_user"
        )

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(listener).onJetpackSiteFlaggedAsUnsupported(
            JetpackSiteFlaggedAsUnsupported(
                scenario = JetpackSiteFlaggedAsUnsupported.Scenario.APP_PASSWORD_GENERATION,
                cause = JetpackSiteFlaggedAsUnsupported.Cause.MAJOR_ERROR,
                apiErrorCode = "application_passwords_disabled_for_user",
                httpStatusCode = 500,
            )
        )
    }

    @Test
    fun `given non app passwords generation error, when handling error, then notify listener with correct flow`() {
        // Given
        val volleyError = VolleyError(NetworkResponse(401, null, true, 0, emptyList()))
        val baseError = BaseNetworkError(volleyError)
        val networkError = WPAPINetworkError(baseError, "incorrect_password")

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(listener).onJetpackSiteFlaggedAsUnsupported(
            JetpackSiteFlaggedAsUnsupported(
                scenario = JetpackSiteFlaggedAsUnsupported.Scenario.API_REQUEST,
                cause = JetpackSiteFlaggedAsUnsupported.Cause.MAJOR_ERROR,
                apiErrorCode = "incorrect_password",
                httpStatusCode = 401
            )
        )
    }

    @Test
    fun `given KeyStore error, when handling error, then flag site as unsupported`() {
        // Given
        val baseError = BaseNetworkError(mock<VolleyError>())
        val networkError = WPAPINetworkError(
            baseError,
            ApplicationPasswordsStore.APPLICATION_PASSWORDS_KEYSTORE_ENCRYPTION_ERROR
        )

        // When
        errorHandler.handleError(testSite, networkError)

        // Then
        verify(jetpackApplicationPasswordsSupport).flagAsUnsupported(testSite)
    }
}
