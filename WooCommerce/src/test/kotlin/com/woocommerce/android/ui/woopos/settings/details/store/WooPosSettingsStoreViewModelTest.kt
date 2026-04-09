package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class WooPosSettingsStoreViewModelTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val storeRepository: WooPosSettingsStoreRepository = mock()
    private val receiptRepository: WooPosSettingsReceiptRepository = mock()
    private val selectedSite: SelectedSite = mock()
    private val ciabSiteGateKeeper: CIABSiteGateKeeper = mock()
    private val analyticsTracker: WooPosAnalyticsTracker = mock()

    @Test
    fun `when screen resumed, then state is updated with loaded store and successful receipt`() = runTest {
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
        val viewModel = createViewModel()
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(storeInfo))
        assertThat(finalState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.Success(receiptInfo))
    }

    @Test
    fun `when screen resumed and receipt is not available, then state shows not supported receipt`() = runTest {
        // GIVEN
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "Test Store",
            address = "123 Test St"
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(storeInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onScreenResumed()
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(storeInfo))
        assertThat(finalState.receiptState).isEqualTo(WooPosSettingsStoreState.ReceiptState.NotSupported)
    }

    @Test
    fun `when screen resumed and receipt fails, then state shows error receipt`() = runTest {
        // GIVEN
        val storeInfo = WooPosSettingsStoreState.StoreInfo(
            storeName = "Test Store",
            address = "123 Test St"
        )
        whenever(storeRepository.getStoreInfo()).thenReturn(storeInfo)
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.Error)

        // WHEN
        val viewModel = createViewModel()
        viewModel.onScreenResumed()
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

        // WHEN
        val viewModel = createViewModel()
        advanceUntilIdle()

        // THEN
        val finalState = viewModel.state.value
        assertThat(finalState.storeInfoState).isEqualTo(WooPosSettingsStoreState.StoreState.Loaded(emptyStoreInfo))
    }

    @Test
    fun `when edit receipt clicked on non-CIAB site, then emits URL with wc-settings path`() = runTest {
        // GIVEN
        val site = SiteModel().apply { url = "https://mystore.com" }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(ciabSiteGateKeeper.isCurrentSiteCIAB()).thenReturn(false)
        whenever(storeRepository.getStoreInfo()).thenReturn(
            WooPosSettingsStoreState.StoreInfo(storeName = "", address = "")
        )
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        val results = mutableListOf<WooPosSettingsStoreViewModel.EditReceiptTarget>()
        val job = launch(coroutineTestRule.testDispatcher) {
            viewModel.openEditReceiptEvent.collect { results.add(it) }
        }
        viewModel.onEditReceiptClicked()
        advanceUntilIdle()

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results.first().url)
            .isEqualTo("https://mystore.com/wp-admin/admin.php?page=wc-settings&tab=point-of-sale")
        job.cancel()
    }

    @Test
    fun `when edit receipt clicked on CIAB site, then emits URL with next-admin path`() = runTest {
        // GIVEN
        val site = SiteModel().apply { url = "https://my.commerce-garden.com" }
        whenever(selectedSite.get()).thenReturn(site)
        whenever(ciabSiteGateKeeper.isCurrentSiteCIAB()).thenReturn(true)
        whenever(storeRepository.getStoreInfo()).thenReturn(
            WooPosSettingsStoreState.StoreInfo(storeName = "", address = "")
        )
        whenever(receiptRepository.getReceiptInfo()).thenReturn(WooPosReceiptDataResult.NotAvailable)

        val viewModel = createViewModel()
        advanceUntilIdle()

        // WHEN
        val results = mutableListOf<WooPosSettingsStoreViewModel.EditReceiptTarget>()
        val job = launch(coroutineTestRule.testDispatcher) {
            viewModel.openEditReceiptEvent.collect { results.add(it) }
        }
        viewModel.onEditReceiptClicked()
        advanceUntilIdle()

        // THEN
        assertThat(results).hasSize(1)
        assertThat(results.first().url).contains("next-admin")
        assertThat(results.first().url).contains("woocommerce%2Fsettings%2Fpayments%2Fpos")
        job.cancel()
    }

    private fun createViewModel() = WooPosSettingsStoreViewModel(
        storeRepository = storeRepository,
        receiptRepository = receiptRepository,
        selectedSite = selectedSite,
        ciabSiteGateKeeper = ciabSiteGateKeeper,
        analyticsTracker = analyticsTracker,
    )
}
