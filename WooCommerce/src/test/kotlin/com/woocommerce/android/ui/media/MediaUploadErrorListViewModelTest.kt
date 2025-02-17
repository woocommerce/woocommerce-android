package com.woocommerce.android.ui.media

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.ui.media.MediaUploadErrorListViewModel.ErrorUiModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import kotlin.test.Test

@ExperimentalCoroutinesApi
class MediaUploadErrorListViewModelTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private val SOME_UPLOAD_ERRORS = arrayOf(
            ProductImageUploadData(
                localUri = "file://test1.jpg",
                remoteProductId = REMOTE_PRODUCT_ID,
                uploadStatus = UploadStatus.Failed(
                    media = null,
                    mediaErrorType = MediaErrorType.GENERIC_ERROR,
                    mediaErrorMessage = "test.jpg"
                )
            ),
            ProductImageUploadData(
                localUri = "file://test2.jpg",
                remoteProductId = REMOTE_PRODUCT_ID,
                uploadStatus = UploadStatus.Failed(
                    media = null,
                    mediaErrorType = MediaErrorType.GENERIC_ERROR,
                    mediaErrorMessage = "test.jpg"
                )
            )
        )
        private val SOME_UI_MODEL_ERRORS = SOME_UPLOAD_ERRORS.map {
            ErrorUiModel(it.uploadStatus as UploadStatus.Failed, it.localUri)
        }
    }

    private val resourceProvider: ResourceProvider = mock()
    private val mediaFileUploadHandler: MediaFileUploadHandler = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: MediaUploadErrorListViewModel

    @Test
    fun `given some errors passed as fragment args, when viewmodel is created, clear image errors`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            verify(mediaFileUploadHandler).clearImageErrors(REMOTE_PRODUCT_ID)
        }

    @Test
    fun `when viewmodel is created, then observe error updates for product`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            verify(mediaFileUploadHandler).observeCurrentUploadErrors(REMOTE_PRODUCT_ID)
        }

    @Test
    fun `given several upload errors, when onRetryUploadClicked is called, then enqueue upload`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            viewModel.onRetryUploadClicked(SOME_UI_MODEL_ERRORS.last())

            verify(mediaFileUploadHandler).enqueueUpload(REMOTE_PRODUCT_ID, listOf("file://test2.jpg"))
        }

    @Test
    fun `given several upload errors, when onRetryUploadClicked is called, then error is removed from the list`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            viewModel.onRetryUploadClicked(SOME_UI_MODEL_ERRORS.last())

            observeState { state ->
                assertThat(state.uploadErrorList).isEqualTo(SOME_UI_MODEL_ERRORS.dropLast(1))
            }
        }

    @Test
    fun `given a single upload error, when onRetryUploadClicked is called, then trigger Exit event`() = testBlocking {
        createViewModel(arrayOf(SOME_UPLOAD_ERRORS.first()))
        val retriedError = SOME_UI_MODEL_ERRORS.first()

        viewModel.onRetryUploadClicked(retriedError)

        observeState { state ->
            assertThat(state.uploadErrorList.isEmpty()).isEqualTo(true)
        }
        observeEvents { event ->
            assertThat(event).isEqualTo(MultiLiveEvent.Event.Exit)
        }
    }

    @Test
    fun `given some upload errors, when onRetryUploadClicked is called, then clear that upload error`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            viewModel.onRetryUploadClicked(SOME_UI_MODEL_ERRORS.last())

            verify(mediaFileUploadHandler).clearImageErrors(REMOTE_PRODUCT_ID, listOf("file://test2.jpg"))
        }

    @Test
    fun `given some upload errors, when onRetryUploadClicked is called, then track retry button tapped`() =
        testBlocking {
            createViewModel(SOME_UPLOAD_ERRORS)

            viewModel.onRetryUploadClicked(SOME_UI_MODEL_ERRORS.last())

            verify(analyticsTrackerWrapper).track(AnalyticsEvent.PRODUCT_IMAGE_UPLOAD_RETRY_BUTTON_TAPPED)
        }

    private fun createViewModel(errors: Array<ProductImageUploadData>? = SOME_UPLOAD_ERRORS) {
        viewModel = MediaUploadErrorListViewModel(
            resourceProvider,
            mediaFileUploadHandler,
            analyticsTrackerWrapper,
            savedStateHandle = MediaUploadErrorListFragmentArgs(
                errorList = errors,
                remoteProductId = REMOTE_PRODUCT_ID
            ).toSavedStateHandle()
        )
    }

    private fun observeState(check: (MediaUploadErrorListViewModel.ViewState) -> Unit) =
        viewModel.viewState.observeForever { check(it) }

    private fun observeEvents(check: (MultiLiveEvent.Event) -> Unit) =
        viewModel.event.observeForever { check(it) }
}
