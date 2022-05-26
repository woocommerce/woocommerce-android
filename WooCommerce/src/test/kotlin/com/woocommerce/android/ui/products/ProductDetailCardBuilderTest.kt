package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard.Type.SECONDARY
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailCardBuilderTest : BaseUnitTest() {
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

    @Test
    fun `given a product with at least one attribute, then create Attributes card`() = testBlocking {
        productStub = ProductTestUtils.generateProduct()
            .copy(
                reviewsAllowed = false,
                type = ProductType.VARIABLE.value,
                weight = 0F,
                length = 0F,
                width = 0F,
                height = 0F
            )

        val cards = sut.buildPropertyCards(productStub, "")
        assertThat(cards).isNotEmpty

        cards.find { it.type == SECONDARY }
            ?.properties?.mapNotNull { it as? ProductProperty.PropertyGroup }
            ?.find { propertyGroup ->
                propertyGroup.properties.toList()
                    .find { it.first == "Color" } != null
            }?.properties?.toList()?.let {
                assertThat(it.first()).isEqualTo(Pair("Color", "3"))
            } ?: fail("Expected a Product card with a single Attribute named Color with value 3 selected")
    }

    @Test
    fun `given a product with no attribute, then ignore Attributes card`() = testBlocking {
        productStub = ProductTestUtils.generateProduct()
            .copy(
                reviewsAllowed = false,
                type = ProductType.VARIABLE.value,
                weight = 0F,
                length = 0F,
                width = 0F,
                height = 0F,
                attributes = emptyList()
            )

        var foundAttributesCard = false
        val cards = sut.buildPropertyCards(productStub, "")
        assertThat(cards).isNotEmpty

        cards.find { it.type == SECONDARY }
            ?.properties?.mapNotNull { it as? ProductProperty.PropertyGroup }
            ?.find { propertyGroup ->
                propertyGroup.properties.toList()
                    .find { it.first == "Color" } != null
            }?.properties?.toList()?.let {
                foundAttributesCard = true
            }

        assertFalse("Expected no Product card with Attributes configured", foundAttributesCard)
    }
}
