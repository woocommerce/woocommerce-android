package com.woocommerce.android.ui.products.details

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.model.ProductAggregate
import com.woocommerce.android.ui.customfields.CustomFieldsRepository
import com.woocommerce.android.ui.products.ProductNavigationTarget
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ProductDetailBottomSheetBuilderTest : BaseUnitTest() {
    private lateinit var sut: ProductDetailBottomSheetBuilder

    private val resourceProvider: ResourceProvider = mock()
    private val variationRepository: VariationRepository = mock()
    private val customFieldsRepository: CustomFieldsRepository = mock {
        on { hasDisplayableCustomFields(any()) } doReturn false
    }

    private var isNonVariationAttributesEnabled = false

    @Before
    fun setUp() {
        sut = ProductDetailBottomSheetBuilder(
            resources = resourceProvider,
            variationRepository = variationRepository,
            customFieldsRepository = customFieldsRepository,
            isNonVariationAttributesEnabled = { isNonVariationAttributesEnabled }
        )
    }

    @Test
    fun `given the feature is on and a product has no attributes, when building the list, then show the attributes item`() =
        testBlocking {
            // GIVEN
            isNonVariationAttributesEnabled = true
            val product = ProductTestUtils.generateProduct(productId = 1L).copy(attributes = emptyList())

            // WHEN
            val result = sut.buildBottomSheetList(ProductAggregate(product))

            // THEN
            val attributesItem = result.single {
                it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.PRODUCT_ATTRIBUTES
            }
            assertThat(attributesItem.clickEvent).isEqualTo(ProductNavigationTarget.AddProductAttribute())
        }

    @Test
    fun `given the feature is on and a product of each type has no attributes, when building the list, then show the attributes item`() =
        testBlocking {
            // GIVEN
            isNonVariationAttributesEnabled = true
            val productTypes = listOf(
                ProductType.SIMPLE,
                ProductType.SUBSCRIPTION,
                ProductType.EXTERNAL,
                ProductType.GROUPED,
                ProductType.VARIABLE,
                ProductType.VARIABLE_SUBSCRIPTION,
                ProductType.BUNDLE,
            )

            productTypes.forEach { productType ->
                val product = ProductTestUtils.generateProduct(productId = 1L)
                    .copy(type = productType.value, attributes = emptyList())

                // WHEN
                val result = sut.buildBottomSheetList(ProductAggregate(product))

                // THEN
                val attributesItem = result.singleOrNull {
                    it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.PRODUCT_ATTRIBUTES
                }
                assertThat(attributesItem).describedAs(productType.name).isNotNull
                assertThat(attributesItem?.stat)
                    .isEqualTo(AnalyticsEvent.PRODUCT_ATTRIBUTE_ADD_BUTTON_TAPPED)
            }
        }

    @Test
    fun `given the feature is on and the product has attributes, when building the list, then hide the attributes item`() = testBlocking {
        // GIVEN
        isNonVariationAttributesEnabled = true
        val product = ProductTestUtils.generateProduct(productId = 1L)

        // WHEN
        val result = sut.buildBottomSheetList(ProductAggregate(product))

        // THEN
        assertThat(product.attributes).isNotEmpty
        assertThat(result).noneMatch {
            it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.PRODUCT_ATTRIBUTES
        }
    }

    @Test
    fun `given the feature is off, when building the list, then hide the attributes item`() = testBlocking {
        // GIVEN
        isNonVariationAttributesEnabled = false
        val product = ProductTestUtils.generateProduct(productId = 1L).copy(attributes = emptyList())

        // WHEN
        val result = sut.buildBottomSheetList(ProductAggregate(product))

        // THEN
        assertThat(result).noneMatch {
            it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.PRODUCT_ATTRIBUTES
        }
    }

    @Test
    fun `when a product has displayable custom fields, then hide the custom fields item`() = testBlocking {
        whenever(customFieldsRepository.hasDisplayableCustomFields(any())).thenReturn(true)

        val product = ProductTestUtils.generateProduct(productId = 1L)
        val result = sut.buildBottomSheetList(ProductAggregate(product))

        assertThat(result).noneMatch {
            it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.CUSTOM_FIELDS
        }
    }

    @Test
    fun `when a product has no displayable custom fields, then show the custom fields item`() = testBlocking {
        whenever(customFieldsRepository.hasDisplayableCustomFields(any())).thenReturn(false)

        val product = ProductTestUtils.generateProduct(productId = 1L)
        val result = sut.buildBottomSheetList(ProductAggregate(product))

        val customFieldsItem = result.single {
            it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.CUSTOM_FIELDS
        }
        assertThat(customFieldsItem.clickEvent).isEqualTo(ProductNavigationTarget.ViewCustomFields(product.remoteId))
    }

    @Test
    fun `when product is not saved in server, then hide the custom fields item`() = testBlocking {
        val product = ProductTestUtils.generateProduct(productId = ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
        val result = sut.buildBottomSheetList(ProductAggregate(product))

        assertThat(result).noneMatch {
            it.type == ProductDetailBottomSheetBuilder.ProductDetailBottomSheetType.CUSTOM_FIELDS
        }
    }
}
