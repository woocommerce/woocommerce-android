package com.woocommerce.android.ui.products.selector

import app.cash.turbine.test
import com.woocommerce.android.ui.products.ProductHelper.getDefaultNewProduct
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductListHandler.SearchType
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class ProductListHandlerTest : BaseUnitTest() {

    private val repo: ProductSelectorRepository = mock {
        on(it.observeProducts(any())) doReturn flow { emit(generateSampleProducts()) }

        onBlocking {
            (it.fetchProducts(any(), any(), any()))
        } doReturn Result.success(true)
    }

    private fun generateSampleProducts() = (1..1000).map {
        getDefaultNewProduct(ProductType.SIMPLE, false).copy(
            remoteId = it.toLong()
        )
    }

    @Test
    fun `when load invoked, then emits first 25 products from db`() = testBlocking {
        whenever(repo.fetchProducts(any(), any(), any())).doReturn(Result.success(true))
        val handler = ProductListHandler(repo)
        handler.loadFromCacheAndFetch(searchType = SearchType.DEFAULT)

        handler.productsFlow.test {
            val products = awaitItem()
            assertThat(products.size).isEqualTo(25)
            assert(products.size == 25)
            assert(products.first().remoteId == 1L)
            assert(products.last().remoteId == 25L)
        }
    }

    @Test
    fun `when load invoked, then side fetches first 25 products from backend`() = testBlocking {
        val handler = ProductListHandler(repo)
        handler.loadFromCacheAndFetch(searchType = SearchType.DEFAULT)
        verify(repo).fetchProducts(0, 25, emptyMap())
    }

    @Test
    fun `when load more invoked, then fetches next 25 products`() = testBlocking {
        val handler = ProductListHandler(repo)
        handler.loadFromCacheAndFetch(searchType = SearchType.DEFAULT)

        handler.loadMore()

        verify(repo).fetchProducts(25, 25, emptyMap())

        handler.productsFlow.test {
            val products = awaitItem()
            assert(products.size == 50)
            assert(products.first().remoteId == 1L)
            assert(products.last().remoteId == 50L)
        }
    }
}
