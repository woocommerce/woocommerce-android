package com.woocommerce.android.ui.moremenu

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.push.UnseenReviewsCountHandler
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.moremenu.domain.MoreMenuRepository
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.AccountModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore

@ExperimentalCoroutinesApi
class MoreMenuViewModelTests : BaseUnitTest() {
    private val unseenReviewsCountHandler: UnseenReviewsCountHandler = mock {
        on { observeUnseenCount() } doReturn flowOf(0)
    }
    private val selectedSite: SelectedSite = mock {
        on { observe() } doReturn flowOf(
            SiteModel().apply {
                displayName = "Site"
                url = "url"
            }
        )
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
    private val moreMenuNewFeatureHandler: MoreMenuNewFeatureHandler = mock {
        on { moreMenuPaymentsFeatureWasClicked }.thenReturn(flowOf(true))
    }

    private lateinit var viewModel: MoreMenuViewModel

    suspend fun setup(setupMocks: suspend () -> Unit) {
        setupMocks()
        viewModel = MoreMenuViewModel(
            savedState = SavedStateHandle(),
            accountStore = accountStore,
            selectedSite = selectedSite,
            moreMenuRepository = moreMenuRepository,
            moreMenuNewFeatureHandler = moreMenuNewFeatureHandler,
            unseenReviewsCountHandler = unseenReviewsCountHandler,
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
    fun `when on view resumed, then new feature handler marks new feature as seen`() = testBlocking {
        // GIVEN
        setup { }

        // WHEN
        viewModel.onViewResumed()

        // THEN
        verify(moreMenuNewFeatureHandler).markNewFeatureAsSeen()
    }

    @Test
    fun `given user never clicked payments, when building state, then badge displayed`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(false)

        // THEN
        assertThat(states.last().moreMenuItems.first().badgeState).isNotNull
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
        assertThat(states.last().moreMenuItems.first().badgeState).isNull()
    }

    @Test
    fun `when building state, then payments icon displayed`() = testBlocking {
        // GIVEN
        val prefsChanges = MutableSharedFlow<Boolean>()
        setup {
            whenever(moreMenuNewFeatureHandler.moreMenuPaymentsFeatureWasClicked).thenReturn(prefsChanges)
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()
        prefsChanges.emit(false)

        // THEN
        val paymentsButton = states.last().moreMenuItems.first { it.text == R.string.more_menu_button_payments }
        assertThat(paymentsButton.icon).isEqualTo(R.drawable.ic_more_menu_payments)
        assertThat(paymentsButton.badgeState?.textColor).isEqualTo(
            R.color.color_on_surface_inverted
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
    fun `when building state, then reviews icon displayed`() = testBlocking {
        // GIVEN
        setup {
            whenever(unseenReviewsCountHandler.observeUnseenCount()).thenReturn(flowOf(1))
        }

        // WHEN
        val states = viewModel.moreMenuViewState.captureValues()

        // THEN
        val reviewsButton = states.last().moreMenuItems.first { it.text == R.string.more_menu_button_reviews }
        assertThat(reviewsButton.icon).isEqualTo(R.drawable.ic_more_menu_reviews)
        assertThat(reviewsButton.badgeState?.textColor).isEqualTo(
            R.color.color_on_surface_inverted
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
}
