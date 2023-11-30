package com.woocommerce.android.ui.themes

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.ui.common.wpcomwebview.WPComWebViewAuthenticator
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.network.UserAgent
import javax.inject.Inject

@HiltViewModel
class ThemePreviewViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    val wpComWebViewAuthenticator: WPComWebViewAuthenticator,
    val userAgent: UserAgent,
    val themeRepository: ThemeRepository
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
            isFromStoreCreation = true // TODO: Pass this from the previous screen
        )
    }.asLiveData()

    fun onPageSelected(updatedDemoUri: String) {
        _selectedPage.value = updatedDemoUri
    }

    fun onBackNavigationClicked() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onActivateThemeClicked() {
        // TODO
    }

    @Parcelize
    data class ViewState(
        val demoUri: String,
        val themeName: String,
        val isFromStoreCreation: Boolean
    ) : Parcelable
}
