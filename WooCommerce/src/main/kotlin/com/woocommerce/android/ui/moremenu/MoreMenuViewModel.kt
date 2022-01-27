package com.woocommerce.android.ui.moremenu

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    val viewStateLiveData = LiveDataDelegate(savedState, MoreMenuViewState())
    private var viewState by viewStateLiveData

    init {
        viewState = viewState.copy(
            adminUrl = selectedSite.get().adminUrl,
            storeUrl = selectedSite.get().url
        )
    }

    fun handleStoreSwitch() {
        // Update URLs to use the newly selected store's information.
        viewState = viewState.copy(
            adminUrl = selectedSite.get().adminUrl,
            storeUrl = selectedSite.get().url
        )
    }

    fun onSettingsClick() {
        triggerEvent(NavigateToSettingsEvent)
    }

    fun onSwitchStoreClick() {
        triggerEvent(StartSitePickerEvent)
    }

    fun onViewAdminButtonClick(url: String) {
        triggerEvent(ViewAdminEvent(url))
    }

    fun onViewStoreButtonClick(url: String) {
        triggerEvent(ViewStoreEvent(url))
    }

    fun onReviewsButtonClick() {
        triggerEvent(ViewReviewsEvent)
    }

    @Parcelize
    data class MoreMenuViewState(
        val adminUrl: String = "",
        val storeUrl: String = ""
    ) : Parcelable

    object NavigateToSettingsEvent : MultiLiveEvent.Event()
    object StartSitePickerEvent : MultiLiveEvent.Event()
    data class ViewAdminEvent(val url: String) : MultiLiveEvent.Event()
    data class ViewStoreEvent(val url: String) : MultiLiveEvent.Event()
    object ViewReviewsEvent : MultiLiveEvent.Event()
}
