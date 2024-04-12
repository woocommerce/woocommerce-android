package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.distinctUntilChanged
import androidx.lifecycle.map
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Failure
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.ConnectivityCheckStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.OpenSupportRequest
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class OrderConnectivityToolViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderConnectivityToolViewModel
    private lateinit var internetConnectionCheck: InternetConnectionCheckUseCase
    private lateinit var wordPressConnectionCheck: WordPressConnectionCheckUseCase
    private lateinit var storeConnectionCheck: StoreConnectionCheckUseCase
    private lateinit var storeOrdersCheck: StoreOrdersCheckUseCase

    @Before
    fun setUp() {
        internetConnectionCheck = mock()
        wordPressConnectionCheck = mock()
        storeConnectionCheck = mock()
        storeOrdersCheck = mock()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success))
        whenever(wordPressConnectionCheck()).thenReturn(flowOf(Success))
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success))
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success))
        sut = OrderConnectivityToolViewModel(
            internetConnectionCheck = internetConnectionCheck,
            wordPressConnectionCheck = wordPressConnectionCheck,
            storeConnectionCheck = storeConnectionCheck,
            storeOrdersCheck = storeOrdersCheck,
            analyticsTrackerWrapper = mock(),
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
    fun `when wordPressConnectionCheck use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityCheckStatus>()
        whenever(wordPressConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState
            .map { it.wordPressCheckData }
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
        assertThat(stateEvents).isEqualTo(listOf(false, false, false, false, true))
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
        whenever(wordPressConnectionCheck()).thenReturn(flowOf(InProgress))
        sut.isCheckFinished.observeForever {
            stateEvents.add(it)
        }

        // When
        sut.startConnectionChecks()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(false, false))
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
}
