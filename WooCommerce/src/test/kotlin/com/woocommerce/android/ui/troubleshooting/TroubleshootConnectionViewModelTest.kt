package com.woocommerce.android.ui.troubleshooting

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.troubleshooting.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.troubleshooting.TroubleshootConnectionViewModel.OpenAiSupportChat
import com.woocommerce.android.ui.troubleshooting.TroubleshootConnectionViewModel.OpenSupportRequest
import com.woocommerce.android.ui.troubleshooting.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.troubleshooting.useCases.WPComConnectionCheckUseCase
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class TroubleshootConnectionViewModelTest : BaseUnitTest() {
    private lateinit var sut: TroubleshootConnectionViewModel
    private lateinit var internetConnectionCheck: InternetConnectionCheckUseCase
    private lateinit var wpComConnectionCheck: WPComConnectionCheckUseCase
    private lateinit var storeConnectionCheck: StoreConnectionCheckUseCase
    private lateinit var storeOrdersCheck: StoreOrdersCheckUseCase
    private lateinit var storeProductsCheck: StoreProductsCheckUseCase
    private lateinit var selectedSite: SelectedSite
    private lateinit var featureFlagRepository: FeatureFlagRepository
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Before
    fun setUp() {
        internetConnectionCheck = mock()
        wpComConnectionCheck = mock()
        storeConnectionCheck = mock()
        storeOrdersCheck = mock()
        storeProductsCheck = mock()
        selectedSite = mock()
        featureFlagRepository = mock()
        analyticsTrackerWrapper = mock()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success()))
        whenever(storeProductsCheck()).thenReturn(flowOf(Success()))
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.Jetpack)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT)).thenReturn(false)
        createViewModel()
    }

    private fun createViewModel() {
        sut = TroubleshootConnectionViewModel(
            internetConnectionCheck = internetConnectionCheck,
            wpComConnectionCheck = wpComConnectionCheck,
            storeConnectionCheck = storeConnectionCheck,
            storeOrdersCheck = storeOrdersCheck,
            storeProductsCheck = storeProductsCheck,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            selectedSite = selectedSite,
            featureFlagRepository = featureFlagRepository,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when a check previously failed, then startConnectionChecks does not execute the next check`() = testBlocking {
        // Given
        val checks = listOf(
            ConnectivityCheckCardData(ConnectivityCheckType.INTERNET, Success()),
            ConnectivityCheckCardData(ConnectivityCheckType.WP_COM, Failure()),
            ConnectivityCheckCardData(ConnectivityCheckType.STORE, NotStarted)
        )
        val savedStateHandle = SavedStateHandle(mapOf("checksFlow" to checks))

        sut = TroubleshootConnectionViewModel(
            internetConnectionCheck = internetConnectionCheck,
            wpComConnectionCheck = wpComConnectionCheck,
            storeConnectionCheck = storeConnectionCheck,
            storeOrdersCheck = storeOrdersCheck,
            storeProductsCheck = storeProductsCheck,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            selectedSite = selectedSite,
            featureFlagRepository = featureFlagRepository,
            savedState = savedStateHandle
        )

        // When
        sut.startConnectionChecks()

        // Then
        verify(storeConnectionCheck, never()).invoke()
    }

    @Test
    fun `when internetConnectionCheck use case starts, then internet check status updates as expected`() = testBlocking {
        // Given
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        val checks = sut.viewState.value!!.checks
        val internetCheck = checks.first { it.type == ConnectivityCheckType.INTERNET }
        assertThat(internetCheck.status).isEqualTo(Success())
    }

    @Test
    fun `when wpComConnectionCheck use case starts, then wpCom check status updates as expected`() = testBlocking {
        // Given
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        val checks = sut.viewState.value!!.checks
        val wpComCheck = checks.first { it.type == ConnectivityCheckType.WP_COM }
        assertThat(wpComCheck.status).isEqualTo(Success())
    }

    @Test
    fun `when storeConnectionCheck use case starts, then store check status updates as expected`() = testBlocking {
        // Given
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        val checks = sut.viewState.value!!.checks
        val storeCheck = checks.first { it.type == ConnectivityCheckType.STORE }
        assertThat(storeCheck.status).isEqualTo(Success())
    }

    @Test
    fun `when storeOrdersCheck use case starts, then orders check status updates as expected`() = testBlocking {
        // Given
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success()))
        sut.viewState.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        val checks = sut.viewState.value!!.checks
        val ordersCheck = checks.first { it.type == ConnectivityCheckType.ORDERS }
        assertThat(ordersCheck.status).isEqualTo(Success())
    }

    @Test
    fun `when storeProductsCheck use case starts, then products check status updates as expected`() = testBlocking {
        // Given
        whenever(storeProductsCheck()).thenReturn(flowOf(Success()))
        sut.viewState.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        val checks = sut.viewState.value!!.checks
        val productsCheck = checks.first { it.type == ConnectivityCheckType.PRODUCTS }
        assertThat(productsCheck.status).isEqualTo(Success())
    }

    @Test
    fun `when all checks are finished, then isCheckFinished is true`() = testBlocking {
        // Given
        sut.isCheckFinished.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(sut.isCheckFinished.value).isTrue()
    }

    @Test
    fun `when one check fails, then isCheckFinished is true`() = testBlocking {
        // Given
        whenever(storeConnectionCheck()).thenReturn(flowOf(Failure()))
        createViewModel()
        sut.isCheckFinished.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(sut.isCheckFinished.value).isTrue()
    }

    @Test
    fun `when checks are still running, then isCheckFinished is false`() = testBlocking {
        // Given
        whenever(internetConnectionCheck()).thenReturn(flowOf(InProgress))
        createViewModel()
        sut.isCheckFinished.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(sut.isCheckFinished.value).isFalse()
    }

    @Test
    fun `when storeProductsCheck fails, then isCheckFinished is true`() = testBlocking {
        // Given
        whenever(storeProductsCheck()).thenReturn(flowOf(Failure()))
        createViewModel()
        sut.isCheckFinished.observeForever {}

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(sut.isCheckFinished.value).isTrue()
    }

    @Test
    fun `when onContactSupportClicked is called, then trigger OpenSupportRequest event with diagnostic log`() =
        testBlocking {
            // Given
            sut.viewState.observeForever {}
            sut.startConnectionChecks()
            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever { events.add(it) }

            // When
            sut.onContactSupportClicked()

            // Then
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(OpenSupportRequest::class.java)
            val log = (events.first() as OpenSupportRequest).diagnosticLog
            assertThat(log).contains("## 1. Internet Connection")
            assertThat(log).contains("Result: Success")
        }

    @Test
    fun `given app password site, when checks run, then WPCom check is skipped`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()

        // WHEN
        sut.startConnectionChecks()

        // THEN
        verify(wpComConnectionCheck, never()).invoke()
    }

    @Test
    fun `given AI support chat is available, when state loads, then AI support chat button is visible`() =
        testBlocking {
            // GIVEN
            stubAiSupportChatAvailable()
            createViewModel()
            sut.viewState.observeForever {}

            // THEN
            assertThat(sut.viewState.value?.shouldDisplayAiSupportChatButton).isTrue()
        }

    @Test
    fun `given AI support chat flag is enabled and site is not Jetpack-connected, when state loads, then button is hidden`() =
        testBlocking {
            // GIVEN
            stubAiSupportChatAvailability(isFeatureEnabled = true, isJetpackConnected = false)
            createViewModel()
            sut.viewState.observeForever {}

            // THEN
            assertThat(sut.viewState.value?.shouldDisplayAiSupportChatButton).isFalse()
        }

    @Test
    fun `given AI support chat flag is disabled and site is Jetpack-connected, when state loads, then button is hidden`() =
        testBlocking {
            // GIVEN
            stubAiSupportChatAvailability(isFeatureEnabled = false, isJetpackConnected = true)
            createViewModel()
            sut.viewState.observeForever {}

            // THEN
            assertThat(sut.viewState.value?.shouldDisplayAiSupportChatButton).isFalse()
        }

    @Test
    fun `given checks finished, when AI support chat clicked, then open AI support chat event is emitted`() =
        testBlocking {
            // GIVEN
            stubAiSupportChatAvailable()
            createViewModel()
            sut.viewState.observeForever {}
            sut.startConnectionChecks()
            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever { events.add(it) }

            // WHEN
            sut.onAiSupportChatClicked()

            // THEN
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(OpenAiSupportChat::class.java)
            assertThat((events.first() as OpenAiSupportChat).checks).isEqualTo(sut.viewState.value?.checks)
        }

    @Test
    fun `given checks are not finished, when AI support chat clicked, then no event is emitted`() =
        testBlocking {
            // GIVEN
            stubAiSupportChatAvailable()
            createViewModel()
            val events = mutableListOf<MultiLiveEvent.Event>()
            sut.event.observeForever { events.add(it) }

            // WHEN
            sut.onAiSupportChatClicked()

            // THEN
            assertThat(events).isEmpty()
        }

    @Test
    fun `given app password site, when all checks succeed, then isCheckFinished is true`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()
        sut.isCheckFinished.observeForever {}

        // WHEN
        sut.startConnectionChecks()

        // THEN
        assertThat(sut.isCheckFinished.value).isTrue()
    }

    private fun stubAiSupportChatAvailable() {
        stubAiSupportChatAvailability(isFeatureEnabled = true, isJetpackConnected = true)
    }

    private fun stubAiSupportChatAvailability(isFeatureEnabled: Boolean, isJetpackConnected: Boolean) {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.AI_SUPPORT_CHAT)).thenReturn(isFeatureEnabled)
        whenever(selectedSite.getIfExists()).thenReturn(
            SiteModel().apply {
                setIsJetpackConnected(isJetpackConnected)
            }
        )
    }

    @Test
    fun `given app password site, when all checks succeed, then shouldDisplaySummary is true`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()
        sut.viewState.observeForever {}

        // WHEN
        sut.startConnectionChecks()

        // THEN
        assertThat(sut.viewState.value?.shouldDisplaySummary).isTrue()
    }

    @Test
    fun `when onViewTechnicalDetailsClicked is called, then technicalDetailsToShow is updated`() = testBlocking {
        // GIVEN
        val details = "Operation: Site Connection\nError Type: TIMEOUT"

        // WHEN
        sut.technicalDetailsToShow.observeForTesting {
            sut.onViewTechnicalDetailsClicked(details)

            // THEN
            assertThat(sut.technicalDetailsToShow.value).isEqualTo(details)
        }
    }

    @Test
    fun `when onViewTechnicalDetailsClicked is called, then analytics event is tracked`() = testBlocking {
        // WHEN
        sut.onViewTechnicalDetailsClicked("some details")

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.CONNECTIVITY_TOOL_TECHNICAL_DETAILS_TAPPED)
    }

    @Test
    fun `when onTechnicalDetailsDismissed is called, then technicalDetailsToShow is cleared`() = testBlocking {
        sut.technicalDetailsToShow.observeForTesting {
            // GIVEN
            sut.onViewTechnicalDetailsClicked("some details")

            // WHEN
            sut.onTechnicalDetailsDismissed()

            // THEN
            assertThat(sut.technicalDetailsToShow.value).isNull()
        }
    }

    @Test
    fun `when all checks succeed, then diagnostic log contains all SUCCESS entries`() = testBlocking {
        // GIVEN
        sut.viewState.observeForever {}
        sut.startConnectionChecks()
        val events = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever { events.add(it) }

        // WHEN
        sut.onContactSupportClicked()

        // THEN
        val log = (events.first() as OpenSupportRequest).diagnosticLog
        assertThat(log).contains("## 1. Internet Connection")
        assertThat(log).contains("## 2. Connecting to WordPress.com Servers")
        assertThat(log).contains("## 3. Connecting to your site")
        assertThat(log).contains("## 4. Fetching your site orders")
        assertThat(log).contains("## 5. Fetching products in your store")
        assertThat(log!!.lines().filter { it == "Result: Success" }).hasSize(5)
    }

    @Test
    fun `when a check fails, then diagnostic log contains FAILED entry with error info`() = testBlocking {
        // GIVEN
        val failure = Failure(
            error = FailureType.TIMEOUT,
            technicalDetails = "Operation: Site Connection\nError Type: TIMEOUT"
        )
        whenever(storeConnectionCheck()).thenReturn(flowOf(failure))
        createViewModel()
        sut.viewState.observeForever {}
        sut.startConnectionChecks()
        val events = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever { events.add(it) }

        // WHEN
        sut.onContactSupportClicked()

        // THEN
        val log = (events.first() as OpenSupportRequest).diagnosticLog
        assertThat(log).contains("## 3. Connecting to your site")
        assertThat(log).contains("Result: TIMEOUT")
        assertThat(log).contains("Operation: Site Connection")
    }

    @Test
    fun `given app password site, when all checks succeed, then diagnostic log skips WPCom entry`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()
        sut.viewState.observeForever {}
        sut.startConnectionChecks()
        val events = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever { events.add(it) }

        // WHEN
        sut.onContactSupportClicked()

        // THEN
        val log = (events.first() as OpenSupportRequest).diagnosticLog
        assertThat(log).contains("## 1. Internet Connection")
        assertThat(log).doesNotContain("WordPress.com Servers")
        assertThat(log).contains("## 2. Connecting to your site")
        assertThat(log).contains("## 3. Fetching your site orders")
        assertThat(log).contains("## 4. Fetching products in your store")
    }

    @Test
    fun `when check fails early, then diagnostic log only contains completed checks`() = testBlocking {
        // GIVEN
        whenever(internetConnectionCheck()).thenReturn(flowOf(Failure()))
        createViewModel()
        sut.viewState.observeForever {}
        sut.startConnectionChecks()
        val events = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever { events.add(it) }

        // WHEN
        sut.onContactSupportClicked()

        // THEN
        val log = (events.first() as OpenSupportRequest).diagnosticLog
        assertThat(log).contains("## 1. Internet Connection")
        assertThat(log).contains("Result: Failed")
        assertThat(log).doesNotContain("WordPress.com Servers")
        assertThat(log).doesNotContain("Connecting to your site")
    }
}
