package com.woocommerce.android.ui.products

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WCProductStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class ProductFilterListViewModelTest : BaseUnitTest() {
    private lateinit var resourceProvider: ResourceProvider
    private lateinit var productCategoriesRepository: ProductCategoriesRepository
    private lateinit var networkStatus: NetworkStatus
    private lateinit var productRestrictions: ProductFilterProductRestrictions
    private lateinit var productFilterListViewModel: ProductFilterListViewModel

    @Before
    fun setup() {
        resourceProvider = mock()
        productCategoriesRepository = mock()
        networkStatus = mock()
        productRestrictions = mock()
        productFilterListViewModel = ProductFilterListViewModel(
            savedState = ProductFilterListFragmentArgs(
                selectedStockStatus = "instock",
                selectedProductStatus = "published",
                selectedProductType = "any",
                selectedProductCategoryId = "1",
                selectedProductCategoryName = "any",
            ).initSavedStateHandle(),
            resourceProvider = resourceProvider,
            productCategoriesRepository = productCategoriesRepository,
            networkStatus = networkStatus,
            productRestrictions = productRestrictions,
            pluginRepository = mock(),
            selectedSite = mock()
        )

        whenever(resourceProvider.getString(any())).thenReturn("")
    }

    @Test
    fun `given there is no NonPublish product restriction, then display product status filter`() {
        val productFilters = mutableListOf<ProductFilterListViewModel.FilterListItemUiModel>()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters.addAll(it)
        }

        productFilterListViewModel.loadFilters()

        assertTrue(
            productFilters.firstOrNull {
                it.filterItemKey == WCProductStore.ProductFilterOption.STATUS
            } != null
        )
    }

    @Test
    fun `given there is a NonPublish product restriction, then do not display product status filter`() {
        val productFilters = mutableListOf<ProductFilterListViewModel.FilterListItemUiModel>()
        productFilterListViewModel.filterListItems.observeForever {
            productFilters.addAll(it)
        }
        whenever(productRestrictions.restrictions).thenReturn(
            listOf(ProductRestriction.NonPublishedProducts)
        )

        productFilterListViewModel.loadFilters()

        assertFalse(
            productFilters.firstOrNull {
                it.filterItemKey == WCProductStore.ProductFilterOption.STATUS
            } != null
        )
    }
}
