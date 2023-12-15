package com.woocommerce.android.ui.themes

import coil.network.HttpException
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class ThemePickerViewModelTest : BaseUnitTest() {
    private val themeRepository: ThemeRepository = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
    }
    private val crashLogger = mock<CrashLogging>()

    private val currentTheme = Theme(
        id = "tsubaki", name = "Tsubaki", demoUrl = "https://example.com/tsubaki"
    )
    private val sampleTheme = Theme(
        id = "tsubaki", name = "Tsubaki", demoUrl = "https://example.com/tsubaki"
    )
    private val sampleTheme2 = Theme(
        id = "tazza", name = "Tazza", demoUrl = "https://example.com/tazza"
    )

    private lateinit var viewModel: ThemePickerViewModel

    suspend fun setup(
        isFromStoreCreation: Boolean,
        prepareMocks: suspend () -> Unit = {}
    ) {
        prepareMocks()
        viewModel = ThemePickerViewModel(
            savedStateHandle = ThemePickerFragmentArgs(isFromStoreCreation).toSavedStateHandle(),
            themeRepository = themeRepository,
            resourceProvider = resourceProvider,
            crashLogger = crashLogger
        )
    }

    @Test
    fun `when screen opened, then use correct value for isFromStoreCreation`() = testBlocking {
        setup(isFromStoreCreation = true)

        val storeCreationViewState = viewModel.viewState.getOrAwaitValue()

        assertThat(storeCreationViewState.isFromStoreCreation).isTrue()

        // ====================

        setup(isFromStoreCreation = false)

        val appSettingsViewState = viewModel.viewState.getOrAwaitValue()

        assertThat(appSettingsViewState.isFromStoreCreation).isFalse()
    }

    @Test
    fun `given navigating from store creation, when screen opened, then don't load current theme`() = testBlocking {
        setup(isFromStoreCreation = true)

        val viewState = viewModel.viewState.runAndCaptureValues {
            advanceUntilIdle()
        }.last()

        assertThat(viewState.currentThemeState).isEqualTo(ThemePickerViewModel.CurrentThemeState.Hidden)
        verify(themeRepository, never()).fetchCurrentTheme()
    }

    @Test
    fun `given navigating from app settings, when screen is opened, then load current theme`() = testBlocking {
        setup(isFromStoreCreation = false) {
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
    fun `given navigating from app settings, when current theme loading fails, then hide section`() = testBlocking {
        setup(isFromStoreCreation = false) {
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
    fun `given navigating from store creation, when showing themes, then use correct values for last item`() =
        testBlocking {
            setup(isFromStoreCreation = true) {
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
                .isEqualTo(resourceProvider.getString(R.string.theme_picker_carousel_info_item_description))
        }

    @Test
    fun `given navigating from app settings, when showing themes, then use correct values for last item`() =
        testBlocking {
            setup(isFromStoreCreation = false) {
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
        setup(isFromStoreCreation = true) {
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
        setup(isFromStoreCreation = true) {
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
        setup(isFromStoreCreation = true)

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
            setup(isFromStoreCreation = false) {
                whenever(themeRepository.fetchThemes())
                    .thenReturn(Result.success(listOf(sampleTheme, sampleTheme2)))
                whenever(themeRepository.fetchCurrentTheme()).thenReturn(Result.success(currentTheme))
            }

            val viewState = viewModel.viewState.runAndCaptureValues { advanceUntilIdle() }.last()
            val carouseItems = (viewState.carouselState as ThemePickerViewModel.CarouselState.Success).carouselItems

            assertThat((carouseItems)).noneMatch { it is CarouselItem.Theme && it.themeId == currentTheme.id }
        }
}
