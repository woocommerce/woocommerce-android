package com.woocommerce.android.ui.login.storecreation.theme

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.themes.ThemeRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeActivationViewModelTests : BaseUnitTest() {
    private val themeRepository: ThemeRepository = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private lateinit var viewModel: ThemeActivationViewModel
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()

    private val themeId: String = "tsubaki"

    private suspend fun setup(setupMocks: suspend () -> Unit = {}) {
        setupMocks()

        viewModel = ThemeActivationViewModel(
            savedStateHandle = ThemeActivationFragmentDialogArgs(themeId).toSavedStateHandle(),
            themeRepository = themeRepository,
            appPrefsWrapper = appPrefsWrapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper
        )
    }

    @Test
    fun `when dialog is shown, then start theme installation`() = testBlocking {
        setup()

        verify(themeRepository).activateTheme(themeId)
    }

    @Test
    fun `when theme installation is successful, then clear theme id and exit`() = testBlocking {
        setup {
            whenever(themeRepository.activateTheme(themeId)).doSuspendableAnswer {
                // The duration is not important, we need the delay just to pause the coroutine
                delay(10L)
                Result.success(Unit)
            }
        }

        val events = viewModel.event.runAndCaptureValues {
            advanceUntilIdle()
        }

        verify(appPrefsWrapper).clearThemeIdForStoreCreation()
        assertThat(events).containsExactly(
            Event.ShowSnackbar(R.string.theme_activated_successfully),
            Event.Exit
        )
    }

    @Test
    fun `given theme installation fails, when retry is clicked, then retry theme installation`() = testBlocking {
        setup {
            whenever(themeRepository.activateTheme(themeId)).thenReturn(Result.failure(Throwable()))
        }

        val state = viewModel.viewState.captureValues().last()
        (state as ThemeActivationViewModel.ViewState.ErrorState).onRetry()

        verify(themeRepository, times(2)).activateTheme(themeId)
    }

    @Test
    fun `given theme installation fails, when dismiss is clicked, then clear theme id and exit`() = testBlocking {
        setup {
            whenever(themeRepository.activateTheme(themeId)).thenReturn(Result.failure(Throwable()))
        }

        val state = viewModel.viewState.captureValues().last()
        (state as ThemeActivationViewModel.ViewState.ErrorState).onDismiss()
        val event = viewModel.event.captureValues().last()

        verify(appPrefsWrapper).clearThemeIdForStoreCreation()
        assertThat(event).isEqualTo(Event.Exit)
    }
}
