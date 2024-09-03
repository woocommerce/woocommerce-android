package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Component
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.QueryType
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class GetComponentOptionsTest : BaseUnitTest() {

    private val getProductsByIds: GetProductsByIds = mock()
    private val getCategoriesByIds: GetCategoriesByIds = mock()
    private val productRepository: ProductDetailRepository = mock()

    lateinit var sut: GetComponentOptions

    @Before
    fun setUp() {
        sut = GetComponentOptions(
            getProductsByIds = getProductsByIds,
            getCategoriesByIds = getCategoriesByIds,
            repository = productRepository,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when component type is PRODUCT, then fetch products`() = testBlocking {
        val component = sampleComponent.copy(queryType = QueryType.PRODUCT)
        val products = listOf(sampleProduct)
        whenever(getProductsByIds(component.queryIds)).doReturn(products)

        val result = sut.invoke(component)

        verify(getProductsByIds).invoke(component.queryIds)
        verify(getCategoriesByIds, never()).invoke(component.queryIds)
        assertThat(result.options.size).isEqualTo(products.size)
        result.options.forEach { option ->
            assertThat(option.shouldDisplayImage).isTrue()
        }
    }

    @Test
    fun `when component type is CATEGORY, then fetch categories`() = testBlocking {
        val component = sampleComponent.copy(queryType = QueryType.CATEGORY)
        val categories = listOf(sampleCategory, anotherCategory)
        whenever(getCategoriesByIds(component.queryIds)).doReturn(categories)

        val result = sut.invoke(component)

        verify(getCategoriesByIds).invoke(component.queryIds)
        verify(getProductsByIds, never()).invoke(component.queryIds)
        assertThat(result.options.size).isEqualTo(categories.size)
        result.options.forEach { option ->
            assertThat(option.shouldDisplayImage).isFalse()
        }
    }

    @Test
    fun `when default option is not null, then fetch the default option`() = testBlocking {
        val defaultId = 2L
        val defaultName = sampleProduct.name
        val component = sampleComponent.copy(
            queryType = QueryType.PRODUCT,
            defaultOptionId = defaultId
        )
        whenever(getProductsByIds(component.queryIds)).doReturn(emptyList())
        whenever(productRepository.fetchAndGetProduct(defaultId)).doReturn(sampleProduct)

        val result = sut.invoke(component)

        verify(productRepository).fetchAndGetProduct(defaultId)
        assertThat(result.default).isEqualTo(defaultName)
    }

    private val queryIds = listOf(1L, 2L, 3L)
    private val sampleComponent = Component(
        id = 0L,
        title = "component",
        description = "sample component",
        queryType = QueryType.PRODUCT,
        queryIds = queryIds,
        defaultOptionId = null,
        thumbnailUrl = null
    )
    private val sampleProduct = ProductTestUtils.generateProduct()
    private val sampleCategory = ProductCategory(
        remoteCategoryId = 4L,
        name = "sample category"
    )
    private val anotherCategory = ProductCategory(
        remoteCategoryId = 8L,
        name = "another category"
    )
}
