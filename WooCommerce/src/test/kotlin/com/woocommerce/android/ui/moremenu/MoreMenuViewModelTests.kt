package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.notifications.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.blaze.IsBlazeEnabled
import com.woocommerce.android.ui.common.wpcomwebview.SharedWebViewFlow
import com.woocommerce.android.ui.google.CanUseAutoLoginWebview
import com.woocommerce.android.ui.google.HasGoogleAdsCampaigns
import com.woocommerce.android.ui.google.IsGoogleForWooEnabled
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.ui.woopos.WooPosIsEnabled
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.util.runAndCaptureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.blaze.BlazeCampaignsStore
import java.time.ZonedDateTime

@ExperimentalCoroutinesApi
class MoreMenuViewModelTests : BaseUnitTest() {
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler = mock {
        on { observeUnseenCount() } doReturn flowOf(0)
    }
    private val selectedSiteFlow = MutableStateFlow(
        SiteModel().apply {
            displayName = "Site"
            url = "url"
        }
    )
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn selectedSiteFlow
        on { get() } doReturn selectedSiteFlow.value
    }
    private val moreMenuRepository: MoreMenuRepository = mock {
        onBlocking { isInboxEnabled() } doReturn true
    }
    private val accountStore: AccountStore = mock {
        on { account } doReturn AccountModel().apply {
            avatarUrl = "avatar"
        }
    }
    private val planRepository: SitePlanRepository = mock {
        onBlocking { fetchCurrentPlanDetails(any()) } doReturn SitePlan(
            name = "",
            expirationDate = ZonedDateTime.now(),
            type = SitePlan.Type.FREE_TRIAL
        )
    }

    private val resourceProvider: ResourceProvider = mock {
        on { getString(R.string.subscription_free_trial) } doReturn "Free Trial"
    }
    private val moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler = mock {
        on { moreMenuPaymentsFeatureWasClicked }.thenReturn(flowOf(true))
    }

    private val isBlazeEnabled: IsBlazeEnabled = mock {
        onBlocking { invoke() } doReturn true
    }

    private val isGoogleForWooEnabled: IsGoogleForWooEnabled = mock {
        onBlocking { invoke() } doReturn true
    }

    private val hasGoogleAdsCampaigns: HasGoogleAdsCampaigns = mock()

    private val canUseAutoLoginWebview: CanUseAutoLoginWebview = mock()

    private val isWooPosEnabled: WooPosIsEnabled = mock {
        onBlocking { invoke() } doReturn true
    }

    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val sharedWebViewFlow: SharedWebViewFlow = mock()

    private val blazeCampaignsStore: BlazeCampaignsStore = mock()

    private lateinit var viewModel: MoreMenuViewModel
    private val tapToPayAvailabilityStatus: TapToPayAvailabilityStatus = mock()

    suspend fun setup(setupMocks: suspend () -> Unit = {}) {
        setupMocks()
        viewModel = MoreMenuViewModel(
            savedState = SavedStateHandle(),
            accountStore = accountStore,
            unseenReviewsCountHandler = unseenReviewsCountHandler,
            selectedSite = selectedSite,
            moreMenuRepository = moreMenuRepository,
            moreMenuNewFeatureHandler = moreMenuNewFeatureHandler,
            planRepository = planRepository,
            resourceProvider = resourceProvider,
            blazeCampaignsStore = blazeCampaignsStore,
            tapToPayAvailabilityStatus = tapToPayAvailabilityStatus,
            isBlazeEnabled = isBlazeEnabled,
            isGoogleForWooEnabled = isGoogleForWooEnabled,
            hasGoogleAdsCampaigns = hasGoogleAdsCampaigns,
            canUseAutoLoginWebview = canUseAutoLoginWebview,
            isWooPosEnabled = isWooPosEnabled,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
            sharedWebViewFlow = sharedWebViewFlow
        )
    }

    @Test
    fun `given ttp is available, when building state, then payments icon displayed with badge`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
            whenever(tapToPayAvailabilityStatus.invoke()).thenReturn(
                TapToPayAvailabilityStatus.Result.Available
            )
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(false)

        // THEN
        val paymentsButton =
            states.last().menuSections.flatMap { it.items }.first { it.title == R.string.more_menu_button_payments }
        assertThat(paymentsButton.icon).isEqualTo(R.drawable.ic_more_menu_payments)
        assertThat(paymentsButton.badgeState?.textColor).isEqualTo(
            R.color.color_on_surface
        )
        assertThat(paymentsButton.badgeState?.badgeSize).isEqualTo(
            R.dimen.major_110
        )
        assertThat(paymentsButton.badgeState?.backgroundColor).isEqualTo(
            R.color.color_secondary
        )
        assertThat(paymentsButton.badgeState?.animateAppearance).isEqualTo(true)
        assertThat(paymentsButton.badgeState?.textState?.text).isEqualTo("")
        assertThat(paymentsButton.badgeState?.textState?.fontSize)
            .isEqualTo(R.dimen.text_minor_80)
    }

    @Test
    fun `given ttp is not available, when building state, then payments badge is not displayed`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(tapToPayAvailabilityStatus.invoke()).thenReturn(
                TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
            )
            whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(false)

        // THEN
        val paymentsButton =
            states.last().menuSections.flatMap { it.items }.first { it.title == R.string.more_menu_button_payments }
        assertThat(paymentsButton.badgeState).isNull()
    }

    @Test
    fun `when building state, then reviews icon displayed`() = testBlocking {
        // GIVEN
        setup {
            whenever(unseenReviewsCountHandler.observeUnseenCount()).thenReturn(flowOf(1))
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        val reviewsButton =
            states.last().menuSections.flatMap { it.items }.first { it.title == R.string.more_menu_button_reviews }
        assertThat(reviewsButton.icon).isEqualTo(R.drawable.ic_more_menu_reviews)
        assertThat(reviewsButton.badgeState?.textColor).isEqualTo(
            R.color.color_on_primary
        )
        assertThat(reviewsButton.badgeState?.badgeSize).isEqualTo(
            R.dimen.major_150
        )
        assertThat(reviewsButton.badgeState?.backgroundColor).isEqualTo(
            R.color.color_primary
        )
        assertThat(reviewsButton.badgeState?.animateAppearance).isEqualTo(false)
        assertThat(reviewsButton.badgeState?.textState?.text).isEqualTo("1")
        assertThat(reviewsButton.badgeState?.textState?.fontSize)
            .isEqualTo(R.dimen.text_minor_80)
    }

    @Test
    fun `given application passwords login, when building state, then store switcher state is disabled `() =
        testBlocking {
            // GIVEN
            selectedSiteFlow.update { it.apply { origin = SiteModel.ORIGIN_XMLRPC } }
            setup()

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()

            // THEN
            assertThat(states.last().isStoreSwitcherEnabled).isEqualTo(false)
        }

    @Test
    fun `given wpcom login on Jetpack connected site, when building state, then store switcher state is enabled `() =
        testBlocking {
            // GIVEN
            selectedSiteFlow.update {
                it.apply {
                    origin = SiteModel.ORIGIN_WPCOM_REST
                    setIsJetpackConnected(true)
                }
            }

            setup()

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()

            // THEN
            assertThat(states.last().isStoreSwitcherEnabled).isEqualTo(true)
        }

    @Test
    fun `given wpcom login on Jetpack CP site, when building state, then store switcher state is enabled `() =
        testBlocking {
            // GIVEN
            selectedSiteFlow.update {
                it.apply {
                    origin = SiteModel.ORIGIN_WPCOM_REST
                    setIsJetpackCPConnected(true)
                }
            }

            setup()

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()

            // THEN
            assertThat(states.last().isStoreSwitcherEnabled).isEqualTo(true)
        }

    @Test
    fun `given site plan is free trial, then free trial name is configured`() = testBlocking {
        // GIVEN
        setup {
            whenever(planRepository.fetchCurrentPlanDetails(any())).thenReturn(
                SitePlan(
                    name = "Test Plan",
                    expirationDate = ZonedDateTime.now(),
                    type = SitePlan.Type.FREE_TRIAL
                )
            )
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().sitePlan).isEqualTo("Free Trial")
    }

    @Test
    fun `given site plan is not free trial, then SitePlan name is used`() = testBlocking {
        // GIVEN
        setup {
            whenever(planRepository.fetchCurrentPlanDetails(any())).thenReturn(
                SitePlan(
                    name = "Test Plan",
                    expirationDate = ZonedDateTime.now(),
                    type = SitePlan.Type.OTHER
                )
            )
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().sitePlan).isEqualTo("Test Plan")
    }

    @Test
    fun `given site plan is WPcom, then SitePlan name is formatted`() = testBlocking {
        // GIVEN
        setup {
            whenever(planRepository.fetchCurrentPlanDetails(any())).thenReturn(
                SitePlan(
                    name = "WordPress.com Test Plan",
                    expirationDate = ZonedDateTime.now(),
                    type = SitePlan.Type.OTHER
                )
            )
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().sitePlan).isEqualTo("Test Plan")
    }

    @Test
    fun `when on view resumed, then new feature handler marks new feature as seen`() = testBlocking {
        // GIVEN
        setup { }

        // WHEN
        viewModel.onViewResumed()

        // THEN
        verify(moreMenuNewFeatureHandler).markNewFeatureAsSeen()
    }

    @Test
    fun `given user never clicked payments and ttp available, when building state, then badge displayed`() =
        testBlocking {
            // GIVEN
            val prefsChanges = MutableSharedFlow<Boolean>()
            setup {
                whenever(tapToPayAvailabilityStatus.invoke()).thenReturn(
                    TapToPayAvailabilityStatus.Result.Available
                )
                whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
            }

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()
            prefsChanges.emit(false)

            // THEN
            assertThat(
                states.last().menuSections.flatMap { it.items }.first {
                    it.title == R.string.more_menu_button_payments
                }.badgeState
            ).isNotNull
        }

    @Test
    fun `given user clicked payments, when building state, then badge is not displayed`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(true)

        // THEN
        assertThat(states.last().menuSections.flatMap { it.items }.first().badgeState).isNull()
    }

    @Test
    fun `given site plan is paid Woo Express, then SitePlan name is formatted`() = testBlocking {
        // GIVEN
        setup {
            whenever(planRepository.fetchCurrentPlanDetails(any())).thenReturn(
                SitePlan(
                    name = "Woo Express: Test Plan",
                    expirationDate = ZonedDateTime.now(),
                    type = SitePlan.Type.OTHER
                )
            )
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().sitePlan).isEqualTo("Test Plan")
    }

    @Test
    fun `given site plan is null, then SitePlan name is empty`() = testBlocking {
        // GIVEN
        setup {
            whenever(planRepository.fetchCurrentPlanDetails(any())).thenReturn(null)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().sitePlan).isEqualTo("")
    }

    @Test
    fun `given no blaze campaigns, when user clicks on blaze, then start campaign creation`() = testBlocking {
        setup {
            whenever(blazeCampaignsStore.getBlazeCampaigns(any())).thenReturn(emptyList())
        }

        val state = viewModel.moreMenuViewState.captureValues().last()
        val button = state.menuSections.flatMap { it.items }.first { it.title == R.string.more_menu_button_blaze }
        val event = viewModel.event.runAndCaptureValues {
            button.onClick()
        }.last()

        assertThat(event).isInstanceOf(MoreMenuViewModel.MoreMenuEvent.OpenBlazeCampaignCreationEvent::class.java)
    }

    @Test
    fun `given existing blaze campaigns, when user clicks on blaze, then show campaigns list`() = testBlocking {
        setup {
            whenever(blazeCampaignsStore.getBlazeCampaigns(any()))
                .thenReturn(listOf(mock()))
        }

        val state = viewModel.moreMenuViewState.captureValues().last()
        val button = state.menuSections.flatMap { it.items }.first { it.title == R.string.more_menu_button_blaze }
        val event = viewModel.event.runAndCaptureValues {
            button.onClick()
        }.last()

        assertThat(event).isEqualTo(MoreMenuViewModel.MoreMenuEvent.OpenBlazeCampaignListEvent)
    }

    @Test
    fun `given isWooPosEnabled returns false, when building state, then WooPOS section is not displayed`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(isWooPosEnabled.invoke()).thenReturn(false)
            }

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()

            // THEN
            assertThat(
                states.last().menuSections.flatMap { it.items }
                    .firstOrNull { it.title == R.string.more_menu_button_woo_pos }
            ).isNull()
        }

    @Test
    fun `given isWooPosEnabled returns true, when building state, then WooPOS section is displayed`() = testBlocking {
        // GIVEN
        setup {
            whenever(isWooPosEnabled.invoke()).thenReturn(true)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        assertThat(states.last().menuSections.first().title).isNull()
        assertThat(states.last().menuSections.first().items.count()).isEqualTo(1)
        assertThat(
            states.last().menuSections.flatMap { it.items }
                .first { it.title == R.string.more_menu_button_woo_pos }.isVisible
        ).isTrue()
    }

    @Test
    fun `given isWooPosEnabled returns true, when building state, then 3 sections are shown without dividers at the end`() =
        testBlocking {
            // GIVEN
            setup {
                whenever(isWooPosEnabled.invoke()).thenReturn(true)
                whenever(moreMenuRepository.isUpgradesEnabled()).thenReturn(true)
            }

            // WHEN
            val states = viewModel.moreMenuViewState.captureValues()

            // THEN
            assertThat(states.last().menuSections.count()).isEqualTo(3)

            assertThat(states.last().menuSections[0].title).isNull()
            val itemsFirstSection = states.last().menuSections[0].items
            assertThat(itemsFirstSection.count()).isEqualTo(1)

            assertThat(states.last().menuSections[1].title).isEqualTo(R.string.more_menu_settings_section_title)
            val itemsThirdSection = states.last().menuSections[1].items
            assertThat(itemsThirdSection.count()).isEqualTo(2)

            assertThat(states.last().menuSections[2].title).isEqualTo(R.string.more_menu_general_section_title)
            val itemsSecondSection = states.last().menuSections[2].items
            assertThat(itemsSecondSection.count()).isEqualTo(9)
        }
}
