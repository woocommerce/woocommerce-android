package com.woocommerce.android.ui.login.storesuggestion

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storesuggestion.LoginStoreSuggestionViewModel.LogInToStore
import com.woocommerce.android.ui.login.storesuggestion.LoginStoreSuggestionViewModel.ViewState
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginStoreSuggestionViewModelTest : BaseUnitTest() {
    private val getInstallReferrerStoreSuggestion: GetInstallReferrerStoreSuggestion = mock {
        on { invoke() } doReturn SUGGESTION
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private lateinit var viewModel: LoginStoreSuggestionViewModel

    private fun setup(savedState: SavedStateHandle = SavedStateHandle()) {
        viewModel = LoginStoreSuggestionViewModel(
            savedState = savedState,
            getInstallReferrerStoreSuggestion = getInstallReferrerStoreSuggestion,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
        )
    }

    @Test
    fun `given happy path, when initialized, then view state shows the display address`() = testBlocking {
        // WHEN
        setup()

        // THEN
        assertThat(currentViewState())
            .isEqualTo(ViewState(displayAddress = DISPLAY_ADDRESS, isDismissRequested = false))
    }

    @Test
    fun `given happy path, when initialized, then suggestion shown is tracked`() = testBlocking {
        // WHEN
        setup()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_SHOWN)
    }

    @Test
    fun `given no suggestion, when initialized, then view state is null`() = testBlocking {
        // GIVEN
        whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)

        // WHEN
        setup()

        // THEN
        assertThat(currentViewState()).isNull()
    }

    @Test
    fun `given no suggestion, when initialized, then nothing is tracked`() = testBlocking {
        // GIVEN
        whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)

        // WHEN
        setup()

        // THEN
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given happy path, when onLogInToStoreClicked, then log in to store event is triggered`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onLogInToStoreClicked()

        // THEN
        assertThat(viewModel.event.captureValues().last()).isEqualTo(LogInToStore(SITE_URL))
    }

    @Test
    fun `given happy path, when onLogInToStoreClicked, then suggestion accepted is tracked`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onLogInToStoreClicked()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_ACCEPTED)
    }

    @Test
    fun `given happy path, when onLogInToStoreClicked, then view state becomes null`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val states = viewModel.viewState.runAndCaptureValues {
            viewModel.onLogInToStoreClicked()
        }

        // THEN
        assertThat(states.last()).isNull()
    }

    @Test
    fun `given no suggestion, when onLogInToStoreClicked, then no event is triggered`() = testBlocking {
        // GIVEN
        whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)
        setup()

        // WHEN
        viewModel.onLogInToStoreClicked()

        // THEN
        assertThat(viewModel.event.captureValues()).isEmpty()
    }

    @Test
    fun `given happy path, when onUseDifferentStoreClicked, then dismiss is requested`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        val states = viewModel.viewState.runAndCaptureValues {
            viewModel.onUseDifferentStoreClicked()
        }

        // THEN
        assertThat(states.last()).isEqualTo(ViewState(displayAddress = DISPLAY_ADDRESS, isDismissRequested = true))
    }

    @Test
    fun `given dismiss requested, when onDismissed, then suggestion dismissed is tracked`() = testBlocking {
        // GIVEN
        setup()
        viewModel.onUseDifferentStoreClicked()

        // WHEN
        viewModel.onDismissed()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_DISMISSED)
    }

    @Test
    fun `given dismiss requested, when onDismissed, then view state becomes null`() = testBlocking {
        // GIVEN
        setup()
        viewModel.onUseDifferentStoreClicked()

        // WHEN
        val states = viewModel.viewState.runAndCaptureValues {
            viewModel.onDismissed()
        }

        // THEN
        assertThat(states.last()).isNull()
    }

    @Test
    fun `given happy path, when onDismissed, then suggestion dismissed is tracked`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.onDismissed()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_DISMISSED)
    }

    @Test
    fun `given no suggestion, when onDismissed, then nothing is tracked`() = testBlocking {
        // GIVEN
        whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)
        setup()

        // WHEN
        viewModel.onDismissed()

        // THEN
        verifyNoInteractions(analyticsTrackerWrapper)
    }

    @Test
    fun `given suggestion already dismissed, when onDismissed again, then dismissal is tracked once`() = testBlocking {
        // GIVEN
        setup()
        viewModel.onDismissed()

        // WHEN
        viewModel.onDismissed()

        // THEN
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_DISMISSED)
    }

    @Test
    fun `given restored suggestion and use case returns null, when initialized, then view state shows it`() =
        testBlocking {
            // GIVEN
            whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)

            // WHEN
            setup(savedState = SavedStateHandle(mapOf(KEY_SUGGESTION to SUGGESTION)))

            // THEN
            assertThat(currentViewState())
                .isEqualTo(ViewState(displayAddress = DISPLAY_ADDRESS, isDismissRequested = false))
        }

    private suspend fun currentViewState() = viewModel.viewState.runAndCaptureValues {}.last()

    private companion object {
        const val KEY_SUGGESTION = "store_suggestion"
        const val DISPLAY_ADDRESS = "example.com"
        const val SITE_URL = "https://$DISPLAY_ADDRESS"
        val SUGGESTION = StoreSuggestion(siteUrl = SITE_URL, displayAddress = DISPLAY_ADDRESS)
    }
}
