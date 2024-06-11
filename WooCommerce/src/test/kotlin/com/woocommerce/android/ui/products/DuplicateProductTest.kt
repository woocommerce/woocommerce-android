package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
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
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify

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
        sut = DuplicateProduct(
            productDetailRepository,
            variationRepository,
            resourceProvider,
        )
    }

    @Test
    fun `should duplicate a product and set expected properties`() = testBlocking {
        // given
        val productToDuplicate = ProductTestUtils.generateProduct().copy(sku = "not an empty value")
        productDetailRepository.stub {
            onBlocking { addProduct(any()) } doReturn Pair(true, 123)
        }

        // when
        sut.invoke(productToDuplicate)

        // then
        val duplicationRequestCapture = argumentCaptor<Product>()
        verify(productDetailRepository).addProduct(duplicationRequestCapture.capture())

        assertThat(duplicationRequestCapture.firstValue)
            .matches {
                it.remoteId == 0L && it.name == "copied name" && it.sku == "" && it.status == ProductStatus.DRAFT
            }
            .usingRecursiveComparison()
            .ignoringFields("remoteId", "name", "sku", "status")
            .isEqualTo(productToDuplicate)
    }

    @Test
    fun `should duplicate a variable product and keep all properties of variations except sku and remoteProductId`() =
        testBlocking {
            // given
            val productToDuplicate = ProductTestUtils.generateProduct().copy(numVariations = 15)
            val duplicatedProductId = 456L
            productDetailRepository.stub {
                onBlocking { addProduct(any()) } doReturn Pair(true, duplicatedProductId)
            }

            val variationsOfProductToDuplicate =
                ProductTestUtils.generateProductVariationList(productToDuplicate.remoteId)
                    .map { it.copy(sku = "not an empty value") }
            variationRepository.stub {
                onBlocking {
                    fetchProductVariations(eq(productToDuplicate.remoteId), any())
                } doReturn variationsOfProductToDuplicate
                onBlocking { createVariations(any(), any()) } doReturn Result.success(Unit)
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
}
