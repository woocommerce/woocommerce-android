package com.woocommerce.android.ui.themes

import androidx.lifecycle.SavedStateHandle
import coil.network.HttpException
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Theme
import com.woocommerce.android.ui.themes.ThemePickerViewModel.CarouselState.Success.CarouselItem
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePickerViewModelTest : BaseUnitTest() {
    private val themeRepository: ThemeRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val crashLogger = mock<CrashLogging>()

    private val currentTheme = Theme(
        id = "tsubaki",
        name = "Tsubaki",
        demoUrl = "https://example.com/tsubaki"
    )
    private val sampleTheme = Theme(
        id = "tsubaki",
        name = "Tsubaki",
        demoUrl = "https://example.com/tsubaki"
    )
    private val sampleTheme2 = Theme(
        id = "tazza",
        name = "Tazza",
        demoUrl = "https://example.com/tazza"
    )

    private lateinit var viewModel: ThemePickerViewModel

    suspend fun setup(prepareMocks: suspend () -> Unit = {}) {
        prepareMocks()
        viewModel = ThemePickerViewModel(
            savedStateHandle = SavedStateHandle(),
            themeRepository = themeRepository,
            resourceProvider = resourceProvider,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            crashLogger = crashLogger
        )
    }

    @Test
    fun `when screen is opened, then load current theme`() = testBlocking {
        setup {
            whenever(themeRepository.fetchCurrentTheme()).thenReturn(Result.success(sampleTheme))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState.currentThemeState)
            .isEqualTo(ThemePickerViewModel.CurrentThemeState.Success(sampleTheme.name, sampleTheme.id))
        verify(themeRepository).fetchCurrentTheme()
    }

    @Test
    fun `when current theme loading fails, then hide section`() = testBlocking {
        setup {
            whenever(themeRepository.fetchThemes()).thenReturn(Result.success(listOf(sampleTheme)))
            whenever(themeRepository.fetchCurrentTheme()).thenReturn(Result.failure(Exception()))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()
        val event = viewModel.event.getOrAwaitValue()

        assertThat(viewState.currentThemeState).isEqualTo(ThemePickerViewModel.CurrentThemeState.Hidden)
        assertThat(event)
            .isEqualTo(MultiLiveEvent.Event.ShowSnackbar(R.string.theme_picker_loading_current_theme_failed))
    }

    @Test
    fun `when showing themes, then use correct values for last item`() =
        testBlocking {
            setup {
                whenever(themeRepository.fetchThemes())
                    .thenReturn(Result.success(listOf(sampleTheme)))
            }

            val viewState = viewModel.viewState.runAndCaptureValues {
                advanceUntilIdle()
            }.last()
            val lastItem = (viewState.carouselState as ThemePickerViewModel.CarouselState.Success).carouselItems.last()

            assertThat(lastItem).isInstanceOf(CarouselItem.Message::class.java)
            assertThat((lastItem as CarouselItem.Message).title)
                .isEqualTo(resourceProvider.getString(R.string.theme_picker_carousel_info_item_title))
            assertThat(lastItem.description)
                .isEqualTo(resourceProvider.getString(R.string.theme_picker_carousel_info_item_description_settings))
        }

    @Test
    fun `when loading themes succeeds, then show theme`() = testBlocking {
        setup {
            whenever(themeRepository.fetchThemes())
                .thenReturn(Result.success(listOf(sampleTheme, sampleTheme2)))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState.carouselState).isInstanceOf(ThemePickerViewModel.CarouselState.Success::class.java)
        assertThat((viewState.carouselState as ThemePickerViewModel.CarouselState.Success).carouselItems.dropLast(1))
            .containsExactly(
                CarouselItem.Theme(
                    themeId = sampleTheme.id,
                    name = sampleTheme.name,
                    screenshotUrl = AppUrls.getScreenshotUrl(sampleTheme.demoUrl!!)
                ),
                CarouselItem.Theme(
                    themeId = sampleTheme2.id,
                    name = sampleTheme2.name,
                    screenshotUrl = AppUrls.getScreenshotUrl(sampleTheme2.demoUrl!!)
                )
            )
    }

    @Test
    fun `when loading themes fails, then show error`() = testBlocking {
        setup {
            whenever(themeRepository.fetchThemes())
                .thenReturn(Result.failure(Exception()))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState.carouselState).isEqualTo(ThemePickerViewModel.CarouselState.Error)
    }

    @Test
    fun `when a theme screenshot is unavailable, then report a Sentry error`() = testBlocking {
        setup()

        viewModel.onThemeScreenshotFailure(
            themeName = "tsubaki",
            throwable = HttpException(
                response = mock { on { code } doAnswer { 307 } }
            )
        )

        verify(crashLogger).sendReport(any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `given current theme is loaded, when showing themes, then remove current theme from the list of items`() =
        testBlocking {
            setup {
                whenever(themeRepository.fetchThemes())
                    .thenReturn(Result.success(listOf(sampleTheme, sampleTheme2)))
                whenever(themeRepository.fetchCurrentTheme()).thenReturn(Result.success(currentTheme))
            }

            val viewState = viewModel.viewState.runAndCaptureValues { advanceUntilIdle() }.last()
            val carouseItems = (viewState.carouselState as ThemePickerViewModel.CarouselState.Success).carouselItems

            assertThat((carouseItems)).noneMatch { it is CarouselItem.Theme && it.themeId == currentTheme.id }
        }

    @Test
    fun `when a new theme is installed, then update current theme state`() = testBlocking {
        setup {
            whenever(themeRepository.fetchThemes())
                .thenReturn(Result.success(listOf(sampleTheme, sampleTheme2)))
            whenever(themeRepository.fetchCurrentTheme()).thenReturn(Result.success(currentTheme))
        }

        val viewState = viewModel.viewState.runAndCaptureValues {
            viewModel.onCurrentThemeUpdated(sampleTheme2.id, sampleTheme2.name)
        }.last()

        assertThat(viewState.currentThemeState)
            .isEqualTo(ThemePickerViewModel.CurrentThemeState.Success(sampleTheme2.name, sampleTheme2.id))
    }
}
