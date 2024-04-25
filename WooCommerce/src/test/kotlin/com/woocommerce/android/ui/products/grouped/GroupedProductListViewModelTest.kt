package com.woocommerce.android.ui.products.grouped

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class GroupedProductListViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private val GROUPED_PRODUCT_IDS = longArrayOf(2, 3, 4, 5)
    }

    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }
    private val productRepository: GroupedProductListRepository = mock()
    private val savedState = GroupedProductListFragmentArgs(
        remoteProductId = PRODUCT_REMOTE_ID,
        productIds = GROUPED_PRODUCT_IDS,
        groupedProductListType = GroupedProductListType.GROUPED
    ).toSavedStateHandle()

    private val productList = ProductTestUtils.generateProductList()
    private val groupedProductIds = GROUPED_PRODUCT_IDS.toList()

    private lateinit var viewModel: GroupedProductListViewModel

    private fun createViewModel() {
        viewModel = GroupedProductListViewModel(
            savedState,
            networkStatus,
            productRepository
        )
    }

    @Test
    fun `Displays the grouped product list view correctly`() = testBlocking {
        doReturn(productList).whenever(productRepository).fetchProductList(groupedProductIds)

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        Assertions.assertThat(products).isEqualTo(productList)
    }

    @Test
    fun `Displays grouped product list view correctly after deletion`() {
        createViewModel()

        Assertions.assertThat(viewModel.hasChanges).isEqualTo(false)

        var productData: GroupedProductListViewModel.GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        viewModel.onProductDeleted(productList.last())

        Assertions.assertThat(groupedProductIds.size - 1).isEqualTo(productData?.selectedProductIds?.size)
        Assertions.assertThat(viewModel.hasChanges).isEqualTo(true)
    }

    @Test
    fun `Displays grouped product list view correctly after addition`() {
        createViewModel()

        Assertions.assertThat(viewModel.hasChanges).isEqualTo(false)

        var productData: GroupedProductListViewModel.GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        val listAdded = listOf<Long>(6, 7, 8)
        viewModel.onProductsAdded(listAdded)

        Assertions.assertThat(groupedProductIds.size + listAdded.size).isEqualTo(productData?.selectedProductIds?.size)
        Assertions.assertThat(viewModel.hasChanges).isEqualTo(true)
    }

    @Test
    fun `ExitWithResult event dispatched correctly when back button clicked`() {
        createViewModel()

        var productData: GroupedProductListViewModel.GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        var event: MultiLiveEvent.Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onProductDeleted(productList.last())
        viewModel.onBackButtonClicked()

        Assertions.assertThat(event).isInstanceOf(MultiLiveEvent.Event.ExitWithResult::class.java)
        Assertions.assertThat(((event as MultiLiveEvent.Event.ExitWithResult<*>).data as List<*>).size).isEqualTo(
            productData?.selectedProductIds?.size
        )
    }
}
