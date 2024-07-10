package com.woocommerce.android.ui.moremenu

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
    data class ViewGoogleEvent(val url: String) : MoreMenuEvent()
    data class ViewStoreEvent(val url: String) : MoreMenuEvent()
    data object ViewReviewsEvent : MoreMenuEvent()
    data object ViewInboxEvent : MoreMenuEvent()
    data object ViewCouponsEvent : MoreMenuEvent()

    data object ViewCustomersEvent : MoreMenuEvent()
    data object NavigateToWooPosEvent : MoreMenuEvent()
}

data class MoreMenuButtonStatus(
    val type: MoreMenuButtonType,
    val status: MoreMenuButtonCheckStatus
)

enum class MoreMenuButtonType {
    Blaze, GoogleForWoo, WooPos
}

enum class MoreMenuButtonCheckStatus {
    Loading, Enabled, Disabled
}
