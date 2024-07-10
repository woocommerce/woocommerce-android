package com.woocommerce.android.ui.woopos.home.products

import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.woopos.home.WooPosChildrenToParentEventSender
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import kotlin.test.Test

class WooPosProductsViewModelTest {
    private val productsDataSource: WooPosProductsDataSource = mock()
    private val fromChildToParentEventSender: WooPosChildrenToParentEventSender = mock()
    private val priceFormat: WooPosFormatPrice = mock {
        onBlocking { invoke(BigDecimal("10.0")) }.thenReturn("$10.0")
        onBlocking { invoke(BigDecimal("20.0")) }.thenReturn("$20.0")
    }

    @Test
    fun `given products from data source, when view model created, then view state updated correctly`() =
        runTest {
            // GIVEN
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "10.0",
                    productType = "simple"
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple"
                ).copy(firstImageUrl = "https://test.com")
            )

            whenever(productsDataSource.products).thenReturn(flowOf(products))

            // WHEN
            val viewModel = createViewModel()

            // THEN
            val value = viewModel.viewState.value as WooPosProductsViewState.Content
            assertThat(value.products).hasSize(2)
            assertThat(value.products[0].id).isEqualTo(1)
            assertThat(value.products[0].name).isEqualTo("Product 1")
            assertThat(value.products[0].price).isEqualTo("$10.0")
            assertThat(value.products[1].id).isEqualTo(2)
            assertThat(value.products[1].name).isEqualTo("Product 2")
            assertThat(value.products[1].price).isEqualTo("$20.0")
            assertThat(value.products[1].imageUrl).isEqualTo("https://test.com")
        }

    @Test
    fun `given empty products list returned, when view model created, then view state is empty`() =
        runTest {
            // GIVEN
            whenever(productsDataSource.products).thenReturn(flowOf(emptyList()))

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.viewState.value).isEqualTo(WooPosProductsViewState.Empty())
        }

    @Test
    fun `given loading products is failure, when view model created, then view state is empty`() =
        runTest {
            // GIVEN
            whenever(productsDataSource.loadSimpleProducts()).thenReturn(Result.failure(Exception()))

            // WHEN
            val viewModel = createViewModel()

            // THEN
            assertThat(viewModel.viewState.value).isEqualTo(WooPosProductsViewState.Error())
        }

    @Test
    fun `given products from data source, when pulled to refresh, then should remove products and fetch again`() =
        runTest {
            // GIVEN
            val products = listOf(
                ProductTestUtils.generateProduct(
                    productId = 1,
                    productName = "Product 1",
                    amount = "10.0",
                    productType = "simple"
                ),
                ProductTestUtils.generateProduct(
                    productId = 2,
                    productName = "Product 2",
                    amount = "20.0",
                    productType = "simple"
                ).copy(firstImageUrl = "https://test.com")
            )

            whenever(productsDataSource.products).thenReturn(flowOf(products))

            // WHEN
            val viewModel = createViewModel()
            viewModel.onUIEvent(WooPosProductsUIEvent.PullToRefreshTriggered)

            // THEN
            verify(productsDataSource).loadSimpleProducts(forceRefreshProducts = true)
        }

    @Test
    fun `when loading without pull to refresh, then should not ask to remove products`() = runTest {
        // GIVEN
        val products = listOf(
            ProductTestUtils.generateProduct(
                productId = 1,
                productName = "Product 1",
                amount = "10.0",
                productType = "simple"
            ),
            ProductTestUtils.generateProduct(
                productId = 2,
                productName = "Product 2",
                amount = "20.0",
                productType = "simple"
            ).copy(firstImageUrl = "https://test.com")
        )

        whenever(productsDataSource.products).thenReturn(flowOf(products))

        // WHEN
        createViewModel()

        // THEN
        verify(productsDataSource).loadSimpleProducts(forceRefreshProducts = false)
    }

    private fun createViewModel() =
        WooPosProductsViewModel(
            productsDataSource,
            fromChildToParentEventSender,
            priceFormat
        )
}
