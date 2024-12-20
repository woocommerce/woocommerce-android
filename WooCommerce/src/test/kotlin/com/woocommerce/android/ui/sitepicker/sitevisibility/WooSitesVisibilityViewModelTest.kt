package com.woocommerce.android.ui.sitepicker.sitevisibility

import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.ui.sitepicker.SitePickerTestUtils
import com.woocommerce.android.ui.sitepicker.sitevisibility.WooSitesVisibilityViewModel.WooStoreUi
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class WooSitesVisibilityViewModelTest : BaseUnitTest() {
    companion object {
        private val DEFAULT_STORES = SitePickerTestUtils.generateStores().map {
            it.apply {
                hasWooCommerce = true
                name = "name $siteId"
                url = "www.$siteId.com"
            }
        }
        private val WOO_STORE_DEFAULT_UI = DEFAULT_STORES.first().let {
            WooStoreUi(
                siteName = it.name,
                siteUrl = it.url,
                siteId = it.siteId,
                isSelected = true
            )
        }
    }

    private val sitePickerRepository: SitePickerRepository = mock {
        onBlocking { getSites() } doReturn DEFAULT_STORES
    }
    private lateinit var viewModel: WooSitesVisibilityViewModel

    @Before
    fun setUp() {
        viewModel = WooSitesVisibilityViewModel(
            sitePickerRepository = sitePickerRepository,
            savedStateHandle = mock()
        )
    }

    @Test
    fun `given all sites are selected, when selected sites change, then enable save button`() =
        testBlocking {
            viewModel.onSiteSelected(WOO_STORE_DEFAULT_UI)

            // Assert
            val updatedState = viewModel.viewState.getOrAwaitValue()
            assert(updatedState?.wooStores?.first()?.isSelected == false)
            assert(updatedState?.isSaveButtonEnabled == true)
        }

    @Test
    fun `given all sites are selected, when selecting unselecting same site, then save button is disabled`() =
        testBlocking {
            viewModel.onSiteSelected(WOO_STORE_DEFAULT_UI)
            viewModel.onSiteSelected(WOO_STORE_DEFAULT_UI)

            // Assert
            val updatedState = viewModel.viewState.getOrAwaitValue()
            assert(updatedState?.wooStores?.first()?.isSelected == true)
            assert(updatedState?.isSaveButtonEnabled == false)
        }
}
