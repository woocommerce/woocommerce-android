package com.woocommerce.android.ui.products

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.categories.ProductCategoriesRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

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
        )

        whenever(resourceProvider.getString(any())).thenReturn("")
    }
}
