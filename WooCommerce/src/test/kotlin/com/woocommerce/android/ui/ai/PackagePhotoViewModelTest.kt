package com.woocommerce.android.ui.ai

import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.AIProductDetailsResult
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.ui.products.ai.PackagePhotoBottomSheetFragmentArgs
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Failure
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.NoKeywordsFound
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.GenerationState.Success
import com.woocommerce.android.ui.products.ai.PackagePhotoViewModel.ViewState.Keyword
import com.woocommerce.android.ui.products.ai.TextRecognitionEngine
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.internal.verification.VerificationModeFactory.atLeastOnce
import org.mockito.internal.verification.VerificationModeFactory.times
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class PackagePhotoViewModelTest : BaseUnitTest() {

    private lateinit var viewModel: PackagePhotoViewModel

    private val aiRepository: AIRepository = mock()
    private val textRecognitionEngine: TextRecognitionEngine = mock()
    private val tracker: AnalyticsTrackerWrapper = mock()

    private fun createViewModel() {
        val navArgs = PackagePhotoBottomSheetFragmentArgs("image_url")
        viewModel = PackagePhotoViewModel(
            navArgs.initSavedStateHandle(),
            aiRepository,
            textRecognitionEngine,
            tracker
        )
    }

    @Before
    fun setup() = testBlocking {
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(emptyList()))
        whenever(aiRepository.identifyISOLanguageCode(any(), any())).thenReturn(Result.success("en"))
        whenever(aiRepository.generateProductNameAndDescription(any(), any())).thenReturn(
            Result.success(
                AIProductDetailsResult(
                    "name",
                    "description"
                )
            )
        )
    }

    @Test
    fun `when no keywords found, the NoKeywordsFound state is set`() = testBlocking {
        createViewModel()

        val lastState = viewModel.viewState.captureValues().last()

        assertThat(lastState.state).isEqualTo(NoKeywordsFound)
    }

    @Test
    fun `when keywords are scanned, they are used to generate name and description`() = testBlocking {
        val keywords = listOf("Keyword1", "Keyword2")
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(keywords))

        createViewModel()

        val lastState = viewModel.viewState.captureValues().last()

        assertThat(lastState.state).isEqualTo(Success)
        assertThat(lastState.keywords).isEqualTo(keywords.map { Keyword(it, true) })
        assertThat(lastState.title).isEqualTo("name")
        assertThat(lastState.description).isEqualTo("description")
    }

    @Test
    fun `when an error occurs during AI completion call, the Failure state is set`() = testBlocking {
        val keywords = listOf("Keyword1", "Keyword2")
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(keywords))
        whenever(aiRepository.generateProductNameAndDescription(any(), any()))
            .thenReturn(Result.failure(JetpackAICompletionsException("errorMessage", "errorType")))

        createViewModel()

        val lastState = viewModel.viewState.captureValues().last()

        assertThat(lastState.state).isEqualTo(Failure("errorMessage"))
        assertThat(lastState.title).isEqualTo("")
        assertThat(lastState.description).isEqualTo("")
    }

    @Test
    fun `when all keywords are unselected, regenerate button gets disabled`() = testBlocking {
        val keywords = listOf("Keyword1", "Keyword2")
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(keywords))

        createViewModel()

        val before = viewModel.viewState.captureValues().last()

        assertThat(before.isRegenerateButtonEnabled).isTrue

        var after = viewModel.viewState.runAndCaptureValues {
            viewModel.onKeywordChanged(0, Keyword("Keyword1", false))
        }.last()

        assertThat(after.isRegenerateButtonEnabled).isTrue

        after = viewModel.viewState.runAndCaptureValues {
            viewModel.onKeywordChanged(1, Keyword("Keyword2", false))
        }.last()

        assertThat(after.isRegenerateButtonEnabled).isFalse
    }

    @Test
    fun `when a successful generation flow is performed, the right set of analytics is tracked`() = testBlocking {
        val keywords = listOf("Keyword1", "Keyword2")
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(keywords))

        createViewModel()

        viewModel.onContinueTapped()

        val captor = argumentCaptor<AnalyticsEvent>()
        verify(tracker, times(5)).track(captor.capture(), any())

        assertThat(captor.allValues[0]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DISPLAYED)
        assertThat(captor.allValues[1]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_COMPLETED)
        assertThat(captor.allValues[2]).isEqualTo(AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS)
        assertThat(captor.allValues[3]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DETAILS_GENERATED)
        assertThat(captor.allValues[4]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_CONTINUE_BUTTON_TAPPED)
    }

    @Test
    fun `when a generation flow fails, the right set of analytics is tracked`() = testBlocking {
        val keywords = listOf("Keyword1", "Keyword2")
        whenever(textRecognitionEngine.processImage(any())).thenReturn(Result.success(keywords))
        whenever(aiRepository.generateProductNameAndDescription(any(), any()))
            .thenReturn(Result.failure(JetpackAICompletionsException("errorMessage", "errorType")))

        createViewModel()

        whenever(aiRepository.identifyISOLanguageCode(any(), any()))
            .thenReturn(Result.failure(JetpackAICompletionsException("errorMessage", "errorType")))

        viewModel.onRegenerateTapped()

        val captor = argumentCaptor<AnalyticsEvent>()
        verify(tracker, atLeastOnce()).track(captor.capture(), any())

        assertThat(captor.allValues[0]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DISPLAYED)
        assertThat(captor.allValues[1]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_SCAN_COMPLETED)
        assertThat(captor.allValues[2]).isEqualTo(AnalyticsEvent.AI_IDENTIFY_LANGUAGE_SUCCESS)
        assertThat(captor.allValues[3]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_DETAIL_GENERATION_FAILED)
        assertThat(captor.allValues[4]).isEqualTo(AnalyticsEvent.ADD_PRODUCT_FROM_IMAGE_REGENERATE_BUTTON_TAPPED)
        assertThat(captor.allValues[5]).isEqualTo(AnalyticsEvent.AI_IDENTIFY_LANGUAGE_FAILED)
    }
}
