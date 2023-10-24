package com.woocommerce.android.ui.moremenu

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.NavigateToSettingsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.NavigateToSubscriptionsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.OpenBlazeCampaignCreationEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.OpenBlazeCampaignListEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.StartSitePickerEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewAdminEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewCouponsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewInboxEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewPayments
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewReviewsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuViewModel.MoreMenuEvent.ViewStoreEvent
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
@ExperimentalFoundationApi
class MoreMenuFragment : TopLevelFragment() {
    @Inject lateinit var selectedSite: SelectedSite

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun getFragmentTitle() = getString(R.string.more_menu)

    override fun shouldExpandToolbar(): Boolean = false

    private val viewModel: MoreMenuViewModel by viewModels()

    override fun scrollToTop() {
        return
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            id = R.id.more_menu_compose_view
            // Dispose of the Composition when the view's LifecycleOwner is destroyed
            setViewCompositionStrategy(DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    MoreMenuScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onViewResumed()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToSettingsEvent -> navigateToSettings()
                is NavigateToSubscriptionsEvent -> navigateToSubscriptions()
                is StartSitePickerEvent -> startSitePicker()
                is ViewAdminEvent -> openInBrowser(event.url)
                is ViewStoreEvent -> openInBrowser(event.url)
                is ViewReviewsEvent -> navigateToReviews()
                is ViewInboxEvent -> navigateToInbox()
                is ViewCouponsEvent -> navigateToCoupons()
                is ViewPayments -> navigateToPayments()
                is OpenBlazeCampaignCreationEvent -> openBlazeWebView(event)
                is OpenBlazeCampaignListEvent -> openBlazeCampaignList()
            }
        }
    }
    private fun openBlazeCampaignList() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToBlazeCampaignListFragment()
        )
    }

    private fun openBlazeWebView(event: OpenBlazeCampaignCreationEvent) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalBlazeCampaignCreationFragment(
                urlToLoad = event.url,
                source = event.source
            )
        )
    }

    private fun navigateToPayments() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToPaymentFlow(CardReaderFlowParam.CardReadersHub())
        )
    }

    private fun navigateToSettings() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToSettingsActivity()
        )
    }

    private fun navigateToSubscriptions() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToSubscriptions()
        )
    }

    private fun startSitePicker() {
        (requireActivity() as MainActivity).startSitePicker()
    }

    private fun openInBrowser(url: String) {
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    private fun navigateToReviews() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToReviewList()
        )
    }

    private fun navigateToInbox() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuFragmentToInboxFragment()
        )
    }

    private fun navigateToCoupons() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToCouponListFragment()
        )
    }
}
