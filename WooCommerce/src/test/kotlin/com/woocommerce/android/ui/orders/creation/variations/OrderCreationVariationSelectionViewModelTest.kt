package com.woocommerce.android.ui.orders.creation.variations

import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.variations.VariationRepository
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.observeForTesting
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class OrderCreationVariationSelectionViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_ID = 1L
    }

    private val productDetailRepository: ProductDetailRepository = mock {
        on { getProduct(PRODUCT_ID) } doReturn ProductTestUtils.generateProduct(PRODUCT_ID)
    }
    private val variationsRepository: VariationRepository = mock {
        on { getProductVariationList(PRODUCT_ID) } doReturn ProductTestUtils.generateProductVariationList(PRODUCT_ID)
        onBlocking { fetchProductVariations(PRODUCT_ID) } doReturn
            ProductTestUtils.generateProductVariationList(PRODUCT_ID)
    }
    private val savedState = OrderCreationVariationSelectionFragmentArgs(PRODUCT_ID).initSavedStateHandle()

    private lateinit var viewModel: OrderCreationVariationSelectionViewModel

    @Before
    fun setup() {
        viewModel = OrderCreationVariationSelectionViewModel(
            savedStateHandle = savedState,
            productRepository = productDetailRepository,
            variationRepository = variationsRepository,
            dispatchers = coroutinesTestRule.testDispatchers
        )
    }

    @Test
    fun `when variations are loaded, then load cached variations`() = testBlocking {
        viewModel.viewState.getOrAwaitValue()

        verify(variationsRepository).getProductVariationList(PRODUCT_ID)
    }

    @Test
    fun `given there is no cached variation, when variations are loaded, then show skeleton`() = testBlocking {
        whenever(variationsRepository.fetchProductVariations(PRODUCT_ID)).doSuspendableAnswer {
            // An artificial delay to make sure we capture the cached values before the network ones
            delay(500)
            ProductTestUtils.generateProductVariationList(PRODUCT_ID)
        }
        whenever(variationsRepository.getProductVariationList(PRODUCT_ID)).thenReturn(emptyList())

        assertThat(viewModel.viewState.getOrAwaitValue().isSkeletonShown).isTrue()
    }

    @Test
    fun `given there are cached variation, when variations are loaded, then don't show skeleton`() = testBlocking {
        whenever(variationsRepository.fetchProductVariations(PRODUCT_ID)).doSuspendableAnswer {
            // An artificial delay to make sure we capture the cached values before the network ones
            delay(500)
            ProductTestUtils.generateProductVariationList(PRODUCT_ID)
        }
        whenever(variationsRepository.getProductVariationList(PRODUCT_ID))
            .thenReturn(ProductTestUtils.generateProductVariationList(PRODUCT_ID))

        assertThat(viewModel.viewState.getOrAwaitValue().isSkeletonShown).isFalse()
    }

    @Test
    fun `when variations are loaded, then fetch network variations`() = testBlocking {
        viewModel.viewState.getOrAwaitValue()

        verify(variationsRepository).fetchProductVariations(PRODUCT_ID)
    }

    @Test
    fun `when loading variations, then exclude any variations without price`() = testBlocking {
        val variations = ProductTestUtils.generateProductVariationList(PRODUCT_ID)
            .mapIndexed { index, productVariation ->
                if (index == 0) productVariation.copy(price = null) else productVariation
            }
        whenever(variationsRepository.getProductVariationList(PRODUCT_ID))
            .thenReturn(variations)

        val displayedVariations = viewModel.viewState.getOrAwaitValue().variationsList
        assertThat(displayedVariations).allMatch { it.price != null }
        assertThat(displayedVariations?.size).isEqualTo(variations.size - 1)
    }

    @Test
    fun `when fetching variations, then exclude any variations without price`() = testBlocking {
        val variations = ProductTestUtils.generateProductVariationList(PRODUCT_ID)
            .mapIndexed { index, productVariation ->
                if (index == 0) productVariation.copy(price = null) else productVariation
            }
        whenever(variationsRepository.fetchProductVariations(PRODUCT_ID))
            .thenReturn(variations)

        var displayedVariations: List<ProductVariation>? = null
        viewModel.viewState.observeForever {
            displayedVariations = it.variationsList
        }

        assertThat(displayedVariations).allMatch { it.price != null }
        assertThat(displayedVariations?.size).isEqualTo(variations.size - 1)
    }

    @Test
    fun `when loading more is request, then fetch more variations`() = testBlocking {
        whenever(variationsRepository.canLoadMoreProductVariations).thenReturn(true)

        viewModel.viewState.observeForTesting {
            viewModel.onLoadMore()
        }

        verify(variationsRepository).fetchProductVariations(PRODUCT_ID, true)
    }
}
