package com.woocommerce.android.ui.products

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.GroupedProductListViewModel.GroupedProductListViewState
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class GroupedProductListViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_REMOTE_ID = 1L
        private const val GROUPED_PRODUCT_IDS = "2,3,4,5"
    }

    private val networkStatus: NetworkStatus = mock()
    private val productRepository: GroupedProductListRepository = mock()
    private val savedState: SavedStateWithArgs = spy(
        SavedStateWithArgs(
            SavedStateHandle(),
            null,
            GroupedProductListFragmentArgs(
                remoteProductId = PRODUCT_REMOTE_ID,
                productIds = GROUPED_PRODUCT_IDS,
                groupedProductListType = GroupedProductListType.GROUPED
            )
        )
    )

    private val coroutineDispatchers = CoroutineDispatchers(
        Dispatchers.Unconfined, Dispatchers.Unconfined, Dispatchers.Unconfined)
    private val productList = ProductTestUtils.generateProductList()
    private val groupedProductIds = GROUPED_PRODUCT_IDS.split(",").map { it.toLong() }

    private lateinit var viewModel: GroupedProductListViewModel

    @Before
    fun setup() {
        doReturn(MutableLiveData(GroupedProductListViewState(groupedProductIds)))
            .whenever(savedState).getLiveData<GroupedProductListViewState>(any(), any())
        doReturn(true).whenever(networkStatus).isConnected()
    }

    private fun createViewModel() {
        viewModel = spy(
            GroupedProductListViewModel(
                savedState,
                coroutineDispatchers,
                networkStatus,
                productRepository
            )
        )
    }

    @Test
    fun `Displays the grouped product list view correctly`() = test {
        doReturn(productList).whenever(productRepository).fetchProductList(groupedProductIds)

        createViewModel()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever {
            it?.let { products.addAll(it) }
        }

        assertThat(products).isEqualTo(productList)
    }

    @Test
    fun `Displays grouped product list view correctly after deletion`() {
        createViewModel()

        val isDoneButtonDisplayed = ArrayList<Boolean>()
        viewModel.productListViewStateData.observeForever { old, new ->
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isDoneButtonDisplayed.add(it) }
        }

        assertThat(viewModel.hasChanges).isEqualTo(false)

        var productData: GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        viewModel.onProductDeleted(productList.last())

        assertThat(groupedProductIds.size - 1).isEqualTo(productData?.selectedProductIds?.size)
        assertThat(viewModel.hasChanges).isEqualTo(true)
        assertThat(isDoneButtonDisplayed).containsExactly(true)
    }

    @Test
    fun `Displays grouped product list view correctly after addition`() {
        createViewModel()

        val isDoneButtonDisplayed = ArrayList<Boolean>()
        viewModel.productListViewStateData.observeForever { old, new ->
            new.isDoneButtonVisible?.takeIfNotEqualTo(old?.isDoneButtonVisible) { isDoneButtonDisplayed.add(it) }
        }
        assertThat(viewModel.hasChanges).isEqualTo(false)

        var productData: GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        val listAdded = listOf<Long>(6, 7, 8)
        viewModel.onProductsAdded(listAdded)

        assertThat(groupedProductIds.size + listAdded.size).isEqualTo(productData?.selectedProductIds?.size)
        assertThat(viewModel.hasChanges).isEqualTo(true)
        assertThat(isDoneButtonDisplayed).containsExactly(true)
    }

    @Test
    fun `ExitWithResult event dispatched correctly when done menu button clicked`() {
        createViewModel()

        var productData: GroupedProductListViewState? = null
        viewModel.productListViewStateData.observeForever { _, new -> productData = new }

        var event: Event? = null
        viewModel.event.observeForever { new -> event = new }

        viewModel.onProductDeleted(productList.last())
        viewModel.onDoneButtonClicked()

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        assertThat(((event as ExitWithResult<*>).data as List<*>).size).isEqualTo(
            productData?.selectedProductIds?.size
        )
    }
}
