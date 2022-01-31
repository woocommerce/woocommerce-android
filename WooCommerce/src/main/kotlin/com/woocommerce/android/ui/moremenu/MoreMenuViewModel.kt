package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.*
import com.woocommerce.android.R
import com.woocommerce.android.extensions.NotificationReceivedEvent
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.push.NotificationChannelType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.MenuButtonType.*
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.*
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val reviewListRepository: ReviewListRepository
) : ScopedViewModel(savedState) {
    private var _moreMenuViewState = MutableLiveData<MoreMenuViewState>()
    val moreMenuViewState: LiveData<MoreMenuViewState> = _moreMenuViewState

    init {
        EventBus.getDefault().register(this)

        _moreMenuViewState.value = MoreMenuViewState(moreMenuItems = generateMenuButtons(unreadReviewsCount = 0))
        refreshUnreadReviewsCount()
    }

    override fun onCleared() {
        EventBus.getDefault().unregister(this)
    }

    private fun generateMenuButtons(unreadReviewsCount: Int): List<MenuUiButton> =
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
                badgeCount = unreadReviewsCount,
                onClick = ::onReviewsButtonClick
            )
        )

    private fun refreshUnreadReviewsCount() {
        viewModelScope.launch {
            //First we set the cached value
            _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                moreMenuItems = generateMenuButtons(getCachedUnreadReviewsCount())
            )

            //Then we fetch from API the refreshed value and update the UI again
            when (reviewListRepository.fetchProductReviews(loadMore = false)) {
                RequestResult.SUCCESS,
                RequestResult.NO_ACTION_NEEDED -> {
                    _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                        moreMenuItems = generateMenuButtons(getCachedUnreadReviewsCount())
                    )
                }
                else -> {
                }
            }
        }
    }

    private suspend fun getCachedUnreadReviewsCount() =
        reviewListRepository.getCachedProductReviews()
            .filter { it.read == false }
            .count()

    fun handleStoreSwitch() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(generateMenuButtons(0))
        refreshUnreadReviewsCount()
    }

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

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: NotificationReceivedEvent) {
        if (event.channel == NotificationChannelType.REVIEW) {
            refreshUnreadReviewsCount()
        }
    }

    data class MoreMenuViewState(
        val moreMenuItems: List<MenuUiButton> = emptyList(),
    )

    sealed class MoreMenuEvent : MultiLiveEvent.Event() {
        object NavigateToSettingsEvent : MoreMenuEvent()
        object StartSitePickerEvent : MoreMenuEvent()
        data class ViewAdminEvent(val url: String) : MoreMenuEvent()
        data class ViewStoreEvent(val url: String) : MoreMenuEvent()
        object ViewReviewsEvent : MoreMenuEvent()
    }
}
