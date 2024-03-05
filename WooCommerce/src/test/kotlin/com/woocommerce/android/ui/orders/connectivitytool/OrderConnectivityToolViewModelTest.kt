package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.InProgress
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersTestUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionTestUseCase
import com.woocommerce.android.viewmodel.BaseUnitTest
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
    private lateinit var internetConnectionTest: InternetConnectionTestUseCase
    private lateinit var wordPressConnectionTest: WordPressConnectionTestUseCase
    private lateinit var storeConnectionTest: StoreConnectionTestUseCase
    private lateinit var storeOrdersTest: StoreOrdersTestUseCase

    @Before
    fun setUp() {
        internetConnectionTest = mock()
        wordPressConnectionTest = mock()
        storeConnectionTest = mock()
        storeOrdersTest = mock()
        whenever(internetConnectionTest()).thenReturn(flowOf())
        whenever(wordPressConnectionTest()).thenReturn(flowOf())
        whenever(storeConnectionTest()).thenReturn(flowOf())
        whenever(storeOrdersTest()).thenReturn(flowOf())
        sut = OrderConnectivityToolViewModel(
            internetConnectionTest = internetConnectionTest,
            wordPressConnectionTest = wordPressConnectionTest,
            storeConnectionTest = storeConnectionTest,
            storeOrdersTest = storeOrdersTest,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when internetConnectionTest use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(internetConnectionTest()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.internetConnectionTestStatus)
        }

        // When
        sut.startConnectionTests()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when wordPressConnectionTest use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(wordPressConnectionTest()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.wordpressConnectionTestStatus)
        }

        // When
        sut.startConnectionTests()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when storeConnectionTest use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(storeConnectionTest()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.storeConnectionTestStatus)
        }

        // When
        sut.startConnectionTests()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }

    @Test
    fun `when storeOrdersTest use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(storeOrdersTest()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.storeOrdersTestStatus)
        }

        // When
        sut.startConnectionTests()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }
}
