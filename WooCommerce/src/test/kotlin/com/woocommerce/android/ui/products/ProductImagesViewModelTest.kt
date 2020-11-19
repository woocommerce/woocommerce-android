package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState
import com.woocommerce.android.ui.products.ProductImagesViewModel.ShowDeleteImageConfirmation
import com.woocommerce.android.ui.products.ProductTestUtils.generateProductImagesList
import com.woocommerce.android.util.CoroutineTestRule
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ProductImagesViewModelTest : BaseUnitTest() {
    @get:Rule
    val coroutinesTestRule = CoroutineTestRule()

    lateinit var viewModel: ProductImagesViewModel

    @Mock
    lateinit var networkStatus: NetworkStatus

    @Mock
    lateinit var productImagesServiceWrapper: ProductImagesServiceWrapper

    private val productImages = generateProductImagesList()

    private val savedState: SavedStateWithArgs = spy(
            SavedStateWithArgs(
                    SavedStateHandle(),
                    arguments = null,
                    defaultArgs = ProductImagesFragmentArgs(
                            0,
                            productImages.toTypedArray(),
                            null,
                            false,
                            123
                    )
            )
    )

    @Before
    fun setUp() {
        viewModel = ProductImagesViewModel(
                networkStatus,
                productImagesServiceWrapper,
                savedState,
                coroutinesTestRule.testDispatchers
        ).apply {
            viewStateData.observeForever { _, _ -> }
        }
    }

    @Test
    fun `Sets drag state when drag starts`() {
        viewModel.onDragStarted()

        observeState { state ->
            assertThat(state.productImagesState).isEqualTo(ProductImagesState.DRAGGING)
        }
    }

    @Test
    fun `Trigger exit event on done button clicked when in browsing state`() {
        viewModel.onDoneButtonClicked()

        observeEvents { event ->
            assertThat(event).isEqualTo(ExitWithResult(productImages))
        }
    }

    @Test
    fun `Exit dragging state on done button clicked when in dragging state`() {
        viewModel.onDragStarted()
        viewModel.onDoneButtonClicked()

        observeState { state ->
            assertThat(state.productImagesState).isEqualTo(ProductImagesState.BROWSING)
        }
    }

    @Test
    fun `Show done button when view is in dragging state even there are no changes to the list`() {
        viewModel.onDragStarted()

        observeState { state ->
            assertThat(state.isDoneButtonVisible).isEqualTo(true)
        }
    }

    @Test
    fun `Request image delete confirmation on image delete button clicked`() {
        val imageToDelete = productImages.first()
        viewModel.onGalleryImageDeleteIconClicked(imageToDelete)

        observeEvents { event ->
            assertThat(event).isEqualTo(ShowDeleteImageConfirmation(imageToDelete))
        }
    }

    @Test
    fun `Remove image on remove confirmation`() {
        val imageToDelete = productImages.first()
        viewModel.onDeleteImageConfirmed(imageToDelete)

        observeState { state ->
            assertThat(state.images).doesNotContain(imageToDelete)
        }
    }

    @Test
    fun `Update list state on image reorder`() {
        val imageA = productImages.first()
        viewModel.onGalleryImageMoved(
                from = productImages.indexOf(imageA),
                to = productImages.lastIndex
        )

        observeState { state ->
            assertThat(state.images).contains(imageA, Index.atIndex(productImages.lastIndex))
        }
    }

    private fun observeState(check: (ProductImagesViewModel.ViewState) -> Unit) =
            viewModel.viewStateData.liveData.observeForever { check(it) }

    private fun observeEvents(check: (Event) -> Unit) =
            viewModel.event.observeForever { check(it) }
}
