package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AttributeTermsListHandlerTest: BaseUnitTest() {
    private lateinit var sut: AttributeTermsListHandler
    private lateinit var repository: ProductDetailRepository

    private val defaultPageSize = 10
    private val defaultAttributeId = 123L

    @Before
    fun setUp() {
        repository = mock {
            onBlocking {
                fetchGlobalAttributeTerms(defaultAttributeId, 1, defaultPageSize)
            } doReturn defaultAttributeList

            onBlocking {
                fetchGlobalAttributeTerms(defaultAttributeId, 2, defaultPageSize)
            } doReturn defaultLoadMoreList
        }
        sut = AttributeTermsListHandler(
            repository = repository
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
}
