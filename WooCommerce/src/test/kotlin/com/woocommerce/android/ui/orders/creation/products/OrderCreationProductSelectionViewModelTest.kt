package com.woocommerce.android.ui.orders.creation.products

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.products.ProductListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class OrderCreationProductSelectionViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderCreationProductSelectionViewModel

    private val productListRepository: ProductListRepository = mock {
        on { getProductList() } doReturn emptyList()
        onBlocking { fetchProductList() } doReturn emptyList()
    }

    @Before
    fun setup() {
        sut = OrderCreationProductSelectionViewModel(
            SavedStateHandle(),
            productListRepository
        )
    }

    @Test
    fun `when loading products, then get cached products before fetching from remote`() = testBlocking {
        inOrder(productListRepository).run {
            this.verify(productListRepository).getProductList()
            this.verify(productListRepository).fetchProductList()
        }
    }

    //should hide skeleton twice if products are cached

    //onProductSelected trigger AddProduct when numVariations == 0

    //onProductSelected trigger ShowProductVariations when numVariations > 0
}
