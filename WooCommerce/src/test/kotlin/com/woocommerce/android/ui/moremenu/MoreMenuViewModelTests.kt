package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.ui.plans.domain.SitePlan
import com.woocommerce.android.ui.plans.repository.SitePlanRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore
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
        on { observeCouponBetaSwitch() } doReturn flowOf(true)
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

    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private lateinit var viewModel: MoreMenuViewModel

    suspend fun setup(setupMocks: suspend () -> Unit = {}) {
        setupMocks()
        viewModel = MoreMenuViewModel(
            savedState = SavedStateHandle(),
            accountStore = accountStore,
            selectedSite = selectedSite,
            moreMenuRepository = moreMenuRepository,
            planRepository = planRepository,
            unseenReviewsCountHandler = unseenReviewsCountHandler,
            appPrefsWrapper = appPrefsWrapper
        )
    }

    @Test
    fun `when coupons beta feature toggle is updated, then refresh the list of button`() = testBlocking {
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(moreMenuRepository.observeCouponBetaSwitch()).thenReturn(prefsChanges)
        }
        val states = viewModel.moreMenuViewState.captureValues()

        prefsChanges.emit(false)
        prefsChanges.emit(true)

        assertThat(states.size).isEqualTo(2)
    }

    @Test
    fun `when building state, then payments icon displayed`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {}

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(false)

        // THEN
        val paymentsButton = states.last().generalMenuItems.first { it.title == R.string.more_menu_button_payments }
        assertThat(paymentsButton.icon).isEqualTo(R.drawable.ic_more_menu_payments)
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
        val reviewsButton = states.last().generalMenuItems.first { it.title == R.string.more_menu_button_reviews }
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
}
