package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import org.junit.Before
import org.mockito.kotlin.mock

class ProductDetailCardBuilderTest {
    private lateinit var sut: ProductDetailCardBuilder
    private lateinit var productStub: Product

    @Before
    fun setUp() {
        productStub = ProductTestUtils.generateProduct(
            productType = ProductType.VARIABLE.value
        )

        sut = ProductDetailCardBuilder(
            viewModel = mock(),
            resources = mock(),
            currencyFormatter = mock(),
            parameters = mock(),
            addonRepository = mock(),
            variationRepository = mock()
        )
    }
}
