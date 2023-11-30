package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : ScopedViewModel(savedStateHandle) {
    private val navArgs: ThemePreviewFragmentArgs by savedStateHandle.navArgs()
    private val _viewState = savedStateHandle.getStateFlow(
        viewModelScope,
        ViewState(
            demoUri = navArgs.themeDemoUri,
            themePages = emptyList()
        )
    )
    val viewState = _viewState.asLiveData()

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
                else it
            }
        )
    }

    fun onBackNavigationClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    @Parcelize
    data class ViewState(
        val demoUri: String,
        val themePages: List<ThemeDemoPage>
    ) : Parcelable

    @Parcelize
    data class ThemeDemoPage(
        val uri: String,
        val title: String,
        val isLoaded: Boolean
    ) : Parcelable
}
