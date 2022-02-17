package com.woocommerce.android.ui.moremenu

import androidx.core.net.toUri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_OPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_ADMIN_MENU
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_INBOX
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_REVIEWS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_VIEW_STORE
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.extensions.NotificationSeenEvent
import com.woocommerce.android.extensions.NotificationsUnseenReviewsEvent
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.MenuButtonType.INBOX
import com.woocommerce.android.ui.moremenu.MenuButtonType.PRODUCT_REVIEWS
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_ADMIN
import com.woocommerce.android.ui.moremenu.MenuButtonType.VIEW_STORE
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.AccountStore
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    accountStore: AccountStore,
    private val selectedSite: SelectedSite,
    private val reviewListRepository: ReviewListRepository
) : ScopedViewModel(savedState) {
    private var _moreMenuViewState = MutableLiveData<MoreMenuViewState>()
    val moreMenuViewState: LiveData<MoreMenuViewState> = _moreMenuViewState

    init {
        EventBus.getDefault().register(this)

        _moreMenuViewState.value = MoreMenuViewState(
            moreMenuItems = generateMenuButtons(unseenReviewsCount = 0),
            siteName = getSelectedSiteName(),
            siteUrl = getSelectedSiteAbsoluteUrl(),
            userAvatarUrl = accountStore.account.avatarUrl
        )
        refreshUnseenReviewsCount()
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
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
            ),
            MenuUiButton(
                type = INBOX,
                text = R.string.more_menu_button_inbox,
                icon = R.drawable.ic_more_menu_inbox,
                isEnabled = FeatureFlag.MORE_MENU_INBOX.isEnabled(),
                onClick = ::onInboxButtonClick
            )
        )

    private fun refreshUnseenReviewsCount() {
        viewModelScope.launch {
            // First we set the cached value
            _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                moreMenuItems = generateMenuButtons(getCachedUnseenReviewsCount())
            )

            // Then we fetch from API the refreshed value and update the UI again
            when (reviewListRepository.fetchProductReviews(loadMore = false)) {
                RequestResult.SUCCESS,
                RequestResult.NO_ACTION_NEEDED -> {
                    _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                        moreMenuItems = generateMenuButtons(getCachedUnseenReviewsCount())
                    )
                }
                else -> {
                }
            }
        }
    }

    private suspend fun getCachedUnseenReviewsCount() =
        reviewListRepository.getCachedProductReviews()
            .filter { it.read == false }
            .count()

    fun handleStoreSwitch() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(
            siteName = getSelectedSiteName(),
            siteUrl = getSelectedSiteAbsoluteUrl()
        )
        refreshUnseenReviewsCount()
    }

    private fun resetUnseenReviewsBadgeCount() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(
            moreMenuItems = generateMenuButtons(0)
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

    private fun onInboxButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_INBOX)
        triggerEvent(MoreMenuEvent.ViewInboxEvent)
    }

    private fun trackMoreMenuOptionSelected(selectedOption: String) {
        AnalyticsTracker.track(
            Stat.HUB_MENU_OPTION_TAPPED,
            mapOf(KEY_OPTION to selectedOption)
        )
    }

    private fun updateUnseenCountBy(updateByValue: Int) {
        val currentCount = _moreMenuViewState.value?.moreMenuItems
            ?.firstOrNull { it.type == PRODUCT_REVIEWS }
            ?.badgeCount ?: 0
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(
            moreMenuItems = generateMenuButtons(currentCount + updateByValue)
        )
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationReceivedEvent) {
        if (event.channel == NotificationChannelType.REVIEW) {
            updateUnseenCountBy(1)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationsUnseenReviewsEvent) {
        if (!event.hasUnseen) {
            resetUnseenReviewsBadgeCount()
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationSeenEvent) {
        if (event.channel == NotificationChannelType.REVIEW) {
            updateUnseenCountBy(-1)
        }
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
        object ViewInboxEvent : MoreMenuEvent()
    }
}
