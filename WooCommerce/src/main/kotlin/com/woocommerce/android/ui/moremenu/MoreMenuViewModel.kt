package com.woocommerce.android.ui.moremenu

import androidx.core.net.toUri
import androidx.lifecycle.*
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_OPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_ADMIN_MENU
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_REVIEWS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_VIEW_STORE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.MenuButtonType.PRODUCT_REVIEWS
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_ADMIN
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_STORE
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    accountStore: AccountStore,
    private val selectedSite: SelectedSite,
    private val reviewListRepository: ReviewListRepository,
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler
) : ScopedViewModel(savedState) {
    private var _moreMenuViewState = MutableLiveData<MoreMenuViewState>()
    val moreMenuViewState: LiveData<MoreMenuViewState> = _moreMenuViewState

    init {
        _moreMenuViewState.value = MoreMenuViewState(
            moreMenuItems = generateMenuButtons(unseenReviewsCount = 0),
            siteName = getSelectedSiteName(),
            siteUrl = getSelectedSiteAbsoluteUrl(),
            userAvatarUrl = accountStore.account.avatarUrl
        )
        observeUnseenReviewsCount() //TODO HANDLE SITE SWITCH
    }

    private fun generateMenuButtons(unseenReviewsCount: Int): List<MenuUiButton> =
        listOf(
            MenuUiButton(
                type = VIEW_ADMIN,
                text = R.string.more_menu_button_woo_admin,
                icon = R.drawable.ic_more_menu_wp_admin,
                onClick = ::onViewAdminButtonClick
            ),
            MenuUiButton(
                type = VIEW_STORE,
                text = R.string.more_menu_button_store,
                icon = R.drawable.ic_more_menu_store,
                onClick = ::onViewStoreButtonClick
            ),
            MenuUiButton(
                type = PRODUCT_REVIEWS,
                text = R.string.more_menu_button_reviews,
                icon = R.drawable.ic_more_menu_reviews,
                badgeCount = unseenReviewsCount,
                onClick = ::onReviewsButtonClick
            )
        )

    private fun observeUnseenReviewsCount() {
        viewModelScope.launch {
            unseenReviewsCountHandler.observeUnseenCount()
                .collect {
                    _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                        moreMenuItems = generateMenuButtons(it)
                    )
                }
        }
    }

    fun handleStoreSwitch() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(
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
        AnalyticsTracker.track(
            Stat.HUB_MENU_SETTINGS_TAPPED
        )
        triggerEvent(MoreMenuEvent.NavigateToSettingsEvent)
    }

    fun onSwitchStoreClick() {
        AnalyticsTracker.track(
            Stat.HUB_MENU_SWITCH_STORE_TAPPED
        )
        triggerEvent(MoreMenuEvent.StartSitePickerEvent)
    }

    private fun onViewAdminButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_ADMIN_MENU)
        triggerEvent(MoreMenuEvent.ViewAdminEvent(selectedSite.get().adminUrl))
    }

    private fun onViewStoreButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_VIEW_STORE)
        triggerEvent(MoreMenuEvent.ViewStoreEvent(selectedSite.get().url))
    }

    private fun onReviewsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_REVIEWS)
        triggerEvent(MoreMenuEvent.ViewReviewsEvent)
    }

    private fun trackMoreMenuOptionSelected(selectedOption: String) {
        AnalyticsTracker.track(
            Stat.HUB_MENU_OPTION_TAPPED,
            mapOf(KEY_OPTION to selectedOption)
        )
    }

    data class MoreMenuViewState(
        val moreMenuItems: List<MenuUiButton> = emptyList(),
        val siteName: String = "",
        val siteUrl: String = "",
        val userAvatarUrl: String = ""
    )

    sealed class MoreMenuEvent : MultiLiveEvent.Event() {
        object NavigateToSettingsEvent : MoreMenuEvent()
        object StartSitePickerEvent : MoreMenuEvent()
        data class ViewAdminEvent(val url: String) : MoreMenuEvent()
        data class ViewStoreEvent(val url: String) : MoreMenuEvent()
        object ViewReviewsEvent : MoreMenuEvent()
    }
}
