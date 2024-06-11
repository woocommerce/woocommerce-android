package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Theme
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.themes.ThemePreviewViewModel.ViewState.PreviewType
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.ThemeCoroutineStore
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThemePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    private val themeCoroutineStore: ThemeCoroutineStore,
    private val resourceProvider: ResourceProvider,
    private val themeRepository: ThemeRepository,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedStateHandle) {
    companion object {
        private const val MINIMUM_NUMBER_OF_PAGE_SECTIONS_TO_DISPLAY_DROPDOWN = 1
    }

    private val navArgs: ThemePreviewFragmentArgs by savedStateHandle.navArgs()

    private val theme = flow {
        val theme = themeRepository.getTheme(navArgs.themeId) ?: run {
            WooLog.w(WooLog.T.THEMES, "Theme not found!!")
            triggerEvent(MultiLiveEvent.Event.Exit)
            return@flow
        }
        emit(theme)
    }
    private val themePages = theme.flatMapLatest { it.prepareThemeDemoPages() }
    private val selectedPage = savedStateHandle.getNullableStateFlow(
        viewModelScope,
        null,
        ThemeDemoPage::class.java,
        "selectedPage"
    )
    private val isActivatingTheme = savedStateHandle.getStateFlow(viewModelScope, false, "isActivatingTheme")

    private val previewType = savedStateHandle.getStateFlow(viewModelScope, PreviewType.MOBILE, "previewType")

    val viewState = combine(
        theme,
        selectedPage,
        isActivatingTheme,
        themePages,
        previewType
    ) { theme, selectedPage, isActivatingTheme, demoPages, previewType ->
        ViewState(
            themeName = theme.name,
            isActivatingTheme = isActivatingTheme,
            themePages = demoPages.map { page ->
                page.copy(isLoaded = (selectedPage?.uri ?: theme.demoUrl) == page.uri)
            },
            shouldShowPagesDropdown = demoPages.size > MINIMUM_NUMBER_OF_PAGE_SECTIONS_TO_DISPLAY_DROPDOWN,
            previewType = previewType
        )
    }.asLiveData()

    init {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PREVIEW_SCREEN_DISPLAYED,
        )
    }

    fun onPageSelected(demoPage: ThemeDemoPage) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PREVIEW_PAGE_SELECTED,
            properties = mapOf(
                AnalyticsTracker.KEY_THEME_PICKER_PAGE_PREVIEW to demoPage.title
            )
        )
        selectedPage.value = demoPage
    }

    fun onBackNavigationClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onActivateThemeClicked() {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PREVIEW_START_WITH_THEME_BUTTON_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_THEME_PICKER_THEME to navArgs.themeId
            )
        )
        launch {
            isActivatingTheme.value = true
            themeRepository.activateTheme(navArgs.themeId).fold(
                onSuccess = {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.THEME_INSTALLATION_COMPLETED,
                        properties = mapOf(
                            AnalyticsTracker.KEY_THEME_PICKER_THEME to navArgs.themeId
                        )
                    )
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.theme_activated_successfully))
                    triggerEvent(
                        ExitWithResult(
                            SelectedTheme(navArgs.themeId, viewState.value?.themeName ?: "")
                        )
                    )
                },
                onFailure = {
                    analyticsTrackerWrapper.track(
                        stat = AnalyticsEvent.THEME_INSTALLATION_FAILED,
                        properties = mapOf(
                            AnalyticsTracker.KEY_THEME_PICKER_THEME to navArgs.themeId
                        )
                    )
                    triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.theme_activation_failed))
                }
            )
            isActivatingTheme.value = false
        }
    }

    fun onPreviewTypeChanged(type: PreviewType) {
        previewType.update { type }
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.THEME_PREVIEW_LAYOUT_SELECTED,
            properties = mapOf(
                AnalyticsTracker.KEY_THEME_PICKER_LAYOUT_PREVIEW to type.name.lowercase()
            )
        )
    }

    private suspend fun Theme.prepareThemeDemoPages(): Flow<List<ThemeDemoPage>> = flow {
        val homePage = ThemeDemoPage(
            uri = requireNotNull(demoUrl),
            title = resourceProvider.getString(R.string.theme_preview_bottom_sheet_home_section),
            isLoaded = true
        )
        emit(listOf(homePage))
        emit(
            buildList {
                add(homePage)
                addAll(
                    themeCoroutineStore.fetchDemoThemePages(demoUrl).map {
                        ThemeDemoPage(
                            uri = it.link,
                            title = it.title,
                            isLoaded = false
                        )
                    }.filter { it.uri != homePage.uri }
                )
            }
        )
    }

    data class ViewState(
        val themeName: String,
        val themePages: List<ThemeDemoPage>,
        val shouldShowPagesDropdown: Boolean,
        val isActivatingTheme: Boolean,
        val previewType: PreviewType = PreviewType.MOBILE
    ) {
        private val currentPage: ThemeDemoPage
            get() = themePages.first { it.isLoaded }

        val currentPageUri: String
            get() = currentPage.uri.toHttpUrl().newBuilder()
                .addQueryParameter("demo", "true")
                .build()
                .toString()

        val currentPageTitle: String
            get() = currentPage.title

        enum class PreviewType {
            DESKTOP,
            TABLET,
            MOBILE
        }
    }

    @Parcelize
    data class ThemeDemoPage(
        val uri: String,
        val title: String,
        val isLoaded: Boolean
    ) : Parcelable

    @Parcelize
    data class SelectedTheme(
        val themeId: String,
        val themeName: String
    ) : Parcelable
}
