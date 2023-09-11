package com.woocommerce.android.ui.products.variations.attributes

import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Before
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
}
