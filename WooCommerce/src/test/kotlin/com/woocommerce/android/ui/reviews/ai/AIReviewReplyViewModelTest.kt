package com.woocommerce.android.ui.reviews.ai

import com.woocommerce.android.ai.AIRepository
import com.woocommerce.android.ai.AIRepository.JetpackAICompletionsException
import com.woocommerce.android.ui.reviews.ai.AIReviewReplyViewModel.GenerationState
import com.woocommerce.android.ui.reviews.ai.AIReviewReplyViewModel.OpenUpgradeUrl
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AIReviewReplyViewModelTest : BaseUnitTest() {

    private lateinit var sut: AIReviewReplyViewModel

    private val aiRepository: AIRepository = mock()

    private val defaultReviewerName = "John"
    private val defaultReviewText = "Great product!"
    private val defaultProductName = "Blue T-Shirt"
    private val defaultRating = 5
    private val defaultSuggestions = listOf("Thank you, John!", "We appreciate your review!", "Glad you liked it!")
    private val defaultUpgradeUrl = "https://example.com/upgrade"

    private val savedStateHandle = AIReviewReplyFragmentArgs(
        reviewerName = defaultReviewerName,
        reviewText = defaultReviewText,
        productName = defaultProductName,
        rating = defaultRating
    ).toSavedStateHandle()

    @Before
    fun setUp() = testBlocking {
        whenever(aiRepository.identifyISOLanguageCode(any(), any()))
            .thenReturn(Result.success("en"))
        whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
            .thenReturn(Result.success(defaultSuggestions))
        whenever(aiRepository.fetchUpgradeUrl())
            .thenReturn(defaultUpgradeUrl)

        createViewModel()
    }

    private fun createViewModel() {
        sut = AIReviewReplyViewModel(
            aiRepository = aiRepository,
            savedStateHandle = savedStateHandle
        )
    }

    @Test
    fun `given happy path, when generating suggestions, then state has suggestions and Generated state`() =
        testBlocking {
            // WHEN
            sut.onConfirmAIGeneration()

            // THEN
            val state = sut.viewState.value
            assertThat(state.suggestions).isEqualTo(defaultSuggestions)
            assertThat(state.generationState).isEqualTo(GenerationState.Generated)
        }

    @Test
    fun `given generation fails with non-quota error, when generating suggestions, then state is Failed`() =
        testBlocking {
            // GIVEN
            whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
                .thenReturn(Result.failure(Exception("Network error")))

            // WHEN
            sut.onConfirmAIGeneration()

            // THEN
            assertThat(sut.viewState.value.generationState).isEqualTo(GenerationState.Failed)
        }

    @Test
    fun `given quota exceeded error, when generating suggestions, then state is QuotaExceeded with upgrade url`() =
        testBlocking {
            // GIVEN
            whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
                .thenReturn(
                    Result.failure(
                        JetpackAICompletionsException(
                            errorMessage = "You have exceeded your quota",
                            errorType = "API_ERROR"
                        )
                    )
                )

            // WHEN
            sut.onConfirmAIGeneration()

            // THEN
            val state = sut.viewState.value.generationState
            assertThat(state).isInstanceOf(GenerationState.QuotaExceeded::class.java)
            assertThat((state as GenerationState.QuotaExceeded).upgradeUrl).isEqualTo(defaultUpgradeUrl)
        }

    @Test
    fun `given happy path, when suggestion selected, then replyText is filled and overlay is hidden`() =
        testBlocking {
            // GIVEN
            sut.onConfirmAIGeneration()
            val selectedSuggestion = defaultSuggestions.first()

            // WHEN
            sut.onSuggestionSelected(selectedSuggestion)

            // THEN
            val state = sut.viewState.value
            assertThat(state.replyText).isEqualTo(selectedSuggestion)
            assertThat(state.showOverlay).isFalse()
        }

    @Test
    fun `given happy path, when dismiss overlay, then overlay is hidden`() = testBlocking {
        // GIVEN
        sut.onConfirmAIGeneration()

        // WHEN
        sut.onDismissOverlay()

        // THEN
        assertThat(sut.viewState.value.showOverlay).isFalse()
    }

    @Test
    fun `given overlay is shown, when back pressed, then overlay is hidden`() = testBlocking {
        // GIVEN
        sut.onConfirmAIGeneration()
        assertThat(sut.viewState.value.showOverlay).isTrue()

        // WHEN
        sut.onBackPressed()

        // THEN
        assertThat(sut.viewState.value.showOverlay).isFalse()
    }

    @Test
    fun `given overlay is not shown, when back pressed, then Exit event is triggered`() {
        // GIVEN
        val events = mutableListOf<com.woocommerce.android.viewmodel.MultiLiveEvent.Event>()
        sut.event.observeForever(events::add)

        // WHEN
        sut.onBackPressed()

        // THEN
        assertThat(events).contains(Exit)
    }

    @Test
    fun `given reply text is not blank, when done pressed, then ExitWithResult is triggered`() {
        // GIVEN
        val replyText = "Thank you for your review!"
        sut.onTextChanged(replyText)

        val events = mutableListOf<com.woocommerce.android.viewmodel.MultiLiveEvent.Event>()
        sut.event.observeForever(events::add)

        // WHEN
        sut.onDonePressed()

        // THEN
        assertThat(events).hasSize(1)
        assertThat(events.first()).isInstanceOf(ExitWithResult::class.java)
        assertThat((events.first() as ExitWithResult<*>).data).isEqualTo(replyText)
    }

    @Test
    fun `given reply text is blank, when done pressed, then no event is triggered`() {
        // GIVEN
        val events = mutableListOf<com.woocommerce.android.viewmodel.MultiLiveEvent.Event>()
        sut.event.observeForever(events::add)

        // WHEN
        sut.onDonePressed()

        // THEN
        assertThat(events).isEmpty()
    }

    @Test
    fun `given state is Generated, when AI button clicked, then overlay is shown`() = testBlocking {
        // GIVEN
        sut.onConfirmAIGeneration()
        sut.onDismissOverlay()
        assertThat(sut.viewState.value.showOverlay).isFalse()

        // WHEN
        sut.onAIButtonClicked()

        // THEN
        assertThat(sut.viewState.value.showOverlay).isTrue()
    }

    @Test
    fun `given state is Idle, when AI button clicked, then confirmation dialog is shown`() {
        // WHEN
        sut.onAIButtonClicked()

        // THEN
        assertThat(sut.viewState.value.showConfirmationDialog).isTrue()
    }

    @Test
    fun `given state is Failed, when AI button clicked, then confirmation dialog is shown`() = testBlocking {
        // GIVEN
        whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Error")))
        sut.onConfirmAIGeneration()
        assertThat(sut.viewState.value.generationState).isEqualTo(GenerationState.Failed)

        // WHEN
        sut.onAIButtonClicked()

        // THEN
        assertThat(sut.viewState.value.showConfirmationDialog).isTrue()
    }

    @Test
    fun `given quota exceeded with upgrade url, when upgrade clicked, then OpenUpgradeUrl event is triggered`() =
        testBlocking {
            // GIVEN
            whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
                .thenReturn(
                    Result.failure(
                        JetpackAICompletionsException(
                            errorMessage = "quota exceeded",
                            errorType = "API_ERROR"
                        )
                    )
                )
            sut.onConfirmAIGeneration()

            val events = mutableListOf<com.woocommerce.android.viewmodel.MultiLiveEvent.Event>()
            sut.event.observeForever(events::add)

            // WHEN
            sut.onUpgradeClicked()

            // THEN
            assertThat(events).hasSize(1)
            assertThat(events.first()).isInstanceOf(OpenUpgradeUrl::class.java)
            assertThat((events.first() as OpenUpgradeUrl).url).isEqualTo(defaultUpgradeUrl)
            assertThat(sut.viewState.value.showOverlay).isFalse()
        }

    @Test
    fun `given quota exceeded without upgrade url, when upgrade clicked, then no event and overlay is hidden`() =
        testBlocking {
            // GIVEN
            whenever(aiRepository.fetchUpgradeUrl()).thenReturn(null)
            whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
                .thenReturn(
                    Result.failure(
                        JetpackAICompletionsException(
                            errorMessage = "quota exceeded",
                            errorType = "API_ERROR"
                        )
                    )
                )
            sut.onConfirmAIGeneration()

            val events = mutableListOf<com.woocommerce.android.viewmodel.MultiLiveEvent.Event>()
            sut.event.observeForever(events::add)

            // WHEN
            sut.onUpgradeClicked()

            // THEN
            assertThat(events).isEmpty()
            assertThat(sut.viewState.value.showOverlay).isFalse()
        }

    @Test
    fun `given happy path, when generating suggestions, then overlay is shown`() = testBlocking {
        // WHEN
        sut.onConfirmAIGeneration()

        // THEN
        assertThat(sut.viewState.value.showOverlay).isTrue()
    }

    @Test
    fun `given happy path, when confirm AI generation, then confirmation dialog is dismissed`() = testBlocking {
        // GIVEN
        sut.onAIButtonClicked()
        assertThat(sut.viewState.value.showConfirmationDialog).isTrue()

        // WHEN
        sut.onConfirmAIGeneration()

        // THEN
        assertThat(sut.viewState.value.showConfirmationDialog).isFalse()
    }

    @Test
    fun `given happy path, when retry clicked, then suggestions are regenerated`() = testBlocking {
        // GIVEN
        whenever(aiRepository.generateReviewReplySuggestions(any(), any(), any(), any(), any()))
            .thenReturn(Result.failure(Exception("Error")))
            .thenReturn(Result.success(defaultSuggestions))
        sut.onConfirmAIGeneration()
        assertThat(sut.viewState.value.generationState).isEqualTo(GenerationState.Failed)

        // WHEN
        sut.onRetryClicked()

        // THEN
        assertThat(sut.viewState.value.generationState).isEqualTo(GenerationState.Generated)
        assertThat(sut.viewState.value.suggestions).isEqualTo(defaultSuggestions)
    }

    @Test
    fun `given happy path, when text changed, then reply text is updated`() {
        // GIVEN
        val newText = "Custom reply text"

        // WHEN
        sut.onTextChanged(newText)

        // THEN
        assertThat(sut.viewState.value.replyText).isEqualTo(newText)
    }

    @Test
    fun `given happy path, when dismiss confirmation dialog, then dialog is hidden`() {
        // GIVEN
        sut.onAIButtonClicked()
        assertThat(sut.viewState.value.showConfirmationDialog).isTrue()

        // WHEN
        sut.onDismissConfirmationDialog()

        // THEN
        assertThat(sut.viewState.value.showConfirmationDialog).isFalse()
    }
}
