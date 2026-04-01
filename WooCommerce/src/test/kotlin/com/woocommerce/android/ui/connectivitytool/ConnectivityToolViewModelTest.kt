package com.woocommerce.android.ui.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.connectivitytool.ConnectivityToolViewModel.OpenSupportRequest
import com.woocommerce.android.ui.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.StoreProductsCheckUseCase
import com.woocommerce.android.ui.connectivitytool.useCases.WPComConnectionCheckUseCase
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

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectivityToolViewModelTest : BaseUnitTest() {
    private lateinit var sut: ConnectivityToolViewModel
    private lateinit var internetConnectionCheck: InternetConnectionCheckUseCase
    private lateinit var wpComConnectionCheck: WPComConnectionCheckUseCase
    private lateinit var storeConnectionCheck: StoreConnectionCheckUseCase
    private lateinit var storeOrdersCheck: StoreOrdersCheckUseCase
    private lateinit var storeProductsCheck: StoreProductsCheckUseCase
    private lateinit var selectedSite: SelectedSite
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Before
    fun setUp() {
        internetConnectionCheck = mock()
        wpComConnectionCheck = mock()
        storeConnectionCheck = mock()
        storeOrdersCheck = mock()
        storeProductsCheck = mock()
        selectedSite = mock()
        analyticsTrackerWrapper = mock()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success()))
        whenever(storeProductsCheck()).thenReturn(flowOf(Success()))
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.Jetpack)
        createViewModel()
    }

    private fun createViewModel() {
        sut = ConnectivityToolViewModel(
            internetConnectionCheck = internetConnectionCheck,
            wpComConnectionCheck = wpComConnectionCheck,
            storeConnectionCheck = storeConnectionCheck,
            storeOrdersCheck = storeOrdersCheck,
            storeProductsCheck = storeProductsCheck,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            selectedSite = selectedSite,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when internetConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState
            .map { it.internetCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success()))
    }

    @Test
    fun `when wpComConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState
            .map { it.wpComCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success()))
    }

    @Test
    fun `when storeConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success()))
        sut.viewState
            .map { it.storeCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success()))
    }

    @Test
    fun `when storeOrdersCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success()))
        sut.viewState
            .map { it.ordersCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success()))
    }

    @Test
    fun `when all checks are finished, then isCheckFinished is true`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<Boolean>()
        sut.isCheckFinished.observeForever {
            stateEvents.add(it)
        }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(false, false, false, false, false, true))
    }

    @Test
    fun `when one check fails, then isCheckFinished is true`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<Boolean>()
        whenever(storeConnectionCheck()).thenReturn(flowOf(Failure()))
        sut.isCheckFinished.observeForever {
            stateEvents.add(it)
        }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(false, false, false, true))
    }

    @Test
    fun `when checks are still running, then isCheckFinished is false`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<Boolean>()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success()))
        whenever(wpComConnectionCheck()).thenReturn(flowOf(InProgress))
        sut.isCheckFinished.observeForever {
            stateEvents.add(it)
        }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(false, false))
    }

    @Test
    fun `when storeProductsCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(storeProductsCheck()).thenReturn(flowOf(Success()))
        sut.viewState
            .map { it.productsCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success()))
    }

    @Test
    fun `when storeProductsCheck fails, then isCheckFinished is true`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<Boolean>()
        whenever(storeProductsCheck()).thenReturn(flowOf(Failure()))
        sut.isCheckFinished.observeForever { stateEvents.add(it) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(false, false, false, false, false, true))
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
            assertThat(log).contains("Connectivity Test Log")
            assertThat(log).contains("Internet Connection: SUCCESS")
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
    fun `given app password site, when all checks succeed, then isCheckFinished is true`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()
        val stateEvents = mutableListOf<Boolean>()
        sut.isCheckFinished.observeForever { stateEvents.add(it) }

        // WHEN
        sut.startConnectionChecks()

        // THEN
        assertThat(stateEvents.last()).isTrue()
    }

    @Test
    fun `given app password site, when all checks succeed, then shouldDisplaySummary is true`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()
        var latestState: ConnectivityToolViewModel.ViewState? = null
        sut.viewState.observeForever { latestState = it }

        // WHEN
        sut.startConnectionChecks()

        // THEN
        assertThat(latestState?.shouldDisplaySummary).isTrue()
    }

    @Test
    fun `given app password site, when viewState is observed, then isWPComCheckVisible is false`() = testBlocking {
        // GIVEN
        whenever(selectedSite.connectionType).thenReturn(SiteConnectionType.ApplicationPasswords)
        createViewModel()

        // WHEN
        var latestState: ConnectivityToolViewModel.ViewState? = null
        sut.viewState.observeForever { latestState = it }

        // THEN
        assertThat(latestState?.isWPComCheckVisible).isFalse()
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
        assertThat(log).contains("1. Internet Connection: SUCCESS")
        assertThat(log).contains("2. WordPress.com Servers: SUCCESS")
        assertThat(log).contains("3. Site Connection: SUCCESS")
        assertThat(log).contains("4. Fetch Orders: SUCCESS")
        assertThat(log).contains("5. Fetch Products: SUCCESS")
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
        assertThat(log).contains("3. Site Connection: FAILED")
        assertThat(log).contains("Error: TIMEOUT")
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
        assertThat(log).contains("Internet Connection: SUCCESS")
        assertThat(log).doesNotContain("WordPress.com Servers")
        assertThat(log).contains("Site Connection: SUCCESS")
        assertThat(log).contains("Fetch Orders: SUCCESS")
        assertThat(log).contains("Fetch Products: SUCCESS")
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
        assertThat(log).contains("1. Internet Connection: FAILED")
        assertThat(log).doesNotContain("WordPress.com Servers")
        assertThat(log).doesNotContain("Site Connection")
    }
}
