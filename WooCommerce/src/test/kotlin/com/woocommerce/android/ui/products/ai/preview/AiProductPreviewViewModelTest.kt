package com.woocommerce.android.ui.products.ai.preview

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Image
import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ai.AIProductModel
import com.woocommerce.android.ui.products.ai.BuildProductPreviewProperties
import com.woocommerce.android.ui.products.ai.SaveAiGeneratedProduct
import com.woocommerce.android.ui.products.ai.components.ImageAction
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class AiProductPreviewViewModelTest : BaseUnitTest() {
    companion object {
        private const val PRODUCT_FEATURES = "product_features"
        private val SAMPLE_PRODUCT = AIProductModel.buildDefault("default name", "default description")
        private val SAMPLE_UPLOADED_IMAGE = Product.Image(0, "image", "url", Date())
    }

    private val buildProductPreviewProperties: BuildProductPreviewProperties = mock()
    private val generateProductWithAI: GenerateProductWithAI = mock {
        onBlocking { invoke(any()) } doSuspendableAnswer {
            delay(100)
            Result.success(SAMPLE_PRODUCT)
        }
    }
    private val uploadImage: UploadImage = mock {
        onBlocking { invoke(any()) } doSuspendableAnswer {
            delay(100)
            Result.success(SAMPLE_UPLOADED_IMAGE)
        }
    }
    private val saveAiGeneratedProduct: SaveAiGeneratedProduct = mock {
        onBlocking { invoke(any(), anyOrNull()) } doSuspendableAnswer {
            delay(100)
            Result.success(1L)
        }
    }
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }

    private lateinit var viewModel: AiProductPreviewViewModel

    suspend fun setup(
        args: AiProductPreviewFragmentArgs = AiProductPreviewFragmentArgs(
            productFeatures = PRODUCT_FEATURES,
            image = null
        ),
        setupMocks: suspend () -> Unit = {}
    ) {
        setupMocks()
        viewModel = AiProductPreviewViewModel(
            savedStateHandle = args.toSavedStateHandle(),
            buildProductPreviewProperties = buildProductPreviewProperties,
            generateProductWithAI = generateProductWithAI,
            uploadImage = uploadImage,
            analyticsTracker = analyticsTracker,
            saveAiGeneratedProduct = saveAiGeneratedProduct,
            resourceProvider = resourceProvider,
        )
    }

    @Test
    fun `when the viewmodel is created, then start generating the product with AI`() = testBlocking {
        setup()

        val viewState = viewModel.state.captureValues().first()

        assertThat(viewState).isInstanceOf(AiProductPreviewViewModel.State.Loading::class.java)
        verify(generateProductWithAI).invoke(PRODUCT_FEATURES)
    }

    @Test
    fun `when the product is generated, then update the view state`() = testBlocking {
        setup()

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState).isInstanceOf(AiProductPreviewViewModel.State.Success::class.java)
        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.name.value).isEqualTo(SAMPLE_PRODUCT.names.first())
        assertThat(successState.description.value).isEqualTo(SAMPLE_PRODUCT.descriptions.first())
    }

    @Test
    fun `when generating a product with different variants, then show the variant selector`() = testBlocking {
        setup {
            val product = SAMPLE_PRODUCT.copy(
                names = listOf("name1", "name2", "name3"),
                descriptions = listOf("description1", "description2"),
                shortDescriptions = listOf("shortDescription1", "shortDescription2"),
            )
            whenever(generateProductWithAI.invoke(any())).thenReturn(Result.success(product))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.shouldShowVariantSelector).isTrue()
        assertThat(successState.variantsCount).isEqualTo(2)
    }

    @Test
    fun `when a field on the product has a single option, then hide the variant selector`() = testBlocking {
        setup {
            val product = SAMPLE_PRODUCT.copy(
                names = listOf("name1"),
                descriptions = listOf("description1", "description2"),
                shortDescriptions = listOf("shortDescription1", "shortDescription2"),
            )
            whenever(generateProductWithAI.invoke(any())).thenReturn(Result.success(product))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.shouldShowVariantSelector).isFalse()
        assertThat(successState.variantsCount).isEqualTo(1)
    }

    @Test
    fun `when the user changes the variant, then update the product`() = testBlocking {
        setup {
            val product = SAMPLE_PRODUCT.copy(
                names = listOf("name1", "name2", "name3"),
                descriptions = listOf("description1", "description2"),
                shortDescriptions = listOf("shortDescription1", "shortDescription2"),
            )
            whenever(generateProductWithAI.invoke(any())).thenReturn(Result.success(product))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onSelectNextVariant()
        }.last()

        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.name.value).isEqualTo("name2")
        assertThat(successState.description.value).isEqualTo("description2")
        assertThat(successState.shortDescription.value).isEqualTo("shortDescription2")
    }

    @Test
    fun `when the user edits a field manually, then show an undo button`() = testBlocking {
        setup {
            val product = SAMPLE_PRODUCT.copy(
                names = listOf("name1", "name2", "name3"),
                descriptions = listOf("description1", "description2"),
                shortDescriptions = listOf("shortDescription1", "shortDescription2"),
            )
            whenever(generateProductWithAI.invoke(any())).thenReturn(Result.success(product))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onNameChanged("new name")
        }.last()

        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.name.isValueEditedManually).isTrue()
    }

    @Test
    fun `when the undo button of a field is clicked, then revert the field to the generated value`() =
        testBlocking {
            setup {
                val product = SAMPLE_PRODUCT.copy(
                    names = listOf("name1", "name2", "name3"),
                    descriptions = listOf("description1", "description2"),
                    shortDescriptions = listOf("shortDescription1", "shortDescription2"),
                )
                whenever(generateProductWithAI.invoke(any())).thenReturn(Result.success(product))
            }

            val viewState = viewModel.state.runAndCaptureValues {
                advanceUntilIdle()
                viewModel.onNameChanged("new name")
                viewModel.onNameChanged(null)
            }.last()

            val successState = viewState as AiProductPreviewViewModel.State.Success
            assertThat(successState.name.value).isEqualTo("name1")
            assertThat(successState.name.isValueEditedManually).isFalse()
        }

    @Test
    fun `when the image remove button is tapped, then show an undo Snackbar`() = testBlocking {
        setup(
            args = AiProductPreviewFragmentArgs(
                productFeatures = PRODUCT_FEATURES,
                image = Image.LocalImage("path")
            )
        )

        val event = viewModel.event.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onImageActionSelected(ImageAction.Remove)
        }.last()

        assertThat(event).isInstanceOf(MultiLiveEvent.Event.ShowUndoSnackbar::class.java)
    }

    @Test
    fun `given a local image, when the user taps on save, then upload the image`() = testBlocking {
        setup(
            args = AiProductPreviewFragmentArgs(
                productFeatures = PRODUCT_FEATURES,
                image = Image.LocalImage("path")
            )
        )

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onSaveProductAsDraft()
            advanceUntilIdle()
        }.last()

        verify(uploadImage).invoke(Image.LocalImage("path"))
        val successState = viewState as AiProductPreviewViewModel.State.Success
        assertThat(successState.imageState).isEqualTo(
            AiProductPreviewViewModel.ImageState(
                image = Image.WPMediaLibraryImage(SAMPLE_UPLOADED_IMAGE)
            )
        )
    }

    @Test
    fun `given a local image, when the image upload fails, then show an error`() = testBlocking {
        setup(
            args = AiProductPreviewFragmentArgs(
                productFeatures = PRODUCT_FEATURES,
                image = Image.LocalImage("path")
            )
        ) {
            whenever(uploadImage.invoke(any())).thenReturn(Result.failure(Exception()))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onSaveProductAsDraft()
            advanceUntilIdle()
        }.last() as AiProductPreviewViewModel.State.Success

        val productSavingState = viewState.savingProductState
        assertThat(productSavingState).isInstanceOf(AiProductPreviewViewModel.SavingProductState.Error::class.java)
        (productSavingState as AiProductPreviewViewModel.SavingProductState.Error).let {
            assertThat(it.messageRes).isEqualTo(R.string.ai_product_creation_error_media_upload)
        }
    }

    @Test
    fun `when product is saved successfully, then navigate to the product details`() = testBlocking {
        setup()

        val event = viewModel.event.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onSaveProductAsDraft()
            advanceUntilIdle()
        }.last()

        assertThat(event).isInstanceOf(AiProductPreviewViewModel.NavigateToProductDetailScreen::class.java)
    }

    @Test
    fun `when product saving fails, then show an error`() = testBlocking {
        setup {
            whenever(saveAiGeneratedProduct.invoke(any(), anyOrNull())).thenReturn(Result.failure(Exception()))
        }

        val viewState = viewModel.state.runAndCaptureValues {
            advanceUntilIdle()
            viewModel.onSaveProductAsDraft()
            advanceUntilIdle()
        }.last() as AiProductPreviewViewModel.State.Success

        val productSavingState = viewState.savingProductState
        assertThat(productSavingState).isInstanceOf(AiProductPreviewViewModel.SavingProductState.Error::class.java)
        (productSavingState as AiProductPreviewViewModel.SavingProductState.Error).let {
            assertThat(it.messageRes).isEqualTo(R.string.error_generic)
        }
    }
}
