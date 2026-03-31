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
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success))
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success))
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success))
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success))
        whenever(storeProductsCheck()).thenReturn(flowOf(Success))
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
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.internetCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when wpComConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wpComConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.wpComCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when storeConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.storeCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when storeOrdersCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.ordersCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
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
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success))
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
        whenever(storeProductsCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.productsCheckData }
            .distinctUntilChanged()
            .observeForever { stateEvents.add(it.connectivityCheckStatus) }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
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
    fun `when onContactSupportClicked is called, then trigger OpenSupportRequest event`() {
        // Given
        val events = mutableListOf<MultiLiveEvent.Event>()
        sut.event.observeForever { events.add(it) }

        // When
        sut.onContactSupportClicked()

        // Then
        assertThat(events).isEqualTo(listOf(OpenSupportRequest))
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
        sut.onViewTechnicalDetailsClicked(details)

        // THEN
        assertThat(sut.technicalDetailsToShow.value).isEqualTo(details)
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
        // GIVEN
        sut.onViewTechnicalDetailsClicked("some details")

        // WHEN
        sut.onTechnicalDetailsDismissed()

        // THEN
        assertThat(sut.technicalDetailsToShow.value).isNull()
    }
}
