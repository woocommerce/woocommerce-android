package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class WooPosSettingsStoreViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val storeRepository: WooPosSettingsStoreRepository = mock()
    private val receiptRepository: WooPosSettingsReceiptRepository = mock()

    @Test
    fun `given init, when repositories return data, then state is updated with loaded store and successful receipt`() = runTest {
        // GIVEN
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "Test Store",
            address = "123 Test St"
        )
        val receiptInfo = WooPosSettingsStoreState.ReceiptInfo(
            storeName = "Test Store",
            address = "123 Test St",
            phone = "+1234567890",
            email = "test@example.com",
            refundPolicy = "30 day returns"
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(storeInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.Success(receiptInfo))

        // WHEN
        val viewModel = WooPosSettingsStoreViewModel(storeRepository, receiptRepository)
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(storeInfo))
        assertThat(finalState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.Success(receiptInfo))
    }

    @Test
    fun `given init, when store loads successfully but receipt is not available, then state shows loaded store and not supported receipt`() = runTest {
        // GIVEN
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "Test Store",
            address = "123 Test St"
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(storeInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

        // WHEN
        val viewModel = WooPosSettingsStoreViewModel(storeRepository, receiptRepository)
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(storeInfo))
        assertThat(finalState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.NotSupported)
    }

    @Test
    fun `given init, when store loads successfully but receipt fails, then state shows loaded store and error receipt`() = runTest {
        // GIVEN
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "Test Store",
            address = "123 Test St"
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(storeInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.Error)

        // WHEN
        val viewModel = WooPosSettingsStoreViewModel(storeRepository, receiptRepository)
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(storeInfo))
        assertThat(finalState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.Error)
    }

    @Test
    fun `when default state is created, then initial state shows loading for both store and receipt`() {
        // GIVEN & WHEN
        val initialState = WooPosSettingsStoreState()

        // THEN
        assertThat(initialState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loading)
        assertThat(initialState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.Loading)
    }

    @Test
    fun `given store repository returns empty data, when init, then state is updated with empty store info`() = runTest {
        // GIVEN
        val emptyStoreInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "",
            address = ""
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(emptyStoreInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

        // WHEN
        val viewModel = WooPosSettingsStoreViewModel(storeRepository, receiptRepository)
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(emptyStoreInfo))
    }
}
