package com.woocommerce.android.ui.products

import androidx.lifecycle.SavedStateHandle
import com.nhaarman.mockitokotlin2.spy
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.Product.Image
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.products.ProductImagesViewModel.ProductImagesState.Dragging
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

        viewModel.onGalleryImageDragStarted()

        observeState { state ->
            assertThat(state.productImagesState is Dragging).isTrue()
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

        viewModel.onGalleryImageDragStarted()
        viewModel.onDeleteImageConfirmed(twoImagesList.first())

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isTrue()
        }
    }

    @Test
    fun `Validate drag and drop process on validation button clicked`() {
        val images = generateProductImagesList()
        val imageToRemove = images[3]
        val imageToReorder = images[1]
        initialize(images)

        viewModel.onGalleryImageDragStarted()
        viewModel.onGalleryImageMoved(
                from = images.indexOf(imageToReorder),
                to = 2
        )
        viewModel.onDeleteImageConfirmed(imageToRemove)
        viewModel.onValidateButtonClicked()

        observeState { state ->
            assertThat(state.images).doesNotContain(imageToRemove)
            assertThat(state.images).contains(imageToReorder, Index.atIndex(2))
        }
    }

    @Test
    fun `Revert to initial images when in dragging and exit button clicked`() {
        val images = generateProductImagesList()
        val imageToRemove = images[3]
        val imageToReorder = images[1]
        initialize(images)

        viewModel.onGalleryImageDragStarted()
        viewModel.onGalleryImageMoved(
                from = images.indexOf(imageToReorder),
                to = 2
        )
        viewModel.onDeleteImageConfirmed(imageToRemove)
        viewModel.onNavigateBackButtonClicked()

        observeState { state ->
            assertThat(state.images).isEqualTo(images)
        }
    }

    private fun observeState(check: (ProductImagesViewModel.ViewState) -> Unit) =
            viewModel.viewStateData.liveData.observeForever { check(it) }

    private fun observeEvents(check: (Event) -> Unit) =
            viewModel.event.observeForever { check(it) }
}
