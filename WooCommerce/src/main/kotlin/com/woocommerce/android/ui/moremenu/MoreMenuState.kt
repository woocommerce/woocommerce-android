package com.woocommerce.android.ui.moremenu

import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.viewmodel.MultiLiveEvent

data class MoreMenuViewState(
    val menuSections: List<MoreMenuItemSection>,
    val siteName: String = "",
    val siteUrl: String = "",
    val sitePlan: String = "",
    val userAvatarUrl: String = "",
    val isStoreSwitcherEnabled: Boolean = false
)

sealed class MoreMenuEvent : MultiLiveEvent.Event() {
    data object NavigateToSettingsEvent : MoreMenuEvent()
    data object NavigateToSubscriptionsEvent : MoreMenuEvent()
    data object StartSitePickerEvent : MoreMenuEvent()
    data object ViewPayments : MoreMenuEvent()
    data object OpenBlazeCampaignListEvent : MoreMenuEvent()
    data class OpenBlazeCampaignCreationEvent(val source: BlazeFlowSource) : MoreMenuEvent()
    data class ViewAdminEvent(val url: String) : MoreMenuEvent()
    data class ViewGoogleForWooEvent(
        val url: String,
        val successUrls: List<String>,
        val isCreationFlow: Boolean
    ) : MoreMenuEvent()
    data class ViewStoreEvent(val url: String) : MoreMenuEvent()
    data object ViewReviewsEvent : MoreMenuEvent()
    data object ViewInboxEvent : MoreMenuEvent()
    data object ViewCouponsEvent : MoreMenuEvent()

    data object ViewCustomersEvent : MoreMenuEvent()
    data object NavigateToWooPosEvent : MoreMenuEvent()
}

data class MoreMenuItemSection(
    @StringRes val title: Int?,
    val items: List<MoreMenuItemButton>,
    val isVisible: Boolean = true,
)

data class MoreMenuItemButton(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val icon: Int,
    @DrawableRes val extraIcon: Int? = null,
    val state: State = State.Visible,
    val badgeState: BadgeState? = null,
    val onClick: () -> Unit = {},
) {
    enum class State {
        Loading, Visible, Hidden,
    }

    enum class Type {
        Blaze, GoogleForWoo, WooPos, Inbox, Settings,
    }
}

data class BadgeState(
    @DimenRes val badgeSize: Int,
    @ColorRes val backgroundColor: Int,
    @ColorRes val textColor: Int,
    val textState: TextState,
    val animateAppearance: Boolean = false,
)

data class TextState(val text: String, @DimenRes val fontSize: Int)
