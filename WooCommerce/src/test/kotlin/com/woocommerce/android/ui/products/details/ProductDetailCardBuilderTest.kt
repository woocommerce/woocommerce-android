package com.woocommerce.android.ui.products.details

import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.SiteModel

@ExperimentalCoroutinesApi
class ProductDetailCardBuilderTest : BaseUnitTest() {
    private lateinit var sut: ProductDetailCardBuilder
    private lateinit var productStub: Product
    private val isBlazeEnabled: IsBlazeEnabled = mock {
        onBlocking { invoke() } doReturn false
    }

    @Before
    fun setUp() {
        val resources: ResourceProvider = mock {
            on { getString(any()) } doReturn ""
            on { getString(any(), anyVararg()) } doReturn ""
        }

        val addonRepo: AddonRepository = mock {
            onBlocking { hasAnyProductSpecificAddons(any()) } doReturn false
        }

        val selectedSite: SelectedSite = mock {
            on { get() } doReturn SiteModel()
        }

        sut = ProductDetailCardBuilder(
            viewModel = mock(),
            selectedSite = selectedSite,
            resources = resources,
            currencyFormatter = mock(),
            parameters = mock(),
            addonRepository = addonRepo,
            variationRepository = mock(),
            appPrefsWrapper = mock(),
            isBlazeEnabled = isBlazeEnabled,
            isProductCurrentlyPromoted = mock(),
            analyticsTrackerWrapper = mock()
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
        Assertions.assertThat(cards).isNotEmpty

        cards.find { it.type == ProductPropertyCard.Type.SECONDARY }
            ?.properties?.mapNotNull { it as? ProductProperty.PropertyGroup }
            ?.find { propertyGroup ->
                propertyGroup.properties.toList()
                    .find { it.first == "Color" } != null
            }?.properties?.toList()?.let {
                Assertions.assertThat(it.first()).isEqualTo(Pair("Color", "3"))
            } ?: Assertions.fail("Expected a Product card with a single Attribute named Color with value 3 selected")
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
        Assertions.assertThat(cards).isNotEmpty

        cards.find { it.type == ProductPropertyCard.Type.SECONDARY }
            ?.properties?.mapNotNull { it as? ProductProperty.PropertyGroup }
            ?.find { propertyGroup ->
                propertyGroup.properties.toList()
                    .find { it.first == "Color" } != null
            }?.properties?.toList()?.let {
                foundAttributesCard = true
            }

        Assert.assertFalse("Expected no Product card with Attributes configured", foundAttributesCard)
    }

    @Test
    fun `given a product with at least one quantity rule, then create Quantity Rules card`() = testBlocking {
        val productMinAllowedQuantity = 8529
        productStub = ProductTestUtils.generateProduct()
            .copy(
                minAllowedQuantity = productMinAllowedQuantity
            )

        var foundQuantityRulesCard = false
        val cards = sut.buildPropertyCards(productStub, "")
        Assertions.assertThat(cards).isNotEmpty

        cards.find { it.type == ProductPropertyCard.Type.SECONDARY }
            ?.properties?.mapNotNull { it as? ProductProperty.PropertyGroup }
            ?.find { propertyGroup ->
                propertyGroup.properties.toList()
                    .find {
                        it.second == productMinAllowedQuantity.toString()
                    } != null
            }?.properties?.toList()?.let {
                foundQuantityRulesCard = true
            }

        Assert.assertTrue("Expected a Product card with Quantity Rules", foundQuantityRulesCard)
    }
}
