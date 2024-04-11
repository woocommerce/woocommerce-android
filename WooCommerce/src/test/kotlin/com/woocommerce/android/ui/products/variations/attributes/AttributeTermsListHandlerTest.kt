package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AttributeTermsListHandlerTest : BaseUnitTest() {
    private lateinit var sut: AttributeTermsListHandler
    private lateinit var repository: ProductDetailRepository

    private val defaultPageSize = 10
    private val defaultAttributeId = 123L

    @Before
    fun setUp() {
        startSutWith(
            attributesFirstPage = defaultAttributeList,
            attributesSecondPage = defaultLoadMoreList
        )
    }

    @Test
    fun `when fetchAttributeTerms is called, then it should return the first page of attribute terms`() = testBlocking {
        // When
        val result = sut.fetchAttributeTerms(defaultAttributeId)

        // Then
        assertThat(result).isEqualTo(defaultAttributeList)
    }

    @Test
    fun `when loadMore is called, then it should return the second page of attribute terms`() = testBlocking {
        // When
        sut.fetchAttributeTerms(defaultAttributeId)
        val result = sut.loadMore(defaultAttributeId)

        // Then
        assertThat(result).isEqualTo(defaultLoadMoreList)
    }

    @Test
    fun `when loadMore is called with no further terms to fetch, then return a empty list`() = testBlocking {
        // Given
        startSutWith(
            attributesFirstPage = generateAttributeList(from = 1, to = 9),
            attributesSecondPage = generateAttributeList(from = 20, to = 21)
        )

        // When
        sut.fetchAttributeTerms(defaultAttributeId)
        val result = sut.loadMore(defaultAttributeId)

        // Then
        assertThat(result).isEmpty()
    }

    @Test
    fun `when a new fetch request is sent, then should ignore last loadMore call result`() = testBlocking {
        // Given
        val termsFirstPage = generateAttributeList(from = 1, to = 9)
        startSutWith(
            attributesFirstPage = termsFirstPage,
            attributesSecondPage = generateAttributeList(from = 20, to = 21)
        )

        // When
        sut.fetchAttributeTerms(defaultAttributeId)
        sut.loadMore(defaultAttributeId)
        val result = sut.fetchAttributeTerms(defaultAttributeId)

        // Then
        assertThat(result).isEqualTo(termsFirstPage)
    }

    private fun startSutWith(
        attributesFirstPage: List<ProductAttributeTerm>,
        attributesSecondPage: List<ProductAttributeTerm>
    ) {
        repository = mock {
            onBlocking {
                fetchGlobalAttributeTerms(defaultAttributeId, 1, defaultPageSize)
            } doReturn attributesFirstPage

            onBlocking {
                fetchGlobalAttributeTerms(defaultAttributeId, 2, defaultPageSize)
            } doReturn attributesSecondPage
        }
        sut = AttributeTermsListHandler(
            repository = repository
        )
    }
}
