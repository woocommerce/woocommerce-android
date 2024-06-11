package com.woocommerce.android.ui.products.images

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Product
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Index
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class ProductImagesViewModelTest : BaseUnitTest() {
    lateinit var viewModel: ProductImagesViewModel

    private val networkStatus: NetworkStatus = mock()
    private val mediaFileUploadHandler: MediaFileUploadHandler = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()

    private fun savedState(productImages: List<Product.Image>) = ProductImagesFragmentArgs(
        remoteId = 0,
        images = productImages.toTypedArray(),
        selectedImage = null,
        showChooser = false,
        requestCode = 123
    ).toSavedStateHandle()

    private fun initialize(productImages: List<Product.Image> = ProductTestUtils.generateProductImagesList()) {
        viewModel = ProductImagesViewModel(
            networkStatus,
            mediaFileUploadHandler,
            resourceProvider,
            analyticsTracker,
            savedState(productImages)
        ).apply {
            viewStateData.observeForever { _, _ -> }
        }
    }

    @Test
    fun `Sets drag state when drag starts`() {
        initialize()

        viewModel.onGalleryImageDragStarted()

        observeState { state ->
            assertThat(state.productImagesState is ProductImagesViewModel.ProductImagesState.Dragging).isTrue()
        }
    }

    @Test
    fun `Trigger exit event on back button clicked when in browsing state`() {
        val images = ProductTestUtils.generateProductImagesList()
        initialize(images)

        viewModel.onNavigateBackButtonClicked()

        observeEvents { event ->
            assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
        }
    }

    @Test
    fun `Trigger exitWithResult event on back button clicked when in browsing state`() {
        initialize()

        val images = ProductTestUtils.generateProductImagesList()
        viewModel.onDeleteImageConfirmed(images[0])
        viewModel.onNavigateBackButtonClicked()

        observeEvents { event ->
            assertThat(event).isEqualTo(MultiLiveEvent.Event.ExitWithResult(images - images[0]))
        }
    }

    @Test
    fun `Request image delete confirmation on image delete button clicked`() {
        val images = ProductTestUtils.generateProductImagesList()
        initialize(images)
        val imageToDelete = images.first()

        viewModel.onGalleryImageDeleteIconClicked(imageToDelete)

        observeEvents { event ->
            assertThat(event).isEqualTo(ProductImagesViewModel.ShowDeleteImageConfirmation(imageToDelete))
        }
    }

    @Test
    fun `Remove image on remove confirmation`() {
        val images = ProductTestUtils.generateProductImagesList()
        initialize(images)
        val imageToDelete = images.first()

        viewModel.onDeleteImageConfirmed(imageToDelete)

        observeState { state ->
            assertThat(state.images).doesNotContain(imageToDelete)
        }
    }

    @Test
    fun `Update list state on image reorder`() {
        val images = ProductTestUtils.generateProductImagesList()
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
        val twoImagesList = ProductTestUtils.generateProductImagesList().subList(0, 2)
        initialize(twoImagesList)

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isTrue()
        }
    }

    @Test
    fun `Hide drag and drop description where there is one image only`() {
        val oneImageList = ProductTestUtils.generateProductImagesList().subList(0, 1)
        initialize(oneImageList)

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isFalse()
        }
    }

    @Test
    fun `Show drag and drop description when in dragging state even if there is one image only`() {
        val twoImagesList = ProductTestUtils.generateProductImagesList().subList(0, 2)
        initialize(twoImagesList)

        viewModel.onGalleryImageDragStarted()
        viewModel.onDeleteImageConfirmed(twoImagesList.first())

        observeState { state ->
            assertThat(state.isDragDropDescriptionVisible).isTrue()
        }
    }

    @Test
    fun `Validate drag and drop process on validation button clicked`() {
        val images = ProductTestUtils.generateProductImagesList()
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
        val images = ProductTestUtils.generateProductImagesList()
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

    @Test
    fun `Send tracks event upon exit if there was no changes`() {
        // given
        initialize()

        // when
        viewModel.onNavigateBackButtonClicked()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to false)
        )
    }

    @Test
    fun `Send tracks event upon exit if there was a change`() {
        // given
        initialize(emptyList())

        // when
        viewModel.onMediaLibraryImagesAdded(ProductTestUtils.generateProductImagesList())
        viewModel.onNavigateBackButtonClicked()

        // then
        verify(analyticsTracker).track(
            AnalyticsEvent.PRODUCT_IMAGE_SETTINGS_DONE_BUTTON_TAPPED,
            mapOf(AnalyticsTracker.KEY_HAS_CHANGED_DATA to true)
        )
    }

    private fun observeState(check: (ProductImagesViewModel.ViewState) -> Unit) =
        viewModel.viewStateData.liveData.observeForever { check(it) }

    private fun observeEvents(check: (MultiLiveEvent.Event) -> Unit) =
        viewModel.event.observeForever { check(it) }
}
