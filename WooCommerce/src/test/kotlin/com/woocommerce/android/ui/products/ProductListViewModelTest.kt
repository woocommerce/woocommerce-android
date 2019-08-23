package com.woocommerce.android.ui.products

import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.spy
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.test
import kotlinx.coroutines.Dispatchers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class ProductListViewModelTest : BaseUnitTest() {
    private val networkStatus: NetworkStatus = mock()
    private val productRepository: ProductListRepository = mock()

    private val productList = ProductTestUtils.generateProductList()
    private lateinit var viewModel: ProductListViewModel

    @Before
    fun setup() {
        viewModel = spy(
                ProductListViewModel(
                        Dispatchers.Unconfined,
                        productRepository,
                        networkStatus
                )
        )

        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Displays the product list view correctly`() {
        doReturn(productList).whenever(productRepository).getProductList()

        val products = ArrayList<Product>()
        viewModel.productList.observeForever { products.addAll(it) }

        viewModel.start()
        assertThat(products).isEqualTo(productList)
    }


    @Test
    fun `Do not fetch product list from api when not connected`() = test {
        doReturn(false).whenever(networkStatus).isConnected()

        var message: Int? = null
        viewModel.showSnackbarMessage.observeForever { message = it }

        viewModel.start()

        verify(productRepository, times(1)).getProductList()
        verify(productRepository, times(0)).fetchProductList()

        assertThat(message).isEqualTo(R.string.offline_error)
    }

    @Test
    fun `Shows and hides product list skeleton correctly`() = test {
        doReturn(emptyList<Product>()).whenever(productRepository).getProductList()

        val isSkeletonShown = ArrayList<Boolean>()
        viewModel.isSkeletonShown.observeForever { isSkeletonShown.add(it) }

        viewModel.start()
        assertThat(isSkeletonShown).containsExactly(true, false)
    }

    @Test
    fun `Shows and hides product list load more progress correctly`() = test {
        doReturn(true).whenever(productRepository).canLoadMoreProducts

        val isLoadingMore = ArrayList<Boolean>()
        viewModel.isLoadingMore.observeForever { isLoadingMore.add(it) }

        viewModel.loadMoreProducts()
        assertThat(isLoadingMore).containsExactly(true, false)
    }
}
