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
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class LoginStoreSuggestionViewModelTest : BaseUnitTest() {
    private val getInstallReferrerStoreSuggestion: GetInstallReferrerStoreSuggestion = mock {
        on { invoke() } doReturn SUGGESTION
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val savedState = SavedStateHandle()

    private lateinit var viewModel: LoginStoreSuggestionViewModel

    private fun setup(savedState: SavedStateHandle = this.savedState) {
        viewModel = LoginStoreSuggestionViewModel(
            savedState = savedState,
            getInstallReferrerStoreSuggestion = getInstallReferrerStoreSuggestion,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
        )
    }

    @Test
    fun `given suggestion, when started twice, then it is requested once, shown, saved and tracked`() = testBlocking {
        // GIVEN
        setup()
        verify(getInstallReferrerStoreSuggestion, never()).invoke()

        // WHEN
        viewModel.start(isAllowed = true)
        viewModel.start(isAllowed = true)

        // THEN
        verify(getInstallReferrerStoreSuggestion, times(1)).invoke()
        assertThat(currentViewState()).isEqualTo(VISIBLE_STATE)
        assertThat(savedState.get<StoreSuggestion>(KEY_SUGGESTION)).isEqualTo(SUGGESTION)
        verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_SHOWN)
    }

    @Test
    fun `given not allowed, when start, then suggestion is not requested`() = testBlocking {
        // GIVEN
        setup()

        // WHEN
        viewModel.start(isAllowed = false)

        // THEN
        verify(getInstallReferrerStoreSuggestion, never()).invoke()
        assertThat(currentViewState()).isNull()
    }

    @Test
    fun `given no suggestion, when start and buttons are tapped, then nothing is shown, sent or tracked`() =
        testBlocking {
            // GIVEN
            whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)
            setup()

            // WHEN
            viewModel.start(isAllowed = true)
            viewModel.onLogInToStoreClicked()
            viewModel.onUseDifferentStoreClicked()
            viewModel.onDismissed()

            // THEN
            assertThat(currentViewState()).isNull()
            assertThat(viewModel.event.captureValues()).isEmpty()
            verifyNoInteractions(analyticsTrackerWrapper)
        }

    @Test
    fun `given suggestion, when log in to store is tapped, then event is sent and suggestion is cleared`() =
        testBlocking {
            // GIVEN
            setup()
            viewModel.start(isAllowed = true)

            // WHEN
            viewModel.onLogInToStoreClicked()

            // THEN
            assertThat(viewModel.event.captureValues().last()).isEqualTo(LogInToStore(siteUrl = SITE_URL, host = HOST))
            verify(analyticsTrackerWrapper).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_ACCEPTED)
            assertThat(currentViewState()).isNull()
            assertThat(savedState.get<StoreSuggestion>(KEY_SUGGESTION)).isNull()
        }

    @Test
    fun `given suggestion, when use a different store is tapped and dismissed, then it closes and is tracked once`() =
        testBlocking {
            // GIVEN
            setup()
            viewModel.start(isAllowed = true)

            // WHEN
            viewModel.onUseDifferentStoreClicked()
            val dismissRequested = currentViewState()?.isDismissRequested
            viewModel.onDismissed()
            viewModel.onDismissed()
            viewModel.onLogInToStoreClicked()

            // THEN
            assertThat(dismissRequested).isTrue()
            assertThat(currentViewState()).isNull()
            assertThat(savedState.get<StoreSuggestion>(KEY_SUGGESTION)).isNull()
            verify(analyticsTrackerWrapper, times(1)).track(AnalyticsEvent.LOGIN_STORE_SUGGESTION_DISMISSED)
            assertThat(viewModel.event.captureValues()).isEmpty()
        }

    @Test
    fun `given restored suggestion, when started and the use case returns null, then it is still shown`() =
        testBlocking {
            // GIVEN
            whenever(getInstallReferrerStoreSuggestion()).thenReturn(null)
            setup(savedState = SavedStateHandle(mapOf(KEY_SUGGESTION to SUGGESTION)))

            // WHEN
            viewModel.start(isAllowed = true)

            // THEN
            assertThat(currentViewState()).isEqualTo(VISIBLE_STATE)
        }

    private suspend fun currentViewState() = viewModel.viewState.runAndCaptureValues {}.last()

    private companion object {
        const val KEY_SUGGESTION = "store_suggestion"
        const val HOST = "shop.store.com"
        const val REGISTRABLE_DOMAIN = "store.com"
        const val SITE_URL = "https://$HOST"
        val SUGGESTION = StoreSuggestion(siteUrl = SITE_URL, host = HOST, registrableDomain = REGISTRABLE_DOMAIN)
        val VISIBLE_STATE = ViewState(host = HOST, registrableDomain = REGISTRABLE_DOMAIN, isDismissRequested = false)
    }
}
