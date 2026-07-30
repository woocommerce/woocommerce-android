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
        assertThat(state.hasChanges).isFalse
    }

    @Test
    fun `when the alt text is changed, then the state has changes`() = testBlocking {
        initialize()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
        }.last()

        assertThat(state.altText).isEqualTo("updated alt text")
        assertThat(state.hasChanges).isTrue
    }

    @Test
    fun `given a changed alt text, when done is clicked, then exit with the updated image`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
            viewModel.onDoneClicked()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = STORED_IMAGE.copy(alt = "updated alt text"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `given an unknown alt text, when only the name is changed, then the alt text stays unknown`() = testBlocking {
        initialize(image = STORED_IMAGE.copy(alt = null))

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onNameChanged("updated name")
            viewModel.onDoneClicked()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = STORED_IMAGE.copy(alt = null, name = "updated name"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `when the alt text is cleared, then there is nothing to save and the stored value shows as a placeholder`() =
        testBlocking {
            initialize()

            val state = viewModel.state.runAndCaptureValues {
                viewModel.onAltTextChanged("")
            }.last()

            assertThat(state.hasChanges).isFalse
            assertThat(state.altTextPlaceholder).isEqualTo(STORED_IMAGE.alt)
        }

    @Test
    fun `given a cleared alt text, when the name is changed and done is clicked, then the stored alt text is kept`() =
        testBlocking {
            initialize()

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onAltTextChanged("")
                viewModel.onNameChanged("updated name")
                viewModel.onDoneClicked()
            }.last()

            assertThat(event).isEqualTo(
                MultiLiveEvent.Event.ExitWithResult(
                    data = STORED_IMAGE.copy(name = "updated name"),
                    key = KEY_IMAGE_DETAILS_RESULT
                )
            )
        }

    @Test
    fun `given no changes, when back is clicked, then exit without a result`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onBackClick()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given changes, when back is clicked, then a discard changes dialog is shown`() = testBlocking {
        initialize()

        val state = viewModel.state.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
            viewModel.onBackClick()
        }.last()

        assertThat(state.discardChangesDialogState).isNotNull
    }

    @Test
    fun `given a discard changes dialog, when discard is clicked, then exit without a result`() = testBlocking {
        initialize()

        val dialogState = viewModel.state.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
            viewModel.onBackClick()
        }.last().discardChangesDialogState!!

        val event = viewModel.event.runAndCaptureValues {
            dialogState.onDiscard()
        }.last()

        assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
    }

    @Test
    fun `given a discard changes dialog, when cancel is clicked, then the dialog is dismissed`() = testBlocking {
        initialize()

        val dialogState = viewModel.state.runAndCaptureValues {
            viewModel.onAltTextChanged("updated alt text")
            viewModel.onBackClick()
        }.last().discardChangesDialogState!!

        val state = viewModel.state.runAndCaptureValues {
            dialogState.onCancel()
        }.last()

        assertThat(state.discardChangesDialogState).isNull()
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
