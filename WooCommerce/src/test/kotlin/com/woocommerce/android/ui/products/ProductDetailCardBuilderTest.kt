package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailCardBuilderTest: BaseUnitTest() {
    private lateinit var sut: ProductDetailCardBuilder
    private lateinit var productStub: Product

    @Before
    fun setUp() {
        val resources: ResourceProvider = mock {
            on { getString(any()) } doReturn ""
            on { getString(any(), anyVararg()) } doReturn ""
        }

        val addonRepo: AddonRepository = mock {
            onBlocking { hasAnyProductSpecificAddons(any()) } doReturn false
        }

        sut = ProductDetailCardBuilder(
            viewModel = mock(),
            resources = resources,
            currencyFormatter = mock(),
            parameters = mock(),
            addonRepository = addonRepo,
            variationRepository = mock()
        )
    }
}
