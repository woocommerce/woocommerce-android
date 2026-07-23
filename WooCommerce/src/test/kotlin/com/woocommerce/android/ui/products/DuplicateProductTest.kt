package com.woocommerce.android.ui.products

import com.woocommerce.android.WooException
import com.woocommerce.android.model.ProductAggregate
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NOT_FOUND
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_NOT_FOUND

@OptIn(ExperimentalCoroutinesApi::class)
class DuplicateProductTest : BaseUnitTest() {

    private val productDetailRepository: ProductDetailRepository = mock()
    private val variationRepository: VariationRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any(), any()) } doReturn "copied name"
    }

    lateinit var sut: DuplicateProduct

    @Before
    fun setUp() {
        productDetailRepository.stub {
            on { duplicateProduct(any()) } doReturn Result.failure(WooException(ROUTE_MISSING_ERROR))
        }
        sut = DuplicateProduct(
            productDetailRepository,
            variationRepository,
            resourceProvider,
        )
    }

    @Test
    fun `given core route is missing, when duplicating a product, then legacy product is created with expected properties`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(
                ProductTestUtils.generateProduct().copy(
                    sku = SOURCE_SKU,
                    slug = SOURCE_SLUG,
                    permalink = SOURCE_PERMALINK,
                )
            )
            productDetailRepository.stub {
                on { addProduct(any<ProductAggregate>()) } doReturn Pair(true, 123)
            }

            // WHEN
            val result = sut.invoke(productToDuplicate)

            // THEN
            val duplicationRequestCapture = argumentCaptor<ProductAggregate>()
            verify(productDetailRepository).duplicateProduct(productToDuplicate.remoteId)
            verify(productDetailRepository).addProduct(duplicationRequestCapture.capture())
            assertThat(result).isEqualTo(Result.success(123L))

            assertThat(duplicationRequestCapture.firstValue)
                .matches {
                    it.remoteId == 0L && it.product.name == "copied name" &&
                        it.product.sku == "" && it.product.status == ProductStatus.DRAFT &&
                        it.product.slug == "" && it.product.permalink == ""
                }
                .usingRecursiveComparison()
                .ignoringFields(
                    "product.remoteId",
                    "product.name",
                    "product.sku",
                    "product.status",
                    "product.slug",
                    "product.permalink"
                )
                .isEqualTo(productToDuplicate)
        }

    @Test
    fun `given core duplication succeeds, when duplicating a product, then ID is returned without legacy calls`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(ProductTestUtils.generateProduct().copy(numVariations = 10))
            productDetailRepository.stub {
                on { duplicateProduct(productToDuplicate.remoteId) } doReturn Result.success(DUPLICATED_PRODUCT_ID)
            }

            // WHEN
            val result = sut(productToDuplicate)

            // THEN
            assertThat(result).isEqualTo(Result.success(DUPLICATED_PRODUCT_ID))
            verify(productDetailRepository, never()).addProduct(any<ProductAggregate>())
            verify(variationRepository, never()).fetchProductVariations(any(), any())
            verify(variationRepository, never()).createVariations(any(), any())
        }

    @Test
    fun `given core route is missing and legacy creation fails, when duplicating a product, then failure is returned once`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(ProductTestUtils.generateProduct())
            productDetailRepository.stub {
                on { addProduct(any<ProductAggregate>()) } doReturn Pair(false, 0L)
            }

            // WHEN
            val result = sut(productToDuplicate)

            // THEN
            assertThat(result.isFailure).isTrue()
            verify(productDetailRepository).duplicateProduct(productToDuplicate.remoteId)
            verify(productDetailRepository).addProduct(any<ProductAggregate>())
        }

    @Test
    fun `given core duplication fails for a non-route error, when duplicating a product, then legacy flow is not called`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(ProductTestUtils.generateProduct())
            val endpointError = WooError(
                type = API_NOT_FOUND,
                original = NOT_FOUND,
                apiErrorCode = "woocommerce_rest_product_invalid_id"
            )
            productDetailRepository.stub {
                on { duplicateProduct(productToDuplicate.remoteId) } doReturn
                    Result.failure(WooException(endpointError))
            }

            // WHEN
            val result = sut(productToDuplicate)

            // THEN
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
            assertThat((result.exceptionOrNull() as WooException).error).isEqualTo(endpointError)
            verify(productDetailRepository, never()).addProduct(any<ProductAggregate>())
            verify(variationRepository, never()).fetchProductVariations(any(), any())
            verify(variationRepository, never()).createVariations(any(), any())
        }

    @Test
    fun `given rest no route code has non-not-found type, when duplicating a product, then legacy flow is not called`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(
                ProductTestUtils.generateProduct().copy(numVariations = 10)
            )
            val endpointError = WooError(
                type = API_ERROR,
                original = NETWORK_ERROR,
                apiErrorCode = "rest_no_route"
            )
            productDetailRepository.stub {
                on { duplicateProduct(productToDuplicate.remoteId) } doReturn
                    Result.failure(WooException(endpointError))
            }

            // WHEN
            val result = sut(productToDuplicate)

            // THEN
            assertThat(result.exceptionOrNull()).isInstanceOf(WooException::class.java)
            assertThat((result.exceptionOrNull() as WooException).error).isEqualTo(endpointError)
            verify(productDetailRepository, never()).addProduct(any<ProductAggregate>())
            verify(variationRepository, never()).fetchProductVariations(any(), any())
            verify(variationRepository, never()).createVariations(any(), any())
        }

    @Test
    fun `given product has slug and permalink, when duplicated, then source product slug and permalink are unchanged`() =
        testBlocking {
            // GIVEN
            val productToDuplicate = ProductAggregate(
                ProductTestUtils.generateProduct().copy(
                    slug = SOURCE_SLUG,
                    permalink = SOURCE_PERMALINK,
                )
            )
            productDetailRepository.stub {
                on { addProduct(any<ProductAggregate>()) } doReturn Pair(true, 123L)
            }

            // WHEN
            sut.invoke(productToDuplicate)

            // THEN
            assertThat(productToDuplicate.product.slug).isEqualTo(SOURCE_SLUG)
            assertThat(productToDuplicate.product.permalink).isEqualTo(SOURCE_PERMALINK)
        }

    @Test
    fun `should duplicate a variable product and keep all properties of variations except sku and remoteProductId`() =
        testBlocking {
            // given
            val productToDuplicate = ProductAggregate(ProductTestUtils.generateProduct().copy(numVariations = 15))
            val duplicatedProductId = 456L
            productDetailRepository.stub {
                on { addProduct(any<ProductAggregate>()) } doReturn Pair(true, duplicatedProductId)
            }

            val variationsOfProductToDuplicate =
                ProductTestUtils.generateProductVariationList(productToDuplicate.remoteId)
                    .map { it.copy(sku = "not an empty value") }
            variationRepository.stub {
                on {
                    fetchProductVariations(eq(productToDuplicate.remoteId), any())
                } doReturn variationsOfProductToDuplicate
                on { createVariations(any(), any()) } doReturn Result.success(Unit)
            }

            // when
            sut.invoke(productToDuplicate)

            // then
            val duplicationRequestCapture = argumentCaptor<List<ProductVariation>>()
            verify(variationRepository).createVariations(
                eq(duplicatedProductId),
                duplicationRequestCapture.capture()
            )

            assertThat(duplicationRequestCapture.firstValue)
                .matches { variations ->
                    variations.all { variation ->
                        variation.remoteProductId == duplicatedProductId && variation.sku.isEmpty()
                    }
                }
                .usingRecursiveComparison()
                .ignoringFields("remoteProductId", "sku")
                .isEqualTo(variationsOfProductToDuplicate)
        }

    private companion object {
        const val DUPLICATED_PRODUCT_ID = 123L
        const val SOURCE_SKU = "not an empty value"
        const val SOURCE_SLUG = "acme-water"
        const val SOURCE_PERMALINK = "https://example.com/product/acme-water"
        val ROUTE_MISSING_ERROR = WooError(
            type = API_NOT_FOUND,
            original = NOT_FOUND,
            apiErrorCode = "rest_no_route"
        )
    }
}
