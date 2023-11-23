package com.woocommerce.android.ui.login.storecreation.themes

import android.content.res.Resources.Theme
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.ThemeModel
import javax.inject.Inject

@HiltViewModel
class ThemePickerViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val newStore: NewStore,
    private val resourceProvider: ResourceProvider,
) : ScopedViewModel(savedStateHandle) {
    private val _viewState = savedStateHandle.getStateFlow(viewModelScope, ViewState())
    val viewState = _viewState.asLiveData()

    @Parcelize
    data class ViewState(
        val themes: List<ThemeModel> = emptyList(),
        val selectedTheme: ThemeModel? = null,
        val isLoading: Boolean = false
    ) : Parcelable
}
