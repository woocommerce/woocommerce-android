package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.JetpackStatus
import com.woocommerce.android.model.User
import com.woocommerce.android.model.UserRole
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.common.UserEligibilityFetcher
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken

@ExperimentalCoroutinesApi
class JetpackBenefitsViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val siteModelMock: SiteModel = mock {
        on { url }.doReturn("https://woocommerce.com/")
    }
    private val selectedSiteMock: SelectedSite = mock {
        on { get() }.doReturn(siteModelMock)
    }
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val fetchJetpackStatus: FetchJetpackStatus = mock()
    private val wpComAccessToken: AccessToken = mock()

    private val user: User = mock()

    private lateinit var sut: JetpackBenefitsViewModel

    @Before
    fun setup() {
        sut = JetpackBenefitsViewModel(
            savedState,
            selectedSiteMock,
            userEligibilityFetcher,
            analyticsTrackerWrapper,
            fetchJetpackStatus,
            wpComAccessToken
        )
    }

    @Test
    fun `given Jetpack CP login, when user starts installation, then StartJetpackActivationForJetpackCP event is triggered`() {
        // Given
        givenConnectionType(SiteConnectionType.JetpackConnectionPackage)

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.StartJetpackActivationForJetpackCP
        )
    }

    // This unit test is to handle the case where:
    // 1. Site initially doesn't have Jetpack,
    // 2. User logs in via REST API login,
    // 3. User returns to site and installs and connects Jetpack,
    // 4. User comes back to app and uses the Jetpack Benefits dialog to install Jetpack.
    @Test
    fun `given REST API login and Jetpack is already activated, when user starts installation, then StartJetpackActivationForApplicationPasswords event is triggered`() = testBlocking {
        // Given
        val jetpackStatus = JetpackStatus(
            isJetpackInstalled = true,
            isJetpackConnected = true,
            wpComEmail = null
        )
        givenConnectionType(SiteConnectionType.ApplicationPasswords)
        givenJetpackFetchResult(
            FetchJetpackStatus.JetpackStatusFetchResponse.Success(jetpackStatus)
        )

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.StartJetpackActivationForApplicationPasswords(
                selectedSiteMock.get().url,
                jetpackStatus
            )
        )
    }

    @Test
    fun `given REST API login and user role is not eligible, when user starts installation, then OpenJetpackEligibilityError event is triggered`() = testBlocking {
        // Given
        givenConnectionType(SiteConnectionType.ApplicationPasswords)
        givenJetpackFetchResult(FetchJetpackStatus.JetpackStatusFetchResponse.ConnectionForbidden)
        givenUserEligibility(user, UserRole.Editor)

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.OpenJetpackEligibilityError(
                user.username,
                user.roles.first().value
            )
        )
    }

    @Test
    fun `given REST API login and Jetpack is not installed while user role is eligible, when user starts installation, then StartJetpackActivationForApplicationPasswords event is triggered`() = testBlocking {
        // Given
        val jetpackStatus = JetpackStatus(
            isJetpackInstalled = false,
            isJetpackConnected = false,
            wpComEmail = null
        )
        givenConnectionType(SiteConnectionType.ApplicationPasswords)
        givenJetpackFetchResult(FetchJetpackStatus.JetpackStatusFetchResponse.Success(jetpackStatus))
        givenUserEligibility(user, UserRole.Administrator)

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.StartJetpackActivationForApplicationPasswords(
                selectedSiteMock.get().url,
                jetpackStatus
            )
        )
    }

    @Test
    fun `given REST API login and Jetpack is not installed while user role is not eligible, when user starts installation, then OpenJetpackEligibilityError event is triggered`() = testBlocking {
        // Given
        val jetpackStatus = JetpackStatus(
            isJetpackInstalled = false,
            isJetpackConnected = false,
            wpComEmail = null
        )
        givenConnectionType(SiteConnectionType.ApplicationPasswords)
        givenJetpackFetchResult(FetchJetpackStatus.JetpackStatusFetchResponse.Success(jetpackStatus))
        givenUserEligibility(user, UserRole.Editor)

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.OpenJetpackEligibilityError(
                user.username,
                user.roles.first().value
            )
        )
    }

    @Test
    fun `when user dismisses the dialog, then wpcom access token is cleared`() {
        // When
        sut.onDismiss()

        // Then
        verify(wpComAccessToken).set(null)
    }

    private fun givenConnectionType(connectionType: SiteConnectionType) {
        whenever(selectedSiteMock.connectionType).thenReturn(connectionType)
    }

    private fun givenJetpackFetchResult(
        jetpackStatusFetchResponse: FetchJetpackStatus.JetpackStatusFetchResponse
    ) = testBlocking {
        val result = Result.success(jetpackStatusFetchResponse)
        whenever(fetchJetpackStatus.invoke()).thenReturn(result)
    }

    private fun givenUserEligibility(user: User, role: UserRole) = testBlocking {
        whenever(user.username).thenReturn("username")
        whenever(user.roles).thenReturn(listOf(role))
        whenever(userEligibilityFetcher.fetchUserInfo()).thenReturn(Result.success(user))
    }
}
