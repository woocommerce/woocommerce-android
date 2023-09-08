package com.woocommerce.android.ui.products.variations.attributes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductAttributeTerm
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AddAttributeTermsViewModelTest {
    private lateinit var sut: AddAttributeTermsViewModel
    private lateinit var termsListHandler: AttributeTermsListHandler

    private val defaultAttributeId = 123L

    @Before
    fun setUp() {
        termsListHandler = mock {
            onBlocking { fetchAttributeTerms(defaultAttributeId) } doReturn defaultAttributeList
            onBlocking { loadMore(defaultAttributeId) } doReturn defaultLoadMoreList
        }

        sut = AddAttributeTermsViewModel(
            savedState = SavedStateHandle(),
            termsListHandler = termsListHandler
        )
    }

    @Test
    fun `when onFetchAttributeTerms is called, then termsListState should be updated`() {
    }

    @Test
    fun `when onLoadMore is called, then termsListState should be updated by appending new items`() {
    }

    @Test
    fun `when onLoadMore is called and returns repeated results, then termsListState should filter them out`() {
    }

    @Test
    fun `when resetGlobalAttributeTerms is called, then termsListState should be empty`() {
    }

    @Test
    fun `when onFetchAttributeTerms is called, then loadingState should be updated as expected`() {
    }

    @Test
    fun `when onLoadMore is called, then loadingState should be updated as expected`() {
    }

    private val defaultAttributeList = generateAttributeList(from = 1, to = 10)

    private val defaultLoadMoreList = generateAttributeList(from = 10, to = 16)

    private fun generateAttributeList(from: Int, to: Int): List<ProductAttributeTerm> {
        return (from..to).map { index ->
            ProductAttributeTerm(
                id = index,
                remoteId = index,
                name = "Term $index",
                slug = "term-$index",
                description = "Term $index description"
            )
        }
    }
}
