package com.woocommerce.android.ui.moremenu

import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite
) : ScopedViewModel(savedState) {
    private var _moreMenuViewState = MutableLiveData<MoreMenuViewState>()
    val moreMenuViewState: LiveData<MoreMenuViewState> = _moreMenuViewState

    init {
        _moreMenuViewState.value = MoreMenuViewState(
            moreMenuItems = generateMenuButtons(),
            siteName = getSelectedSiteName(),
            siteUrl = getSelectedSiteAbsoluteUrl()
        )
    }

    private fun generateMenuButtons(): List<MenuUiButton> =
        listOf(
            MenuUiButton(
                R.string.more_menu_button_woo_admin,
                R.drawable.ic_more_menu_wp_admin,
                ::onViewAdminButtonClick
            ),
            MenuUiButton(
                R.string.more_menu_button_store,
                R.drawable.ic_more_menu_store,
                ::onViewStoreButtonClick
            ),
            MenuUiButton(
                R.string.more_menu_button_reviews,
                R.drawable.ic_more_menu_reviews,
                ::onReviewsButtonClick
            )
        )

    fun handleStoreSwitch() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(
            moreMenuItems = generateMenuButtons(),
            siteName = getSelectedSiteName(),
            siteUrl = getSelectedSiteAbsoluteUrl()
        )
    }

    private fun getSelectedSiteName(): String =
        selectedSite.getIfExists()?.let { site ->
            if (!site.displayName.isNullOrBlank()) {
                site.displayName
            } else {
                site.name
            }
        } ?: ""

    private fun getSelectedSiteAbsoluteUrl(): String = selectedSite.get().url.toUri().host ?: ""

    fun onSettingsClick() {
        triggerEvent(MoreMenuEvent.NavigateToSettingsEvent)
    }

    fun onSwitchStoreClick() {
        triggerEvent(MoreMenuEvent.StartSitePickerEvent)
    }

    private fun onViewAdminButtonClick() {
        triggerEvent(MoreMenuEvent.ViewAdminEvent(selectedSite.get().adminUrl))
    }

    private fun onViewStoreButtonClick() {
        triggerEvent(MoreMenuEvent.ViewStoreEvent(selectedSite.get().url))
    }

    private fun onReviewsButtonClick() {
        triggerEvent(MoreMenuEvent.ViewReviewsEvent)
    }

    data class MoreMenuViewState(
        val moreMenuItems: List<MenuUiButton> = emptyList(),
        val siteName: String = "",
        val siteUrl: String = ""
    )

    sealed class MoreMenuEvent : MultiLiveEvent.Event() {
        object NavigateToSettingsEvent : MoreMenuEvent()
        object StartSitePickerEvent : MoreMenuEvent()
        data class ViewAdminEvent(val url: String) : MoreMenuEvent()
        data class ViewStoreEvent(val url: String) : MoreMenuEvent()
        object ViewReviewsEvent : MoreMenuEvent()
    }
}
