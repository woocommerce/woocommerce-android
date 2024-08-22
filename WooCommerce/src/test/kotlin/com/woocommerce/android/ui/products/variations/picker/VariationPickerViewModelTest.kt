package com.woocommerce.android.ui.products.variations.picker

import androidx.lifecycle.Observer
import com.woocommerce.android.ui.products.variations.selector.VariationListHandler
import com.woocommerce.android.ui.products.variations.selector.VariationSelectorRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

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
            onBlocking { getVariationsFlow(any()) } doReturn flowOf(emptyList())
        }

        variationRepository = mock {

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
        val variation = VariationPickerViewModel.VariationListItem(1L, "Title", null, emptyList())
        val observer: Observer<MultiLiveEvent.Event> = mock()
        viewModel.event.observeForever(observer)

        // Act
        viewModel.onSelectVariation(variation)

        // Assert
        val expectedEvent = MultiLiveEvent.Event.ExitWithResult(
            VariationPickerViewModel.VariationPickerResult(
                itemId = navArgs.itemId,
                productId = navArgs.productId,
                variationId = variation.id,
                attributes = variation.attributes
            )
        )
        verify(observer).onChanged(expectedEvent)
    }
}
