package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_OPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_ADMIN_MENU
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_COUPONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_INBOX
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_REVIEWS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_VIEW_STORE
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import java.net.URL
import javax.inject.Inject

@HiltViewModel
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    accountStore: AccountStore,
    private val selectedSite: SelectedSite,
    private val moreMenuRepository: MoreMenuRepository,
    private val moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler,
    unseenReviewsCountHandler: UnseenReviewsCountHandler
) : ScopedViewModel(savedState) {
    val moreMenuViewState =
        combine(
            unseenReviewsCountHandler.observeUnseenCount(),
            selectedSite.observe().filterNotNull(),
            moreMenuRepository.observeCouponBetaSwitch(),
            moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked,
        ) { count, selectedSite, isCouponsEnabled, paymentsFeatureWasClicked ->
            MoreMenuViewState(
                moreMenuItems = generateMenuButtons(
                    unseenReviewsCount = count,
                    isCouponsEnabled = isCouponsEnabled,
                    paymentsFeatureWasClicked = paymentsFeatureWasClicked,
                ),
                siteName = selectedSite.getSelectedSiteName(),
                siteUrl = selectedSite.getSelectedSiteAbsoluteUrl(),
                userAvatarUrl = accountStore.account.avatarUrl
            )
        }.asLiveData()

    fun onViewResumed() {
        moreMenuNewFeatureHandler.markNewFeatureAsSeen()
    }

    private suspend fun generateMenuButtons(
        unseenReviewsCount: Int,
        isCouponsEnabled: Boolean,
        paymentsFeatureWasClicked: Boolean,
    ) = listOf(
        MenuUiButton(
            text = R.string.more_menu_button_payments,
            icon = R.drawable.ic_more_menu_payments,
            badgeState = buildPaymentsBadgeState(paymentsFeatureWasClicked),
            onClick = ::onPaymentsButtonClick,
        ),
        MenuUiButton(
            text = R.string.more_menu_button_wс_admin,
            icon = R.drawable.ic_more_menu_wp_admin,
            onClick = ::onViewAdminButtonClick
        ),
        MenuUiButton(
            text = R.string.more_menu_button_store,
            icon = R.drawable.ic_more_menu_store,
            onClick = ::onViewStoreButtonClick
        ),
        MenuUiButton(
            text = R.string.more_menu_button_coupons,
            icon = R.drawable.ic_more_menu_coupons,
            isEnabled = isCouponsEnabled,
            onClick = ::onCouponsButtonClick
        ),
        MenuUiButton(
            text = R.string.more_menu_button_reviews,
            icon = R.drawable.ic_more_menu_reviews,
            badgeState = buildUnseenReviewsBadgeState(unseenReviewsCount),
            onClick = ::onReviewsButtonClick
        ),
        MenuUiButton(
            text = R.string.more_menu_button_inbox,
            icon = R.drawable.ic_more_menu_inbox,
            isEnabled = moreMenuRepository.isInboxEnabled(),
            onClick = ::onInboxButtonClick
        )
    )

    private fun buildPaymentsBadgeState(paymentsFeatureWasClicked: Boolean) =
        if (!paymentsFeatureWasClicked) BadgeState(
            badgeSize = R.dimen.major_110,
            backgroundColor = R.color.color_secondary,
            textColor = R.color.color_on_surface_inverted,
            textState = TextState("", R.dimen.text_minor_80),
            animateAppearance = true,
        ) else null

    private fun buildUnseenReviewsBadgeState(unseenReviewsCount: Int) =
        if (unseenReviewsCount > 0) BadgeState(
            badgeSize = R.dimen.major_150,
            backgroundColor = R.color.color_primary,
            textColor = R.color.color_on_surface_inverted,
            textState = TextState(unseenReviewsCount.toString(), R.dimen.text_minor_80),
        ) else null

    private fun SiteModel.getSelectedSiteName(): String =
        if (!displayName.isNullOrBlank()) {
            displayName
        } else {
            name
        }

    private fun SiteModel.getSelectedSiteAbsoluteUrl(): String = runCatching { URL(url).host }.getOrDefault("")

    fun onSettingsClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.HUB_MENU_SETTINGS_TAPPED
        )
        triggerEvent(MoreMenuEvent.NavigateToSettingsEvent)
    }

    fun onSwitchStoreClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.HUB_MENU_SWITCH_STORE_TAPPED
        )
        triggerEvent(MoreMenuEvent.StartSitePickerEvent)
    }

    private fun onPaymentsButtonClick() {
        trackMoreMenuOptionSelected(
            VALUE_MORE_MENU_PAYMENTS,
            mapOf(VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE to isPaymentBadgeVisible().toString())
        )
        moreMenuNewFeatureHandler.markPaymentsIconAsClicked()
        triggerEvent(MoreMenuEvent.ViewPayments)
    }

    private fun onViewAdminButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_ADMIN_MENU)
        triggerEvent(MoreMenuEvent.ViewAdminEvent(selectedSite.get().adminUrl))
    }

    private fun onViewStoreButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_VIEW_STORE)
        triggerEvent(MoreMenuEvent.ViewStoreEvent(selectedSite.get().url))
    }

    private fun onCouponsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_COUPONS)
        triggerEvent(MoreMenuEvent.ViewCouponsEvent)
    }

    private fun onReviewsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_REVIEWS)
        triggerEvent(MoreMenuEvent.ViewReviewsEvent)
    }

    private fun onInboxButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_INBOX)
        triggerEvent(MoreMenuEvent.ViewInboxEvent)
    }

    private fun trackMoreMenuOptionSelected(
        selectedOption: String,
        extraOptions: Map<String, String> = emptyMap()
    ) {
        AnalyticsTracker.track(
            AnalyticsEvent.HUB_MENU_OPTION_TAPPED,
            mapOf(KEY_OPTION to selectedOption) + extraOptions
        )
    }

    private fun isPaymentBadgeVisible() = moreMenuViewState.value
        ?.moreMenuItems
        ?.find { it.text == R.string.more_menu_button_payments }
        ?.badgeState != null

    data class MoreMenuViewState(
        val moreMenuItems: List<MenuUiButton> = emptyList(),
        val siteName: String = "",
        val siteUrl: String = "",
        val userAvatarUrl: String = ""
    )

    sealed class MoreMenuEvent : MultiLiveEvent.Event() {
        object NavigateToSettingsEvent : MoreMenuEvent()
        object StartSitePickerEvent : MoreMenuEvent()
        object ViewPayments : MoreMenuEvent()
        data class ViewAdminEvent(val url: String) : MoreMenuEvent()
        data class ViewStoreEvent(val url: String) : MoreMenuEvent()
        object ViewReviewsEvent : MoreMenuEvent()
        object ViewInboxEvent : MoreMenuEvent()
        object ViewCouponsEvent : MoreMenuEvent()
    }
}
