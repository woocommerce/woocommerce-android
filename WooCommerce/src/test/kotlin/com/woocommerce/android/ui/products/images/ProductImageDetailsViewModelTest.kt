package com.woocommerce.android.ui.products.images

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.images.ProductImageDetailsViewModel.Companion.KEY_IMAGE_DETAILS_RESULT
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ProductImageDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductImageDetailsViewModel

    private fun initialize(image: Product.Image = STORED_IMAGE) {
        viewModel = ProductImageDetailsViewModel(
            savedStateHandle = ProductImageDetailsFragmentArgs(image = image).toSavedStateHandle()
        )
    }

    @Test
    fun `when initialized, then the stored image details are shown`() = testBlocking {
        initialize()

        val state = viewModel.state.runAndCaptureValues { }.last()

        assertThat(state.imageUrl).isEqualTo(STORED_IMAGE.source)
        assertThat(state.altText).isEqualTo(STORED_IMAGE.alt)
        assertThat(state.name).isEqualTo(STORED_IMAGE.name)
    }

    @Test
    fun `given a changed alt text, when leaving the screen, then exit with the updated image`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = STORED_IMAGE.copy(alt = "updated alt text"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `given no changes, when leaving the screen, then exit without a result`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given an unknown alt text, when only the name is changed, then the alt text stays unknown`() = testBlocking {
        initialize(image = STORED_IMAGE.copy(alt = null))

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNameChanged("updated name")
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = STORED_IMAGE.copy(alt = null, name = "updated name"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `when the alt text is cleared, then the stored value shows as a placeholder with a notice`() = testBlocking {
        initialize()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onAltTextChanged("")
        }.last()

        assertThat(state.altTextPlaceholder).isEqualTo(STORED_IMAGE.alt)
        assertThat(state.isAltTextRemovalBlocked).isTrue
    }

    @Test
    fun `given a cleared alt text, when leaving the screen, then the stored alt text is kept`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAltTextChanged("")
            viewModel.onNameChanged("updated name")
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = STORED_IMAGE.copy(name = "updated name"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `given only a cleared alt text, when leaving the screen, then exit without a result`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAltTextChanged("")
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    private companion object {
        val STORED_IMAGE = Product.Image(
            id = 1L,
            name = "black-tee",
            alt = "A black t-shirt",
            source = "https://example.com/image.jpg",
            dateCreated = null,
            isCoverImage = false
        )
    }
}
