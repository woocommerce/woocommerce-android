package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProductListHandlerTest : BaseUnitTest() {
    companion object {
        private val VALID_PRODUCT = ProductTestUtils.generateProduct()
        private val DRAFT_PRODUCT = ProductTestUtils.generateProduct(customStatus = "draft")
        private val VARIABLE_PRODUCT_WITH_NO_VARIATIONS = ProductTestUtils.generateProduct(
            isVariable = true,
            variationIds = "[]",
        )
    }

    private val repository: ProductSelectorRepository = mock {
        on(it.observeProducts(any())) doReturn flowOf(
            listOf(
                VALID_PRODUCT,
                DRAFT_PRODUCT,
                VARIABLE_PRODUCT_WITH_NO_VARIATIONS,
            )
        )
    }

    private val sut = ProductListHandler(repository)

    @Test
    fun `should not return draft products`() = testBlocking {
        val job = launch {
            sut.productsFlow.collect { products ->
                assertThat(products).contains(VALID_PRODUCT)
                assertThat(products).doesNotContain(DRAFT_PRODUCT)
            }
        }
        job.cancel()
    }

    @Test
    fun `should not return variable products with no variations`() = testBlocking {
        assertTrue(VARIABLE_PRODUCT_WITH_NO_VARIATIONS.numVariations == 0)
        assertTrue(VARIABLE_PRODUCT_WITH_NO_VARIATIONS.variationIds.isEmpty())
        assertTrue(VARIABLE_PRODUCT_WITH_NO_VARIATIONS.type == "variable")

        val job = launch {
            sut.productsFlow.collect { products ->
                assertThat(products).contains(VALID_PRODUCT)
                assertThat(products).doesNotContain(VARIABLE_PRODUCT_WITH_NO_VARIATIONS)
            }
        }
        job.cancel()
    }
}
