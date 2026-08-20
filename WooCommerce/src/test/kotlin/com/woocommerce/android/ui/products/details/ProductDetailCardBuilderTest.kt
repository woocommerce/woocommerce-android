package com.woocommerce.android.ui.products.details

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.model.Component
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAggregate
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.addons.AddonRepository
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductPropertyCard
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyVararg
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import com.woocommerce.android.ui.compose.designsystem.R as DesignSystemR

@ExperimentalCoroutinesApi
class ProductDetailCardBuilderTest : BaseUnitTest() {
    private lateinit var sut: ProductDetailCardBuilder
    private lateinit var viewModel: ProductDetailViewModel
    private lateinit var productStub: Product
    private val isBlazeEnabled: IsBlazeEnabled = mock {
        on { invoke() } doReturn false
    }
    private val customFieldsRepository: CustomFieldsRepository = mock {
        on { hasDisplayableCustomFields(any()) } doReturn false
    }
    private val addonRepository: AddonRepository = mock {
        on { hasAnyProductSpecificAddons(any()) } doReturn false
    }
    private val selectedSite: SelectedSite = mock {
        on { get() } doReturn SiteModel()
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock {
        on { isProductAddonsEnabled } doReturn false
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.getArgument<Any>(0).toString() }
    }

    @Before
    fun setUp() {
        viewModel = mock {
            on { getShippingClassByRemoteShippingClassId(any()) } doReturn ""
        }

        val resources: ResourceProvider = mock {
            on { getString(any()) } doAnswer { it.arguments[0].toString() }
            on { getString(any(), anyVararg()) } doAnswer { it.arguments[0].toString() + it.arguments[1].toString() }
        }

        val variationRepository: VariationRepository = mock {
            on { getProductVariationList(any()) } doReturn emptyList()
        }

        sut = ProductDetailCardBuilder(
            viewModel = viewModel,
            selectedSite = selectedSite,
            resources = resources,
            currencyFormatter = mock(),
            parameters = mock(),
            addonRepository = addonRepository,
            variationRepository = variationRepository,
            appPrefsWrapper = appPrefsWrapper,
            isBlazeEnabled = isBlazeEnabled,
            isProductCurrentlyPromoted = mock(),
            analyticsTrackerWrapper = mock(),
            customFieldsRepository = customFieldsRepository
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

        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")
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
        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")
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
        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")
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

    @Test
    fun `given a product is saved on server, when a product has no displayable fields, then hide the custom fields card`() = testBlocking {
        whenever(customFieldsRepository.hasDisplayableCustomFields(any())) doReturn false

        productStub = ProductTestUtils.generateProduct(productId = 1L)
        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")

        val properties = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }.properties
        val customFieldsCard = properties.find {
            it is ProductProperty.ComplexProperty &&
                it.title == R.string.product_custom_fields
        }
        Assertions.assertThat(customFieldsCard).isNull()
    }

    @Test
    fun `given a product is saved on server, when a product has displayable fields, then show the custom fields card`() = testBlocking {
        whenever(customFieldsRepository.hasDisplayableCustomFields(any())) doReturn true

        productStub = ProductTestUtils.generateProduct(productId = 1L)
        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")

        val properties = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }.properties
        val customFieldsCard = properties.find {
            it is ProductProperty.ComplexProperty &&
                it.title == R.string.product_custom_fields
        }
        Assertions.assertThat(customFieldsCard).isNotNull
    }

    @Test
    fun `when a new is not saved on the server, then hide the custom fields card`() = testBlocking {
        productStub = ProductTestUtils.generateProduct(productId = ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")

        val properties = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }.properties
        val customFieldsCard = properties.find {
            it is ProductProperty.ComplexProperty &&
                it.title == R.string.product_custom_fields
        }
        Assertions.assertThat(customFieldsCard).isNull()
    }

    @Test
    fun `given subscription product with one time shipping enabled, when building cards, then shipping includes one-time shipping`() = testBlocking {
        productStub = ProductTestUtils.generateProduct()
            .copy(
                isVirtual = false,
                type = ProductType.SUBSCRIPTION.value,
                weight = 1.5f,
                length = 10f,
                width = 20f,
                height = 30f,
                shippingClassId = 123
            )

        val subscriptionDetails = ProductHelper.getDefaultSubscriptionDetails().copy(
            oneTimeShipping = true
        )

        val cards = sut.buildPropertyCards(
            ProductAggregate(
                product = productStub,
                subscription = subscriptionDetails
            ),
            ""
        )

        val shippingGroup = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }
            .properties
            .find {
                it is ProductProperty.PropertyGroup &&
                    it.title == R.string.product_shipping
            } as ProductProperty.PropertyGroup

        val propertyKeys = shippingGroup.properties.toList().map { it.first }
        Assertions.assertThat(propertyKeys).hasSize(4) // Weight, Dimensions, Shipping class, One-time shipping
        Assertions.assertThat(propertyKeys).contains(
            resourceProvider.getString(R.string.subscription_one_time_shipping)
        )
    }

    @Test
    fun `given variable subscription product with one time shipping enabled, when building cards, then shipping includes one-time shipping`() = testBlocking {
        productStub = ProductTestUtils.generateProduct()
            .copy(
                isVirtual = false,
                type = ProductType.VARIABLE_SUBSCRIPTION.value,
                weight = 1.5f,
                length = 10f,
                width = 20f,
                height = 30f,
                shippingClassId = 123
            )

        val subscriptionDetails = ProductHelper.getDefaultSubscriptionDetails().copy(
            oneTimeShipping = true
        )

        val cards = sut.buildPropertyCards(
            ProductAggregate(
                product = productStub,
                subscription = subscriptionDetails
            ),
            ""
        )

        val shippingGroup = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }
            .properties
            .find {
                it is ProductProperty.PropertyGroup &&
                    it.title == R.string.product_shipping
            } as ProductProperty.PropertyGroup

        val propertyKeys = shippingGroup.properties.toList().map { it.first }
        Assertions.assertThat(propertyKeys).hasSize(4) // Weight, Dimensions, Shipping class, One-time shipping
        Assertions.assertThat(propertyKeys).contains(
            resourceProvider.getString(R.string.subscription_one_time_shipping)
        )
    }

    @Test
    fun `given simple non-virtual product, when building cards, then shipping excludes one-time shipping`() = testBlocking {
        productStub = ProductTestUtils.generateProduct()
            .copy(
                isVirtual = false,
                type = ProductType.SIMPLE.value,
                weight = 1.5f,
                length = 10f,
                width = 20f,
                height = 30f,
                shippingClassId = 123
            )

        val cards = sut.buildPropertyCards(ProductAggregate(productStub), "")

        val shippingGroup = cards.first { it.type == ProductPropertyCard.Type.SECONDARY }
            .properties
            .find {
                it is ProductProperty.PropertyGroup &&
                    it.title == R.string.product_shipping
            } as ProductProperty.PropertyGroup

        val propertyKeys = shippingGroup.properties.toList().map { it.first }

        Assertions.assertThat(propertyKeys).hasSize(3) // Weight, Dimensions, Shipping class
        Assertions.assertThat(propertyKeys).doesNotContain(
            resourceProvider.getString(R.string.subscription_one_time_shipping)
        )
    }

    @Test
    fun `given Wagner icon branches, when building cards, then regular design system icons are used`() =
        testBlocking {
            val simpleCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProductWithTagsAndCategories()),
                ""
            )
            val subscriptionCards = sut.buildPropertyCards(
                ProductAggregate(
                    product = ProductTestUtils.generateProduct(productType = ProductType.SUBSCRIPTION.value),
                    subscription = ProductHelper.getDefaultSubscriptionDetails()
                ),
                ""
            )
            val externalCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(productType = ProductType.EXTERNAL.value)),
                ""
            )
            val populatedVariationCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(isVariable = true)),
                ""
            )
            val emptyVariationCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(isVariable = true, variationIds = "[]")),
                ""
            )
            Assertions.assertThat(simpleCards.iconFor(R.string.product_short_description))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_align_left_24dp)
            Assertions.assertThat(subscriptionCards.iconFor(R.string.product_subscription_expiration_title))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_calendar_xmark_24dp)
            Assertions.assertThat(simpleCards.iconFor(R.string.product_categories))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_folder_24dp)
            Assertions.assertThat(subscriptionCards.iconFor(R.string.product_subscription_free_trial_title))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_hourglass_24dp)
            Assertions.assertThat(simpleCards.iconFor(R.string.product_inventory))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_list_check_24dp)
            Assertions.assertThat(populatedVariationCards.iconFor(R.string.product_variations))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_shapes_24dp)
            Assertions.assertThat(emptyVariationCards.iconForValue(R.string.product_detail_add_variations))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_shapes_24dp)
            Assertions.assertThat(simpleCards.iconFor(R.string.product_shipping))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_truck_24dp)
            Assertions.assertThat(externalCards.iconFor(R.string.product_external_link))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_up_right_from_square_24dp)
        }

    @Test
    fun `given ready simple rows, when building cards, then regular design system icons are used`() = testBlocking {
        whenever(selectedSite.get()) doReturn SiteModel().apply { planActiveFeatures = "ai-assistant" }
        whenever(addonRepository.hasAnyProductSpecificAddons(any())) doReturn true
        whenever(appPrefsWrapper.isProductAddonsEnabled) doReturn true
        whenever(customFieldsRepository.hasDisplayableCustomFields(any())) doReturn true
        val simpleCards = sut.buildPropertyCards(
            ProductAggregate(
                ProductTestUtils.generateProductWithTagsAndCategories().copy(
                    minAllowedQuantity = 1,
                    upsellProductIds = listOf(2L)
                )
            ),
            ""
        )

        Assertions.assertThat(simpleCards.iconFor(R.string.product_price))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_square_dollar_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_type))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_box_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_quantity_rules_title))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_sliders_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_downloadable_files))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_cloud_24dp)
        Assertions.assertThat(simpleCards.iconForValue(R.string.product_add_ons_title))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_circle_plus_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_detail_linked_products))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_retweet_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_tags))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_tag_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_reviews))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_star_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_custom_fields))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_grid_plus_24dp)
        Assertions.assertThat(simpleCards.iconFor(R.string.product_sharing_write_with_ai))
            .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_sparkles_24dp)
    }

    @Test
    fun `given ready type-specific rows, when building cards, then regular design system icons are used`() =
        testBlocking {
            doReturn(2).whenever(viewModel).getBundledProductsSize(any())
            doReturn(listOf(component())).whenever(viewModel).getComponents(any())
            val populatedVariationCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(isVariable = true)),
                ""
            )
            val groupedCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(productType = ProductType.GROUPED.value)),
                ""
            )
            val bundleCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(productType = ProductType.BUNDLE.value)),
                ""
            )
            val compositeCards = sut.buildPropertyCards(
                ProductAggregate(ProductTestUtils.generateProduct(productType = ProductType.COMPOSITE.value)),
                ""
            )

            Assertions.assertThat(populatedVariationCards.iconFor(R.string.variable_product_attributes))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_screwdriver_wrench_24dp)
            Assertions.assertThat(groupedCards.iconFor(R.string.grouped_products))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_layer_group_24dp)
            Assertions.assertThat(bundleCards.iconFor(R.string.product_bundle))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_layer_group_24dp)
            Assertions.assertThat(compositeCards.iconFor(R.string.product_components))
                .isEqualTo(DesignSystemR.drawable.woo_ds_ic_regular_layer_group_24dp)
        }

    @Test
    fun `given every product detail type, when mapping builder output, then card and row order is preserved`() = testBlocking {
        doReturn(0).whenever(viewModel).getBundledProductsSize(any())
        doReturn(emptyList<com.woocommerce.android.model.Component>()).whenever(viewModel).getComponents(any())
        val productTypes = listOf(
            ProductType.SIMPLE,
            ProductType.VARIABLE,
            ProductType.GROUPED,
            ProductType.EXTERNAL,
            ProductType.SUBSCRIPTION,
            ProductType.VARIABLE_SUBSCRIPTION,
            ProductType.BUNDLE,
            ProductType.COMPOSITE,
            ProductType.OTHER,
        )
        val mapper = ProductDetailUiMapper()

        productTypes.forEach { productType ->
            val type = productType.value.ifEmpty { "unsupported" }
            val aggregate = ProductAggregate(
                product = ProductTestUtils.generateProduct().copy(type = type),
                subscription = ProductHelper.getDefaultSubscriptionDetails(),
            )

            val cards = sut.buildPropertyCards(aggregate, "")
            val mappedCards = mapper.map(cards)

            Assertions.assertThat(mappedCards.map { it.style }).containsExactlyElementsOf(
                cards.map {
                    when (it.type) {
                        ProductPropertyCard.Type.PRIMARY -> ProductDetailCardStyle.PRIMARY
                        ProductPropertyCard.Type.SECONDARY -> ProductDetailCardStyle.SECONDARY
                    }
                }
            )
            Assertions.assertThat(mappedCards.map { it.rows.size }).containsExactlyElementsOf(
                cards.map { it.properties.size }
            )
        }
    }

    private fun List<ProductPropertyCard>.iconFor(title: Int): Int? =
        flatMap { it.properties }
            .firstNotNullOf { property ->
                when (property) {
                    is ProductProperty.Button -> property.icon.takeIf { property.text == title }
                    is ProductProperty.ComplexProperty -> property.icon.takeIf { property.title == title }
                    is ProductProperty.PropertyGroup -> property.icon.takeIf { property.title == title }
                    is ProductProperty.RatingBar -> property.icon.takeIf { property.title == title }
                    else -> null
                }
            }

    private fun List<ProductPropertyCard>.iconForValue(value: Int): Int? =
        flatMap { it.properties }
            .filterIsInstance<ProductProperty.ComplexProperty>()
            .first { it.value == value.toString() }
            .icon

    private fun component() = Component(
        id = 1L,
        title = "Component",
        description = "",
        queryType = QueryType.PRODUCT,
        queryIds = listOf(1L),
        defaultOptionId = null,
        thumbnailUrl = null
    )
}
