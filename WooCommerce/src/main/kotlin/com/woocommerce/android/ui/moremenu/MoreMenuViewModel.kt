package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.*
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.reviews.ReviewListRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val reviewListRepository: ReviewListRepository
) : ScopedViewModel(savedState) {
    private var _moreMenuViewState = MutableLiveData<MoreMenuViewState>()
    val moreMenuViewState: LiveData<MoreMenuViewState> = _moreMenuViewState

    init {
        _moreMenuViewState.value = MoreMenuViewState(moreMenuItems = generateMenuButtons())
        getUnseenReviewsCount()
    }

    private fun generateMenuButtons(unreadReviewsCount: Int = 0): List<MenuUiButton> =
        listOf(
            MenuUiButton(
                text = R.string.more_menu_button_woo_admin,
                icon = R.drawable.ic_more_menu_wp_admin,
                onClick = ::onViewAdminButtonClick
            ),
            MenuUiButton(
                text = R.string.more_menu_button_store,
                icon = R.drawable.ic_more_menu_store,
                onClick = ::onViewStoreButtonClick
            ),
            MenuUiButton(
                text = R.string.more_menu_button_reviews,
                icon = R.drawable.ic_more_menu_reviews,
                badgeCount = unreadReviewsCount,
                onClick = ::onReviewsButtonClick
            )
        )

    private fun getUnseenReviewsCount() {
        viewModelScope.launch {
            val unreadReviewsCount = reviewListRepository.getCachedProductReviews()
                .filter { it.read == false }
                .count()
            _moreMenuViewState.value = _moreMenuViewState.value?.copy(
                moreMenuItems = generateMenuButtons(unreadReviewsCount)
            )
        }
    }

    fun handleStoreSwitch() {
        _moreMenuViewState.value = _moreMenuViewState.value?.copy(moreMenuItems = generateMenuButtons())
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
