package com.woocommerce.android.ui.products.images

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.images.ProductImageDetailsViewModel.Companion.KEY_IMAGE_DETAILS_RESULT
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ProductImageDetailsViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: ProductImageDetailsViewModel

    private fun initialize(
        draftImage: Product.Image = SERVER_IMAGE,
        serverImage: Product.Image? = SERVER_IMAGE
    ) {
        val productImagesRepository: ProductImagesRepository = mock {
            on { getProduct(REMOTE_PRODUCT_ID) } doReturn serverImage?.let {
                ProductTestUtils.generateProduct(productId = REMOTE_PRODUCT_ID).copy(images = listOf(it))
            }
        }
        viewModel = ProductImageDetailsViewModel(
            savedStateHandle = ProductImageDetailsFragmentArgs(
                image = draftImage,
                remoteProductId = REMOTE_PRODUCT_ID
            ).toSavedStateHandle(),
            productImagesRepository = productImagesRepository
        )
    }

    @Test
    fun `when initialized, then the draft image details are shown`() = testBlocking {
        initialize(draftImage = SERVER_IMAGE.copy(alt = "unsaved alt text"))

        val state = viewModel.state.runAndCaptureValues { }.last()

        assertThat(state.imageUrl).isEqualTo(SERVER_IMAGE.source)
        assertThat(state.altText).isEqualTo("unsaved alt text")
        assertThat(state.name).isEqualTo(SERVER_IMAGE.name)
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
                data = SERVER_IMAGE.copy(alt = "updated alt text"),
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
    fun `given an image the site doesn't have, when only the name is changed, then the alt text stays unknown`() =
        testBlocking {
            initialize(draftImage = SERVER_IMAGE.copy(alt = null), serverImage = null)

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onNameChanged("updated name")
                viewModel.onExit()
            }.last()

            assertThat(event).isEqualTo(
                MultiLiveEvent.Event.ExitWithResult(
                    data = SERVER_IMAGE.copy(alt = null, name = "updated name"),
                    key = KEY_IMAGE_DETAILS_RESULT
                )
            )
        }

    @Test
    fun `given a stored alt text, when the field is cleared, then removal is blocked with the stored value hinted`() =
        testBlocking {
            initialize()

            val state = viewModel.state.runAndCaptureValues {
                viewModel.onAltTextChanged("")
            }.last()

            assertThat(state.altTextPlaceholder).isEqualTo(SERVER_IMAGE.alt)
            assertThat(state.isAltTextRemovalBlocked).isTrue
        }

    @Test
    fun `given a stored alt text, when leaving with a cleared field, then the stored value is kept`() = testBlocking {
        initialize()

        val event = viewModel.event.runAndCaptureValues {
            viewModel.onAltTextChanged("")
            viewModel.onNameChanged("updated name")
            viewModel.onExit()
        }.last()

        assertThat(event).isEqualTo(
            MultiLiveEvent.Event.ExitWithResult(
                data = SERVER_IMAGE.copy(name = "updated name"),
                key = KEY_IMAGE_DETAILS_RESULT
            )
        )
    }

    @Test
    fun `given an empty stored alt text, when an unsaved edit is cleared, then removal is not blocked`() =
        testBlocking {
            initialize(
                draftImage = SERVER_IMAGE.copy(alt = "unsaved alt text"),
                serverImage = SERVER_IMAGE.copy(alt = "")
            )

            val state = viewModel.state.runAndCaptureValues {
                viewModel.onAltTextChanged("")
            }.last()

            assertThat(state.isAltTextRemovalBlocked).isFalse
            assertThat(state.altTextPlaceholder).isEmpty()
        }

    @Test
    fun `given an empty stored alt text, when leaving with a cleared unsaved edit, then the draft is reverted`() =
        testBlocking {
            initialize(
                draftImage = SERVER_IMAGE.copy(alt = "unsaved alt text"),
                serverImage = SERVER_IMAGE.copy(alt = "")
            )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onAltTextChanged("")
                viewModel.onExit()
            }.last()

            assertThat(event).isEqualTo(
                MultiLiveEvent.Event.ExitWithResult(
                    data = SERVER_IMAGE.copy(alt = ""),
                    key = KEY_IMAGE_DETAILS_RESULT
                )
            )
        }

    @Test
    fun `given an empty stored alt text, when a value is typed and cleared, then exit without a result`() =
        testBlocking {
            initialize(
                draftImage = SERVER_IMAGE.copy(alt = ""),
                serverImage = SERVER_IMAGE.copy(alt = "")
            )

            val event = viewModel.event.runAndCaptureValues {
                viewModel.onAltTextChanged("temporary value")
                viewModel.onAltTextChanged("")
                viewModel.onExit()
            }.last()

            assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
        }

    private companion object {
        const val REMOTE_PRODUCT_ID = 42L
        val SERVER_IMAGE = Product.Image(
            id = 1L,
            name = "black-tee",
            alt = "A black t-shirt",
            source = "https://example.com/image.jpg",
            dateCreated = null,
            isCoverImage = false
        )
    }
}
