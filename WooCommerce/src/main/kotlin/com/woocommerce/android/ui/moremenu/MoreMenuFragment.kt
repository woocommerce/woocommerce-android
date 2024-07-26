package com.woocommerce.android.ui.moremenu

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewFragment
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.NavigateToSettingsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.NavigateToSubscriptionsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.NavigateToWooPosEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.OpenBlazeCampaignCreationEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.OpenBlazeCampaignListEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.StartSitePickerEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewAdminEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewCouponsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewCustomersEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewGoogleForWooEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewInboxEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewPayments
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewReviewsEvent
import com.woocommerce.android.ui.moremenu.MoreMenuEvent.ViewStoreEvent
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.woopos.root.WooPosActivity
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MoreMenuFragment : TopLevelFragment() {
    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

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
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.MORE_MENU_ITEM)
        setupObservers()
        setupResultHandlers()
    }

    override fun onResume() {
        super.onResume()

        viewModel.onViewResumed()
    }

    @Suppress("CyclomaticComplexMethod")
    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is NavigateToSettingsEvent -> navigateToSettings()
                is NavigateToSubscriptionsEvent -> navigateToSubscriptions()
                is StartSitePickerEvent -> startSitePicker()
                is ViewGoogleForWooEvent -> openGoogleAdsWebview(event.url, event.isCreationFlow)
                is ViewAdminEvent -> openInBrowser(event.url)
                is ViewStoreEvent -> openInBrowser(event.url)
                is ViewReviewsEvent -> navigateToReviews()
                is ViewInboxEvent -> navigateToInbox()
                is ViewCouponsEvent -> navigateToCoupons()
                is ViewCustomersEvent -> navigateToCustomers()
                is ViewPayments -> navigateToPayments()
                is OpenBlazeCampaignCreationEvent -> openBlazeCreationFlow()
                is OpenBlazeCampaignListEvent -> openBlazeCampaignList()
                is NavigateToWooPosEvent -> openWooPos()
            }
        }
    }

    private fun setupResultHandlers() {
        handleNotice(GoogleAdsWebViewFragment.WEBVIEW_RESULT) {
            navigateToGoogleAdsCreationSuccess()
            viewModel.handleSuccessfulGoogleAdsCreation()
        }
    }

    private fun openWooPos() {
        startActivity(Intent(requireContext(), WooPosActivity::class.java))
    }

    private fun openBlazeCampaignList() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToBlazeCampaignListFragment()
        )
    }

    private fun openBlazeCreationFlow() {
        lifecycleScope.launch {
            blazeCampaignCreationDispatcher.startCampaignCreation(source = BlazeFlowSource.MORE_MENU_ITEM)
        }
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

    private fun navigateToGoogleAdsCreationSuccess() {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalGoogleAdsCampaignSuccessBottomSheet()
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

    private fun navigateToCustomers() {
        findNavController().navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToCustomerListFragment()
        )
    }

    private fun openGoogleAdsWebview(url: String, isCreationFlow: Boolean) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalGoogleAdsWebViewFragment(
                urlToLoad = url,
                title = getString(R.string.more_menu_button_google),
                urlComparisonMode = GoogleAdsWebViewViewModel.UrlComparisonMode.PARTIAL,
                isCreationFlow = isCreationFlow,
                entryPointSource = GoogleAdsWebViewViewModel.EntryPointSource.MORE_MENU
            )
        )
    }
}
