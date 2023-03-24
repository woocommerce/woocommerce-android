package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorRestriction.NoVariableProductsWithNoVariations
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel.ProductSelectorRestriction.OnlyPublishedProducts
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
internal class ProductSelectorViewModelTest : BaseUnitTest() {
    companion object {
        private val VALID_PRODUCT = ProductTestUtils.generateProduct(productId = 1L)
        private val DRAFT_PRODUCT = ProductTestUtils.generateProduct(productId = 2L, customStatus = "draft")
        private val VARIABLE_PRODUCT_WITH_NO_VARIATIONS = ProductTestUtils.generateProduct(
            productId = 3L,
            isVariable = true,
            variationIds = "[]",
        )
    }

    private val currencyFormatter: CurrencyFormatter = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val listHandler: ProductListHandler = mock {
        on { productsFlow } doReturn flowOf(listOf(VALID_PRODUCT, DRAFT_PRODUCT, VARIABLE_PRODUCT_WITH_NO_VARIATIONS))
    }
    private val variationSelectorRepository: VariationSelectorRepository = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val orderStore: WCOrderStore = mock()
    private val productsMapper: ProductsMapper = mock()

    @Test
    fun `given published products restriction, when view model created, should not show draft products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(state.products.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given no variable products with no variations restriction, when view model created, should not show variable products with no variations`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(NoVariableProductsWithNoVariations),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter { it.type == ProductType.VARIABLE && it.numVariations == 0 }
            ).isEmpty()
        }
    }

    @Test
    fun `given multiple restrictions, when view model created, should show correct products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = arrayOf(OnlyPublishedProducts, NoVariableProductsWithNoVariations),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products).isNotEmpty
            assertThat(
                state.products.filter { it.type == ProductType.VARIABLE && it.numVariations == 0 }
            ).isEmpty()
            assertThat(state.products.filter { it.id == DRAFT_PRODUCT.remoteId }).isEmpty()
        }
    }

    @Test
    fun `given no restrictions, when view model created, should show all products`() {
        val navArgs = ProductSelectorFragmentArgs(
            selectedItems = emptyArray(),
            restrictions = emptyArray(),
        ).initSavedStateHandle()

        val sut = ProductSelectorViewModel(
            navArgs,
            currencyFormatter,
            wooCommerceStore,
            orderStore,
            selectedSite,
            listHandler,
            variationSelectorRepository,
            resourceProvider,
            productsMapper,
        )

        sut.viewState.observeForever { state ->
            assertThat(state.products.count()).isEqualTo(3)
            assertThat(
                state.products.map { it.id }
            ).containsAll(
                listOf(VALID_PRODUCT.remoteId, DRAFT_PRODUCT.remoteId, VARIABLE_PRODUCT_WITH_NO_VARIATIONS.remoteId)
            )
        }
    }

    private fun generateLineItems(
        name: String,
        productId: String,
    ): String {
        return "[{\"id\":1121,\"meta_data\":[],\"name\":\"$name\",\"price\":\"88.6\"," +
            "\"product_id\":$productId,\"quantity\":1.0,\"sku\":\"ACBI\"," +
            "\"subtotal\":\"88.60\",\"total\":\"88.60\",\"total_tax\":\"0.00\",\"variation_id\":0}]"
    }

    private fun generateTestOrders() = listOf(
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "ACME Bike",
                productId = "2444"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Variation Product",
                productId = "2446"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Lorry",
                productId = "2449"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "Bus",
                productId = "2450"
            )
        ),
        OrderTestUtils.generateOrder(
            lineItems = generateLineItems(
                name = "TVS",
                productId = "2451"
            )
        ),
    )

}
