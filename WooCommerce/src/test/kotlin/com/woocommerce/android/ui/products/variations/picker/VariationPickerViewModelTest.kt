package com.woocommerce.android.ui.products.variations.picker

import androidx.lifecycle.Observer
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.variations.picker.VariationPickerViewModel.VariationListItem
import com.woocommerce.android.ui.products.variations.picker.VariationPickerViewModel.VariationPickerResult
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VariationPickerViewModelTest : BaseUnitTest() {

    // Mocks
    private lateinit var variationListHandler: VariationListHandler
    private lateinit var variationRepository: VariationSelectorRepository

    // Class under test
    private lateinit var viewModel: VariationPickerViewModel

    private val navArgs = VariationPickerFragmentArgs(
        itemId = 1L,
        productId = 123L,
        allowedVatiations = longArrayOf(34L, 56L)
    )

    @Before
    fun setup() {
        // Initialize mocks
        variationListHandler = mock {
            on { getVariationsFlow(any()) } doReturn flowOf(emptyList())
        }

        val productMock = mock<Product>()
        variationRepository = mock {
            on { getProduct(any()) } doReturn productMock
        }

        val savedState = navArgs.toSavedStateHandle()

        // Initialize ViewModel with the mocks
        viewModel = VariationPickerViewModel(savedState, variationListHandler, variationRepository)
    }

    @Test
    fun `viewModel initializes loading state to LOADING and then to IDLE`() = testBlocking {
        val result = VariationPickerViewModel.ViewState(VariationPickerViewModel.LoadingState.IDLE)
        val observer: Observer<VariationPickerViewModel.ViewState> = mock()
        viewModel.viewSate.observeForever(observer)

        advanceTimeBy(100)

        verify(observer).onChanged(result)
    }

    @Test
    fun `onLoadMore() sets loading state to APPENDING and then to IDLE`() = testBlocking {
        val result = VariationPickerViewModel.ViewState(VariationPickerViewModel.LoadingState.IDLE)
        val observer: Observer<VariationPickerViewModel.ViewState> = mock()
        viewModel.viewSate.observeForever(observer)

        viewModel.onLoadMore()
        // Assert
        verify(observer).onChanged(result)
    }

    @Test
    fun `onCancel() triggers Exit event`() {
        // Arrange
        val observer: Observer<MultiLiveEvent.Event> = mock()
        viewModel.event.observeForever(observer)

        // Act
        viewModel.onCancel()

        // Assert
        verify(observer).onChanged(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `onSelectVariation() triggers ExitWithResult event`() {
        // Arrange
        val variation = VariationListItem(
            id = 1L,
            title = "Title",
            imageUrl = null,
            selectedAttributes = emptyList(),
            selectableAttributes = emptyList()
        )
        val observer: Observer<MultiLiveEvent.Event> = mock()
        viewModel.event.observeForever(observer)

        // Act
        viewModel.onSelectVariation(variation)

        // Assert
        val expectedEvent = ExitWithResult(
            VariationPickerResult(
                itemId = navArgs.itemId,
                productId = navArgs.productId,
                variationId = variation.id,
                attributes = variation.attributes
            )
        )
        verify(observer).onChanged(expectedEvent)
    }

    @Test
    fun `given parent product with a non-variation attribute, when variations load, then it is not selectable`() =
        testBlocking {
            // GIVEN
            val parentProduct = ProductTestUtils.generateProduct(productId = navArgs.productId).copy(
                attributes = listOf(
                    ProductAttribute(id = 1L, name = "Color", terms = listOf("Red", "Blue"), isVariation = true),
                    ProductAttribute(id = 0L, name = "Material", terms = listOf("Cotton"), isVariation = false)
                )
            )
            val variation = ProductTestUtils.generateProductVariation(
                productId = navArgs.productId,
                variationId = 34L
            )
            whenever(variationListHandler.getVariationsFlow(any())).thenReturn(flowOf(listOf(variation)))
            whenever(variationRepository.getProduct(any())).thenReturn(parentProduct)
            viewModel = VariationPickerViewModel(
                navArgs.toSavedStateHandle(),
                variationListHandler,
                variationRepository
            )

            // WHEN
            var viewState: VariationPickerViewModel.ViewState? = null
            viewModel.viewSate.observeForever { viewState = it }
            advanceTimeBy(100)

            // THEN
            assertThat(viewState?.variations?.single()?.attributes?.map { it.name }).containsExactly("Color")
        }
}
