package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.store.ThemeCoroutineStore
import javax.inject.Inject

@HiltViewModel
class ThemePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    val themeCoroutineStore: ThemeCoroutineStore,
    val resourceProvider: ResourceProvider
    private val themeRepository: ThemeRepository,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val newStore: NewStore
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ThemePreviewFragmentArgs by savedStateHandle.navArgs()

    private val theme = flow {
        val theme = themeRepository.getTheme(navArgs.themeId) ?: run {
            WooLog.w(WooLog.T.THEMES, "Theme not found!!")
            triggerEvent(MultiLiveEvent.Event.Exit)
            return@flow
        }
        emit(theme)
    }
    private val _selectedPage = savedStateHandle.getNullableStateFlow(
        viewModelScope,
        null,
        String::class.java,
        "selectedPage"
    )

    val viewState = combine(theme, _selectedPage) { theme, selectedPage ->
        ViewState(
            demoUri = selectedPage ?: theme.demoUrl,
            themeName = theme.name,
            isFromStoreCreation = true // TODO Pass this from the previous screen
        )
    }.asLiveData()

    init {
        launch {
            val themePages = themeCoroutineStore.fetchDemoThemePages(navArgs.themeDemoUri)
            _viewState.value = _viewState.value.copy(
                themePages =
                listOf(
                    ThemeDemoPage(
                        uri = navArgs.themeDemoUri,
                        title = resourceProvider.getString(R.string.theme_preview_bottom_sheet_home_section),
                        isLoaded = true
                    )
                ) + themePages.map {
                    ThemeDemoPage(
                        uri = it.link,
                        title = it.title,
                        isLoaded = false
                    )
                }
            )
        }
    }

    fun onPageSelected(demoPage: ThemeDemoPage) {
        _viewState.value = _viewState.value.copy(
            demoUri = demoPage.uri,
            themePages = _viewState.value.themePages.map {
                if (it.uri == demoPage.uri)
                    it.copy(isLoaded = true)
                else it.copy(isLoaded = false)
            }
        )
    fun onPageSelected(updatedDemoUri: String) {
        _selectedPage.value = updatedDemoUri
    }

    fun onBackNavigationClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onActivateThemeClicked() {
        if (viewState.value?.isFromStoreCreation == true) {
            appPrefsWrapper.saveThemeIdForStoreCreation(newStore.data.siteId!!, navArgs.themeId)
            triggerEvent(ContinueStoreCreationWithTheme)
        } else {
            TODO()
        }
    }

    @Parcelize
    data class ViewState(
        val demoUri: String,
        val themeName: String,
        val isFromStoreCreation: Boolean,
        val themePages: List<ThemeDemoPage>
    ) : Parcelable

    @Parcelize
    data class ThemeDemoPage(
        val uri: String,
        val title: String,
        val isLoaded: Boolean
    ) : Parcelable

    object ContinueStoreCreationWithTheme : MultiLiveEvent.Event()
}
