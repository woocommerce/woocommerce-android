package com.woocommerce.android.ui.orders.connectivitytool

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class OrderConnectivityToolViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderConnectivityToolViewModel

    @Before
    fun setUp() {
        sut = OrderConnectivityToolViewModel(
            internetConnectionTest = mock(),
            storeConnectionTest = mock(),
            wordPressConnectionTest = mock(),
            storeOrdersTest = mock(),
            savedState = SavedStateHandle()
        )
    }

    @Test
    fun `when internetConnectionTest use case starts, then update ViewState as expected`() = testBlocking {

    }

    @Test
    fun `when wordPressConnectionTest use case starts, then update ViewState as expected`() = testBlocking {

    }

    @Test
    fun `when storeConnectionTest use case starts, then update ViewState as expected`() = testBlocking {

    }

    @Test
    fun `when storeOrdersTest use case starts, then update ViewState as expected`() = testBlocking {

    }
}
