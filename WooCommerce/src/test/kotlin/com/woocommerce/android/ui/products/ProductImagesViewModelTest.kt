package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product.Image
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

    private fun savedState(productImages: List<Image>) = spy(
            SavedStateWithArgs(
                    SavedStateHandle(),
                    arguments = null,
                    defaultArgs = ProductImagesFragmentArgs(
                            remoteId = 0,
                            images = productImages.toTypedArray(),
                            selectedImage = null,
                            showChooser = false,
                            requestCode = 123
                    )
            )
    )

    private fun initialize(productImages: List<Image> = generateProductImagesList()) {
        viewModel = ProductImagesViewModel(
                networkStatus,
                productImagesServiceWrapper,
                savedState(productImages),
                coroutinesTestRule.testDispatchers
        ).apply {
            viewStateData.observeForever { _, _ -> }
        }
    }

    @Test
    fun `Sets drag state when drag starts`() {
        initialize()

        viewModel.onDragStarted()

        observeState { state ->
            assertThat(state.productImagesState).isEqualTo(ProductImagesState.DRAGGING)
        }
    }

    @Test
    fun `Trigger exit event on done button clicked when in browsing state`() {
        val images = generateProductImagesList()
        initialize(images)

        viewModel.onDoneButtonClicked()

        observeEvents { event ->
            assertThat(event).isEqualTo(ExitWithResult(images))
        }
    }

    @Test
    fun `Exit dragging state on done button clicked when in dragging state`() {
        initialize()

        viewModel.onDragStarted()
        viewModel.onDoneButtonClicked()

        observeState { state ->
            assertThat(state.productImagesState).isEqualTo(ProductImagesState.BROWSING)
        }
    }

    @Test
    fun `Show done button when view is in dragging state even there are no changes to the list`() {
        initialize()

        viewModel.onDragStarted()

        observeState { state ->
            assertThat(state.isDoneButtonVisible).isEqualTo(true)
        }
    }

    @Test
    fun `Request image delete confirmation on image delete button clicked`() {
        val images = generateProductImagesList()
        initialize(images)
        val imageToDelete = images.first()

        viewModel.onGalleryImageDeleteIconClicked(imageToDelete)

        observeEvents { event ->
            assertThat(event).isEqualTo(ShowDeleteImageConfirmation(imageToDelete))
        }
    }

    @Test
    fun `Remove image on remove confirmation`() {
        val images = generateProductImagesList()
        initialize(images)
        val imageToDelete = images.first()

        viewModel.onDeleteImageConfirmed(imageToDelete)

        observeState { state ->
            assertThat(state.images).doesNotContain(imageToDelete)
        }
    }

    @Test
    fun `Update list state on image reorder`() {
        val images = generateProductImagesList()
        initialize(images)
        val imageA = images.first()

        viewModel.onGalleryImageMoved(
                from = images.indexOf(imageA),
                to = images.lastIndex
        )

        observeState { state ->
            assertThat(state.images).contains(imageA, Index.atIndex(images.lastIndex))
        }
    }

    @Test
    fun `Show drag and drop description where there are more than one images`() {
        val twoImagesList = generateProductImagesList().subList(0, 2)
        initialize(twoImagesList)

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isTrue()
        }
    }

    @Test
    fun `Hide drag and drop description where there is one image only`() {
        val oneImageList = generateProductImagesList().subList(0, 1)
        initialize(oneImageList)

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isFalse()
        }
    }

    @Test
    fun `Show drag and drop description when in dragging state even if there is one image only`() {
        val twoImagesList = generateProductImagesList().subList(0, 2)
        initialize(twoImagesList)

        viewModel.onDragStarted()
        viewModel.onDeleteImageConfirmed(twoImagesList.first())

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isTrue()
        }
    }

    private fun observeState(check: (ProductImagesViewModel.ViewState) -> Unit) =
            viewModel.viewStateData.liveData.observeForever { check(it) }

    private fun observeEvents(check: (Event) -> Unit) =
            viewModel.event.observeForever { check(it) }
}
