package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED
import com.woocommerce.android.analytics.AnalyticsEvent.BLAZE_ENTRY_POINT_DISPLAYED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_OPTION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_ADMIN_MENU
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_COUPONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_CUSTOMERS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_INBOX
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_REVIEWS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_UPGRADES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_VIEW_STORE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.google.HasGoogleAdsCampaigns
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.isAvailable
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.woopos.WooPosIsEnabled
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import org.wordpress.android.fluxc.utils.extensions.slashJoin
import java.net.URL
import javax.inject.Inject

@HiltViewModel
@Suppress("TooManyFunctions", "LongParameterList", "UnusedPrivateMember")
class MoreMenuViewModel @Inject constructor(
    savedState: SavedStateHandle,
    accountStore: AccountStore,
    unseenReviewsCountHandler: UnseenReviewsCountHandler,
    private val selectedSite: SelectedSite,
    private val moreMenuRepository: MoreMenuRepository,
    private val planRepository: SitePlanRepository,
    private val resourceProvider: ResourceProvider,
    private val blazeCampaignsStore: BlazeCampaignsStore,
    private val moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler,
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus,
    private val isBlazeEnabled: IsBlazeEnabled,
    private val isGoogleForWooEnabled: IsGoogleForWooEnabled,
    private val hasGoogleAdsCampaigns: HasGoogleAdsCampaigns,
    private val isWooPosEnabled: WooPosIsEnabled,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper
) : ScopedViewModel(savedState) {
    private var storeHasGoogleAdsCampaigns = false

    val moreMenuViewState =
        combine(
            unseenReviewsCountHandler.observeUnseenCount(),
            selectedSite.observe().filterNotNull(),
            moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked,
            loadSitePlanName()
        ) { count, selectedSite, paymentsFeatureWasClicked, sitePlanName ->
            MoreMenuViewState(
                menuSections = listOf(
                    generatePOSSection(),
                    generateSettingsMenuButtons(),
                    generateGeneralSection(
                        unseenReviewsCount = count,
                        paymentsFeatureWasClicked = paymentsFeatureWasClicked,
                    )
                ).map { section ->
                    section.copy(
                        items = section.items.filter { it.isVisible }
                    )
                }.filter { it.isVisible && it.items.isNotEmpty() },
                siteName = selectedSite.getSelectedSiteName(),
                siteUrl = selectedSite.getSelectedSiteAbsoluteUrl(),
                sitePlan = sitePlanName,
                userAvatarUrl = accountStore.account.avatarUrl,
                isStoreSwitcherEnabled = selectedSite.connectionType != SiteConnectionType.ApplicationPasswords,
            )
        }.asLiveData()

    init {
        launch {
            hasGoogleAdsCampaigns().fold(
                onSuccess = { storeHasGoogleAdsCampaigns = it },
                onFailure = { WooLog.e(WooLog.T.GOOGLE_ADS, "Failed to fetch Google Ads campaigns: $it") }
            )
        }
    }

    fun onViewResumed() {
        moreMenuNewFeatureHandler.markNewFeatureAsSeen()
        launch { trackBlazeDisplayed() }
    }

    private suspend fun generatePOSSection() =
        MoreMenuItemSection(
            title = null,
            items = listOf(
                MoreMenuItemButton(
                    title = R.string.more_menu_button_woo_pos,
                    description = R.string.more_menu_button_woo_pos_description,
                    icon = R.drawable.ic_more_menu_pos,
                    extraIcon = R.drawable.ic_more_menu_pos_extra,
                    isVisible = isWooPosEnabled(),
                    onClick = {
                        triggerEvent(MoreMenuEvent.NavigateToWooPosEvent)
                    }
                )
            )
        )

    @Suppress("LongMethod")
    private suspend fun generateGeneralSection(
        unseenReviewsCount: Int,
        paymentsFeatureWasClicked: Boolean,
    ) = MoreMenuItemSection(
        title = R.string.more_menu_general_section_title,
        items = listOf(
            MoreMenuItemButton(
                title = R.string.more_menu_button_payments,
                description = R.string.more_menu_button_payments_description,
                icon = R.drawable.ic_more_menu_payments,
                badgeState = buildPaymentsBadgeState(paymentsFeatureWasClicked),
                onClick = ::onPaymentsButtonClick,
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_google,
                description = R.string.more_menu_button_google_description,
                icon = R.drawable.google_logo,
                onClick = ::onPromoteProductsWithGoogle,
                isVisible = isGoogleForWooEnabled()
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_blaze,
                description = R.string.more_menu_button_blaze_description,
                icon = R.drawable.ic_blaze,
                onClick = ::onPromoteProductsWithBlaze,
                isVisible = isBlazeEnabled()
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_wс_admin,
                description = R.string.more_menu_button_wc_admin_description,
                icon = R.drawable.ic_more_menu_wp_admin,
                extraIcon = R.drawable.ic_external,
                onClick = ::onViewAdminButtonClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_store,
                description = R.string.more_menu_button_store_description,
                icon = R.drawable.ic_more_menu_store,
                extraIcon = R.drawable.ic_external,
                onClick = ::onViewStoreButtonClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_coupons,
                description = R.string.more_menu_button_coupons_description,
                icon = R.drawable.ic_more_menu_coupons,
                onClick = ::onCouponsButtonClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_reviews,
                description = R.string.more_menu_button_reviews_description,
                icon = R.drawable.ic_more_menu_reviews,
                badgeState = buildUnseenReviewsBadgeState(unseenReviewsCount),
                onClick = ::onReviewsButtonClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_customers,
                description = R.string.more_menu_button_customers_description,
                icon = R.drawable.icon_multiple_users,
                onClick = ::onCustomersButtonClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_inbox,
                description = R.string.more_menu_button_inbox_description,
                icon = R.drawable.ic_more_menu_inbox,
                isVisible = moreMenuRepository.isInboxEnabled(),
                onClick = ::onInboxButtonClick,
            )
        )
    )

    private fun generateSettingsMenuButtons() = MoreMenuItemSection(
        title = R.string.more_menu_settings_section_title,
        items = listOf(
            MoreMenuItemButton(
                title = R.string.more_menu_button_settings,
                description = R.string.more_menu_button_settings_description,
                icon = R.drawable.ic_more_screen_settings,
                onClick = ::onSettingsClick
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_subscriptions,
                description = R.string.more_menu_button_subscriptions_description,
                icon = R.drawable.ic_more_menu_upgrades,
                isVisible = moreMenuRepository.isUpgradesEnabled(),
                onClick = ::onUpgradesButtonClick
            )
        )
    )

    private suspend fun trackBlazeDisplayed() {
        if (isBlazeEnabled()) {
            AnalyticsTracker.track(
                stat = BLAZE_ENTRY_POINT_DISPLAYED,
                properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.MORE_MENU_ITEM.trackingName)
            )
        }
    }

    private fun buildPaymentsBadgeState(paymentsFeatureWasClicked: Boolean) =
        if (!paymentsFeatureWasClicked && tapToPayAvailabilityStatus().isAvailable) {
            BadgeState(
                badgeSize = R.dimen.major_110,
                backgroundColor = R.color.color_secondary,
                textColor = R.color.color_on_surface,
                textState = TextState("", R.dimen.text_minor_80),
                animateAppearance = true,
            )
        } else {
            null
        }

    private fun buildUnseenReviewsBadgeState(unseenReviewsCount: Int) =
        if (unseenReviewsCount > 0) {
            BadgeState(
                badgeSize = R.dimen.major_150,
                backgroundColor = R.color.color_primary,
                textColor = R.color.color_on_primary,
                textState = TextState(unseenReviewsCount.toString(), R.dimen.text_minor_80),
            )
        } else {
            null
        }

    private fun SiteModel.getSelectedSiteName(): String =
        if (!displayName.isNullOrBlank()) {
            displayName
        } else {
            name
        }

    private fun SiteModel.getSelectedSiteAbsoluteUrl(): String = runCatching { URL(url).host }.getOrDefault("")

    fun onSwitchStoreClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.HUB_MENU_SWITCH_STORE_TAPPED
        )
        triggerEvent(MoreMenuEvent.StartSitePickerEvent)
    }

    private fun onSettingsClick() {
        AnalyticsTracker.track(
            AnalyticsEvent.HUB_MENU_SETTINGS_TAPPED
        )
        triggerEvent(MoreMenuEvent.NavigateToSettingsEvent)
    }

    private fun onPaymentsButtonClick() {
        trackMoreMenuOptionSelected(
            VALUE_MORE_MENU_PAYMENTS,
            mapOf(VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE to isPaymentBadgeVisible().toString())
        )
        moreMenuNewFeatureHandler.markPaymentsIconAsClicked()
        triggerEvent(MoreMenuEvent.ViewPayments)
    }

    private fun onPromoteProductsWithGoogle() {
        launch {
            val urlToOpen = determineUrlToOpen()

            if (urlToOpen.contains(AppUrls.GOOGLE_ADMIN_CAMPAIGN_CREATION_SUFFIX)) {
                launchGoogleAdsCampaignCreation(urlToOpen)
            } else {
                launchGoogleAdsCampaignDetails(urlToOpen)
            }
        }
    }

    private fun determineUrlToOpen(): String {
        val baseUrl = selectedSite.get().adminUrlOrDefault
        return if (storeHasGoogleAdsCampaigns) {
            baseUrl.slashJoin(AppUrls.GOOGLE_ADMIN_DASHBOARD)
        } else {
            baseUrl.slashJoin(AppUrls.GOOGLE_ADMIN_CAMPAIGN_CREATION_SUFFIX)
        }
    }

    private fun launchGoogleAdsCampaignCreation(url: String) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.GOOGLEADS_ENTRY_POINT_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_GOOGLEADS_SOURCE
                    to AnalyticsTracker.VALUE_GOOGLEADS_ENTRY_POINT_SOURCE_MOREMENU,
                AnalyticsTracker.KEY_GOOGLEADS_TYPE
                    to AnalyticsTracker.VALUE_GOOGLEADS_ENTRY_POINT_TYPE_CREATION,
                AnalyticsTracker.KEY_GOOGLEADS_HAS_CAMPAIGNS
                    to storeHasGoogleAdsCampaigns
            )
        )
        val successUrlTriggers = listOf(
            AppUrls.GOOGLE_ADMIN_FIRST_CAMPAIGN_CREATION_SUCCESS_TRIGGER,
            AppUrls.GOOGLE_ADMIN_SUBSEQUENT_CAMPAIGN_CREATION_SUCCESS_TRIGGER
        )

        triggerEvent(MoreMenuEvent.ViewGoogleForWooEvent(url, successUrlTriggers))
    }

    private fun launchGoogleAdsCampaignDetails(url: String) {
        analyticsTrackerWrapper.track(
            stat = AnalyticsEvent.GOOGLEADS_ENTRY_POINT_TAPPED,
            properties = mapOf(
                AnalyticsTracker.KEY_GOOGLEADS_SOURCE
                    to AnalyticsTracker.VALUE_GOOGLEADS_ENTRY_POINT_SOURCE_MOREMENU,
                AnalyticsTracker.KEY_GOOGLEADS_TYPE
                    to AnalyticsTracker.VALUE_GOOGLEADS_ENTRY_POINT_TYPE_CREATION,
                AnalyticsTracker.KEY_GOOGLEADS_HAS_CAMPAIGNS
                    to storeHasGoogleAdsCampaigns
            )
        )

        triggerEvent(MoreMenuEvent.ViewGoogleForWooEvent(url, listOf()))
    }

    fun handleSuccessfulGoogleAdsCreation() {
        storeHasGoogleAdsCampaigns = true
    }

    private fun onPromoteProductsWithBlaze() {
        launch {
            val hasCampaigns = blazeCampaignsStore.getBlazeCampaigns(selectedSite.get()).isNotEmpty()
            if (hasCampaigns) {
                AnalyticsTracker.track(
                    stat = BLAZE_CAMPAIGN_LIST_ENTRY_POINT_SELECTED,
                    properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.MORE_MENU_ITEM.trackingName)
                )
                triggerEvent(MoreMenuEvent.OpenBlazeCampaignListEvent)
            } else {
                triggerEvent(
                    MoreMenuEvent.OpenBlazeCampaignCreationEvent(source = BlazeFlowSource.MORE_MENU_ITEM)
                )
            }
        }
    }

    private fun onViewAdminButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_ADMIN_MENU)
        triggerEvent(MoreMenuEvent.ViewAdminEvent(selectedSite.get().adminUrlOrDefault))
    }

    private fun onViewStoreButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_VIEW_STORE)
        triggerEvent(MoreMenuEvent.ViewStoreEvent(selectedSite.get().url))
    }

    private fun onCouponsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_COUPONS)
        triggerEvent(MoreMenuEvent.ViewCouponsEvent)
    }

    private fun onCustomersButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_CUSTOMERS)
        triggerEvent(MoreMenuEvent.ViewCustomersEvent)
    }

    private fun onReviewsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_REVIEWS)
        triggerEvent(MoreMenuEvent.ViewReviewsEvent)
    }

    private fun onInboxButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_INBOX)
        triggerEvent(MoreMenuEvent.ViewInboxEvent)
    }

    private fun onUpgradesButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_UPGRADES)
        triggerEvent(MoreMenuEvent.NavigateToSubscriptionsEvent)
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
        ?.menuSections
        ?.filterIsInstance<MoreMenuItemButton>()
        ?.find { it.title == R.string.more_menu_button_payments }
        ?.badgeState != null

    private fun loadSitePlanName(): Flow<String> = selectedSite.observe()
        .filterNotNull()
        .map { site ->
            planRepository.fetchCurrentPlanDetails(site)
                ?.formattedPlanName.orEmpty()
        }
        .onStart { emit("") }

    private val SitePlan.formattedPlanName
        get() = generateFormattedPlanName(resourceProvider)

    data class MoreMenuViewState(
        val menuSections: List<MoreMenuItemSection>,
        val siteName: String = "",
        val siteUrl: String = "",
        val sitePlan: String = "",
        val userAvatarUrl: String = "",
        val isStoreSwitcherEnabled: Boolean = false
    )

    sealed class MoreMenuEvent : MultiLiveEvent.Event() {
        object NavigateToSettingsEvent : MoreMenuEvent()
        object NavigateToSubscriptionsEvent : MoreMenuEvent()
        object StartSitePickerEvent : MoreMenuEvent()
        object ViewPayments : MoreMenuEvent()
        object OpenBlazeCampaignListEvent : MoreMenuEvent()
        data class OpenBlazeCampaignCreationEvent(val source: BlazeFlowSource) : MoreMenuEvent()
        data class ViewGoogleForWooEvent(
            val url: String,
            val successUrls: List<String>
        ) : MoreMenuEvent()
        data class ViewAdminEvent(val url: String) : MoreMenuEvent()
        data class ViewStoreEvent(val url: String) : MoreMenuEvent()
        object ViewReviewsEvent : MoreMenuEvent()
        object ViewInboxEvent : MoreMenuEvent()
        object ViewCouponsEvent : MoreMenuEvent()

        object ViewCustomersEvent : MoreMenuEvent()
        object NavigateToWooPosEvent : MoreMenuEvent()
    }
}
