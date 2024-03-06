package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.NotStarted
import com.woocommerce.android.ui.orders.connectivitytool.OrderConnectivityToolViewModel.ConnectivityTestStatus.Success
import com.woocommerce.android.ui.orders.connectivitytool.useCases.InternetConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreConnectionCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.StoreOrdersCheckUseCase
import com.woocommerce.android.ui.orders.connectivitytool.useCases.WordPressConnectionCheckUseCase
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
        whenever(internetConnectionCheck()).thenReturn(flowOf())
        whenever(wordPressConnectionCheck()).thenReturn(flowOf())
        whenever(storeConnectionCheck()).thenReturn(flowOf())
        whenever(storeOrdersCheck()).thenReturn(flowOf())
        sut = OrderConnectivityToolViewModel(
            internetConnectionCheck = internetConnectionCheck,
            wordPressConnectionCheck = wordPressConnectionCheck,
            storeConnectionCheck = storeConnectionCheck,
            storeOrdersCheck = storeOrdersCheck,
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when internetConnectionTest use case starts, then update ViewState as expected`() = testBlocking {
        // Given
        val stateEvents = mutableListOf<ConnectivityTestStatus>()
        whenever(internetConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.internetConnectionCheckStatus)
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
        whenever(wordPressConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.wordpressConnectionCheckStatus)
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
        whenever(storeConnectionCheck()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.storeConnectionCheckStatus)
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
        whenever(storeOrdersCheck()).thenReturn(flowOf(Success))
        sut.viewState.observeForever {
            stateEvents.add(it.storeOrdersCheckStatus)
        }

        // When
        sut.startConnectionTests()

        // Then
        assertThat(stateEvents).isEqualTo(listOf(NotStarted, Success))
    }
}
