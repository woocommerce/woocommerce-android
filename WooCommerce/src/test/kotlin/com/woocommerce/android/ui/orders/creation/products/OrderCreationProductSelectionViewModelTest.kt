package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

@ExperimentalCoroutinesApi
class OrderCreationProductSelectionViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationProductSelectionViewModel
    private lateinit var productListRepository: ProductListRepository

    @Before
    fun setUp() {
        productListRepository = mock {
            on { getProductList() } doReturn ProductTestUtils.generateProductList()
            onBlocking { fetchProductList() } doReturn ProductTestUtils.generateProductList()
        }
    }

    @Test
    fun `when loading products, then get cached products before fetching from remote`() = testBlocking {
        startSut()
        inOrder(productListRepository).run {
            this.verify(productListRepository).getProductList()
            this.verify(productListRepository).fetchProductList()
        }
    }

    @Test
    fun `when loading products, then pass loadMore to fetch products from store`() = testBlocking {
        startSut()
        verify(productListRepository).fetchProductList(false)
        sut.loadProductList(true)
        verify(productListRepository).fetchProductList(true)
    }

    @Test
    fun `when loading empty cached product list, then ignore result`() = testBlocking {
        var productListUpdateCalls = 0
        whenever(productListRepository.getProductList()).thenReturn(emptyList())
        startSut()
        sut.productListData.observeForever { productListUpdateCalls++ }
        productListUpdateCalls = 0
        sut.loadProductList()
        assertThat(productListUpdateCalls).isEqualTo(1)
    }

    //onProductSelected trigger AddProduct when numVariations == 0

    //onProductSelected trigger ShowProductVariations when numVariations > 0

    private fun startSut() {
        sut = OrderCreationProductSelectionViewModel(
            SavedStateHandle(),
            productListRepository
        )
    }
}
