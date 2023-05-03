package com.woocommerce.android.ui.jetpack.benefits

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken

@ExperimentalCoroutinesApi
class JetpackBenefitsViewModelTest : BaseUnitTest() {
    private val savedState: SavedStateHandle = SavedStateHandle()
    private val siteModelMock: SiteModel = mock()
    private val selectedSiteMock: SelectedSite = mock {
        on { get() }.doReturn(siteModelMock)
    }
    private val userEligibilityFetcher: UserEligibilityFetcher = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val fetchJetpackStatus: FetchJetpackStatus = mock()
    private val wpComAccessToken: AccessToken = mock()

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
    fun `given Jetpack CP connection type, when user starts installation, then StartJetpackActivationForJetpackCP event is triggered`() {
        // Given
        whenever(selectedSiteMock.connectionType).thenReturn(SiteConnectionType.JetpackConnectionPackage)

        // When
        sut.onInstallClick()

        // Then
        assertThat(sut.event.value).isEqualTo(
            JetpackBenefitsViewModel.StartJetpackActivationForJetpackCP
        )
    }
}
