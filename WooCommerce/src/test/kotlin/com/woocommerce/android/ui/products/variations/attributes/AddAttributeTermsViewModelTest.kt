package com.woocommerce.android.ui.products.variations.attributes

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ProductAttributeTerm
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class AddAttributeTermsViewModelTest: BaseUnitTest() {
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
    fun `when onFetchAttributeTerms is called, then termsListState should be updated`() = testBlocking {
        // Given
        val termListUpdates = mutableListOf<List<ProductAttributeTerm>>()
        sut.termsListState.observeForever { termListUpdates.add(it) }

        // When
        sut.onFetchAttributeTerms(defaultAttributeId)

        // Then
        assertThat(termListUpdates).containsExactly(
            emptyList(),
            defaultAttributeList
        )
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
