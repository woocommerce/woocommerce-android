package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.OrderCreationNavigationTarget.ShowProductVariations
import com.woocommerce.android.ui.orders.creation.products.OrderCreationProductSelectionViewModel.AddProduct
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
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
            val products = ProductTestUtils.generateProductList()
            on { getProductList() } doReturn products
            onBlocking { fetchProductList() } doReturn products
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
        assertThat(productListUpdateCalls).isEqualTo(1)
    }

    @Test
    fun `when non variable product is selected, then trigger AddProduct event`() = testBlocking {
        var lastReceivedEvent: Event? = null
        whenever(productListRepository.fetchProductList())
            .thenReturn(ProductTestUtils.generateProductListWithVariations())
        startSut()
        sut.event.observeForever {
            lastReceivedEvent = it
        }
        sut.onProductSelected(NON_VARIABLE_PRODUCT_ID)
        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(AddProduct::class.java)
    }

    @Test
    fun `when variable product is selected, then trigger ShowProductVariations event`() = testBlocking {
        var lastReceivedEvent: Event? = null
        whenever(productListRepository.fetchProductList())
            .thenReturn(ProductTestUtils.generateProductListWithVariations())
        startSut()
        sut.event.observeForever {
            lastReceivedEvent = it
        }
        sut.onProductSelected(VARIABLE_PRODUCT_ID)
        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(ShowProductVariations::class.java)
    }

    @Test
    fun `when loaded product list equals fetched products, then ignore the result`() = testBlocking {
        var productListUpdateCalls = 0
        startSut()
        sut.productListData.observeForever { productListUpdateCalls++ }
        assertThat(productListUpdateCalls).isEqualTo(1)
    }

    private fun startSut() {
        sut = OrderCreationProductSelectionViewModel(
            SavedStateHandle(),
            productListRepository
        )
    }

    companion object {
        const val VARIABLE_PRODUCT_ID = 6L
        const val NON_VARIABLE_PRODUCT_ID = 1L
    }
}
