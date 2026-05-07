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
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_AI_ASSISTANT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_BOOKINGS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_COUPONS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_CUSTOMERS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_INBOX
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_PAYMENTS_BADGE_VISIBLE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_REVIEWS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_UPGRADES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_MORE_MENU_VIEW_STORE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ciab.CIABAffectedFeature
import com.woocommerce.android.ciab.CIABSiteGateKeeper
import com.woocommerce.android.extensions.adminUrlOrDefault
import com.woocommerce.android.model.UiString
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.tools.SiteConnectionType
import com.woocommerce.android.tools.connectionType
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.bookings.tab.ObserveBookingsVisibility
import com.woocommerce.android.ui.google.HasGoogleAdsCampaigns
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.payments.taptopay.isAvailable
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
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
    observeBookingsVisibility: ObserveBookingsVisibility,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val ciabSiteGateKeeper: CIABSiteGateKeeper,
    private val featureFlagRepository: FeatureFlagRepository,
) : ScopedViewModel(savedState) {
    private var storeHasGoogleAdsCampaigns = false
    private val shouldShowBookingsInMenu = MutableStateFlow(false)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val bookingsVisibilityFlow = shouldShowBookingsInMenu.flatMapLatest { shouldShow ->
        if (shouldShow) {
            observeBookingsVisibility()
        } else {
            flowOf(false)
        }
    }

    val moreMenuViewState =
        combine(
            unseenReviewsCountHandler.observeUnseenCount(),
            selectedSite.observe().filterNotNull(),
            moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked,
            loadSitePlanName(),
            checkFeaturesAvailability(),
        ) { count, selectedSite, paymentsFeatureWasClicked, sitePlanName, moreMenuButtonStatus ->
            MoreMenuViewState(
                menuSections = generateAllSections(
                    moreMenuButtonStatus,
                    count,
                    paymentsFeatureWasClicked,
                ).map { section ->
                    section.copy(
                        items = section.items.filter { it.state != MoreMenuItemButton.State.Hidden }
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

    private fun generateAllSections(
        buttonsStates: Map<MoreMenuItemButton.Type, MoreMenuItemButton.State>,
        count: Int,
        paymentsFeatureWasClicked: Boolean
    ) = listOf(
        generateSettingsMenuButtons(buttonsStates[MoreMenuItemButton.Type.Settings]!!),
        generateGeneralSection(
            unseenReviewsCount = count,
            paymentsFeatureWasClicked = paymentsFeatureWasClicked,
            googleForWooState = buttonsStates[MoreMenuItemButton.Type.GoogleForWoo]!!,
            blazeState = buttonsStates[MoreMenuItemButton.Type.Blaze]!!,
            inboxState = buttonsStates[MoreMenuItemButton.Type.Inbox]!!,
            paymentsState = buttonsStates[MoreMenuItemButton.Type.Payments]!!,
            bookingsButtonState = buttonsStates[MoreMenuItemButton.Type.Bookings]!!,
            aiAssistantState = buttonsStates[MoreMenuItemButton.Type.AiAssistant]!!,
        )
    )

    fun onViewResumed() {
        moreMenuNewFeatureHandler.markNewFeatureAsSeen()
        launch {
            trackBlazeDisplayed()
            trackGoogleAdsDisplayed()
        }
    }

    fun onWindowClassChanged(isLargeThanCompact: Boolean) {
        shouldShowBookingsInMenu.value = isLargeThanCompact
    }

    @Suppress("LongMethod")
    private fun generateGeneralSection(
        unseenReviewsCount: Int,
        paymentsFeatureWasClicked: Boolean,
        googleForWooState: MoreMenuItemButton.State,
        blazeState: MoreMenuItemButton.State,
        inboxState: MoreMenuItemButton.State,
        paymentsState: MoreMenuItemButton.State,
        bookingsButtonState: MoreMenuItemButton.State,
        aiAssistantState: MoreMenuItemButton.State,
    ) = MoreMenuItemSection(
        title = R.string.more_menu_general_section_title,
        items = listOf(
            MoreMenuItemButton(
                title = R.string.more_menu_button_payments,
                description = R.string.more_menu_button_payments_description,
                icon = R.drawable.ic_more_menu_payments,
                badgeState = buildPaymentsBadgeState(paymentsFeatureWasClicked),
                onClick = ::onPaymentsButtonClick,
                state = paymentsState
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_ai_assistant,
                description = R.string.more_menu_button_ai_assistant_description,
                icon = R.drawable.ic_more_menu_ai_assistant,
                onClick = ::onAiAssistantButtonClick,
                state = aiAssistantState,
            ),
            MoreMenuItemButton(
                title = R.string.bookings_tab_title,
                description = R.string.more_menu_button_bookings_description,
                icon = R.drawable.ic_bookings_tab,
                onClick = ::onBookingsButtonClick,
                state = bookingsButtonState,
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_google,
                description = R.string.more_menu_button_google_description,
                icon = R.drawable.google_logo,
                onClick = ::onPromoteProductsWithGoogle,
                state = googleForWooState,
            ),
            MoreMenuItemButton(
                title = R.string.more_menu_button_blaze,
                description = R.string.more_menu_button_blaze_description,
                icon = R.drawable.ic_blaze,
                onClick = ::onPromoteProductsWithBlaze,
                state = blazeState,
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
                onClick = ::onInboxButtonClick,
                state = inboxState,
            )
        )
    )

    private fun generateSettingsMenuButtons(settingsMenuButtonState: MoreMenuItemButton.State) =
        MoreMenuItemSection(
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
                    state = settingsMenuButtonState,
                    onClick = ::onUpgradesButtonClick
                )
            )
        )

    private fun trackBlazeDisplayed() {
        if (isBlazeEnabled()) {
            AnalyticsTracker.track(
                stat = BLAZE_ENTRY_POINT_DISPLAYED,
                properties = mapOf(AnalyticsTracker.KEY_BLAZE_SOURCE to BlazeFlowSource.MORE_MENU_ITEM.trackingName)
            )
        }
    }

    private suspend fun trackGoogleAdsDisplayed() {
        if (isGoogleForWooEnabled()) {
            analyticsTrackerWrapper.track(
                stat = AnalyticsEvent.GOOGLEADS_ENTRY_POINT_DISPLAYED,
                properties = mapOf(
                    AnalyticsTracker.KEY_GOOGLEADS_SOURCE
                        to AnalyticsTracker.VALUE_GOOGLEADS_ENTRY_POINT_SOURCE_MOREMENU
                )
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

    private fun onBookingsButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_BOOKINGS)
        triggerEvent(MoreMenuEvent.ViewBookingsEvent)
    }

    private fun onAiAssistantButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_AI_ASSISTANT)
        triggerEvent(MoreMenuEvent.ViewAiAssistantEvent)
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

        triggerEvent(MoreMenuEvent.ViewGoogleForWooEvent(url, isCreationFlow = true))
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

        triggerEvent(MoreMenuEvent.ViewGoogleForWooEvent(url, isCreationFlow = false))
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
        val site = selectedSite.get()
        val adminUrl = site.adminUrlOrDefault
        if (site.isCIABSite) {
            // CIAB hides the navigation sidebar when detecting a mobile user agent,
            // which breaks the WC Admin experience. Open in Chrome Custom Tab instead.
            triggerEvent(MultiLiveEvent.Event.LaunchUrlInChromeTab(url = adminUrl))
        } else {
            triggerEvent(
                MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView(
                    url = adminUrl,
                    screenTitle = UiString.UiStringRes(R.string.more_menu_button_wс_admin)
                )
            )
        }
    }

    private fun onViewStoreButtonClick() {
        trackMoreMenuOptionSelected(VALUE_MORE_MENU_VIEW_STORE)
        triggerEvent(
            MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView(
                url = selectedSite.get().url,
                screenTitle = UiString.UiStringText(selectedSite.get().getSelectedSiteName())
            )
        )
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
        analyticsTrackerWrapper.track(
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

    private fun checkFeaturesAvailability(): Flow<Map<MoreMenuItemButton.Type, MoreMenuItemButton.State>> {
        val initialState = MoreMenuItemButton.Type.entries.associateWith {
            MoreMenuItemButton.State.Loading
        }.toMutableMap().apply {
            this[MoreMenuItemButton.Type.Bookings] = MoreMenuItemButton.State.Hidden
            this[MoreMenuItemButton.Type.AiAssistant] = MoreMenuItemButton.State.Hidden
        }

        return listOf(
            bookingsVisibilityFlow.map { isVisible ->
                MoreMenuItemButton.Type.Bookings to if (isVisible) {
                    MoreMenuItemButton.State.Visible
                } else {
                    MoreMenuItemButton.State.Hidden
                }
            },
            doCheckAvailability(MoreMenuItemButton.Type.Blaze) { isBlazeEnabled() },
            doCheckAvailability(MoreMenuItemButton.Type.GoogleForWoo) { isGoogleForWooEnabled() },
            doCheckAvailability(MoreMenuItemButton.Type.Inbox) { moreMenuRepository.isInboxEnabled() },
            doCheckAvailability(MoreMenuItemButton.Type.Settings) { moreMenuRepository.isUpgradesEnabled() },
            doCheckAvailability(MoreMenuItemButton.Type.Payments) {
                ciabSiteGateKeeper.isFeatureSupported(CIABAffectedFeature.InPersonPayments)
            },
            doCheckAvailability(MoreMenuItemButton.Type.AiAssistant) {
                featureFlagRepository.isEnabled(FeatureFlag.AI_ASSISTANT)
            },
        ).merge()
            .map { update ->
                initialState[update.first] = update.second
                initialState
            }
            .onStart { emit(initialState) }
    }

    private fun doCheckAvailability(
        type: MoreMenuItemButton.Type,
        checker: suspend () -> Boolean
    ): Flow<Pair<MoreMenuItemButton.Type, MoreMenuItemButton.State>> = flow {
        val state = if (checker()) MoreMenuItemButton.State.Visible else MoreMenuItemButton.State.Hidden
        emit(type to state)
    }

    private val SitePlan.formattedPlanName
        get() = generateFormattedPlanName(resourceProvider)
}
