package com.woocommerce.android.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.fragment.compose.AndroidFragment
import androidx.fragment.compose.rememberFragmentState
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.handleResult
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewFragment
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.CampaignStopped
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.None
import com.woocommerce.android.ui.blaze.detail.BlazeCampaignDetailWebViewViewModel.BlazeAction.PromoteProductAgain
import com.woocommerce.android.ui.common.webview.AuthenticatedWebViewLauncher
import com.woocommerce.android.ui.compose.designSystemComposeView
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ContactSupport
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.FeedbackNegativeAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.FeedbackPositiveAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenAiAssistant
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenEditWidgets
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.RefreshJitm
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShareStore
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPrivacyBanner
import com.woocommerce.android.ui.google.webview.GoogleAdsWebViewFragment
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.jitm.JitmViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivityViewModel
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.prefs.privacy.banner.PrivacyBannerFragmentDirections
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import org.wordpress.android.util.ToastUtils
import javax.inject.Inject

@AndroidEntryPoint
class DashboardFragment : TopLevelFragment() {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
    }

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val mainActivityViewModel: MainActivityViewModel by activityViewModels()

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    @Inject
    lateinit var authenticatedWebViewLauncher: AuthenticatedWebViewLauncher

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private var jitmFragment: JitmFragment? = null

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    private var wasPreviouslyStopped = false

    private val scrollToTopTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(dashboardViewModel.performanceObserver)
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = designSystemComposeView {
        val storeName by dashboardViewModel.storeName.observeAsState("")
        val appbarState by dashboardViewModel.appbarState.observeAsState()
        val jetpackBenefitsBanner by dashboardViewModel.jetpackBenefitsBannerState.observeAsState()
        val showJetpackBenefitsBanner = jetpackBenefitsBanner?.show == true
        var wasJetpackBenefitsBannerVisible by remember { mutableStateOf(false) }

        LaunchedEffect(showJetpackBenefitsBanner) {
            if (showJetpackBenefitsBanner && !wasJetpackBenefitsBannerVisible) {
                trackJetpackBenefitsBannerShown()
            }
            wasJetpackBenefitsBannerVisible = showJetpackBenefitsBanner
        }

        DashboardScreen(
            storeName = storeName,
            showShareStoreButton = appbarState?.showShareStoreButton == true,
            onShareStoreClicked = dashboardViewModel::onShareStoreClicked,
            showJetpackBenefitsBanner = showJetpackBenefitsBanner,
            onJetpackBenefitsBannerClicked = ::onJetpackBenefitsBannerClicked,
            onJetpackBenefitsBannerDismissed = { jetpackBenefitsBanner?.onDismiss?.invoke() },
            jitmContent = { modifier -> JitmHost(modifier) },
            dashboardContent = { modifier, headerScrollBridge ->
                DashboardContainer(
                    mainActivityViewModel = mainActivityViewModel,
                    dashboardViewModel = dashboardViewModel,
                    blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher,
                    scrollToTopTrigger = scrollToTopTrigger,
                    headerScrollBridge = headerScrollBridge,
                    modifier = modifier,
                )
            },
        )
    }.apply {
        id = R.id.dashboard_container
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.MY_STORE_SECTION)
        setupStateObservers()
        setupResultHandlers()
    }

    @Suppress("ComplexMethod", "MagicNumber", "LongMethod")
    private fun setupStateObservers() {
        dashboardViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ShowPrivacyBanner ->
                    findNavController().navigate(
                        PrivacyBannerFragmentDirections.actionGlobalPrivacyBannerFragment()
                    )

                is ShareStore -> ActivityUtils.shareStoreUrl(requireActivity(), event.storeUrl)

                is OpenEditWidgets -> {
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToEditWidgetsFragment()
                    )
                }

                is OpenAiAssistant -> {
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAiAssistantHostFragment()
                    )
                }

                is OpenRangePicker -> showDateRangePicker(event.start, event.end, event.callback)

                is ContactSupport -> activity?.startHelpActivity(HelpOrigin.MY_STORE)

                is FeedbackPositiveAction -> handleFeedbackRequestPositiveClick()

                is FeedbackNegativeAction -> mainNavigationRouter?.showFeedbackSurvey()

                is ShowSnackbar -> ToastUtils.showToast(requireContext(), event.message)

                is MultiLiveEvent.Event.LaunchUrlInAuthenticatedWebView ->
                    authenticatedWebViewLauncher.showAuthenticatedWebView(event)

                is RefreshJitm -> refreshJitm()

                is DashboardViewModel.DashboardEvent.OpenWooPushNotificationsIntroduction -> {
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToWooPushNotificationsIntroductionDialog()
                    )
                }

                is DashboardViewModel.DashboardEvent.OpenScheduledImportInfo -> {
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToScheduledImportInfoBottomSheet(event.isEnabled)
                    )
                }

                is DashboardViewModel.DashboardEvent.ShowScheduledImportNotice ->
                    uiMessageResolver.showActionSnack(
                        message = R.string.dashboard_stats_delayed_footer,
                        actionText = R.string.learn_more,
                        action = { dashboardViewModel.onDelayedStatsInfoClicked() }
                    )

                else -> event.isHandled = false
            }
        }
    }

    private fun setupResultHandlers() {
        handleNotice(GoogleAdsWebViewFragment.WEBVIEW_RESULT) {
            navigateToGoogleAdsCreationSuccess()
        }
        handleResult<BlazeAction>(BlazeCampaignDetailWebViewFragment.BLAZE_WEBVIEW_RESULT) {
            when (it) {
                None,
                CampaignStopped -> Unit // We don't need to handle actions here
                is PromoteProductAgain ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        blazeCampaignCreationDispatcher.startCampaignCreation(
                            BlazeFlowSource.MY_STORE_SECTION,
                            it.productId
                        )
                    }
            }
        }
    }

    private fun trackJetpackBenefitsBannerShown() {
        AnalyticsTracker.track(
            stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "shown")
        )
    }

    private fun onJetpackBenefitsBannerClicked() {
        AnalyticsTracker.track(
            stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
            properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "tapped")
        )
        findNavController().navigateSafely(DashboardFragmentDirections.actionDashboardToJetpackBenefitsDialog())
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        // Avoid executing interacted() on first load. Only when the user navigated away from the fragment.
        if (wasPreviouslyStopped) {
            usageTracksEventEmitter.interacted()
            wasPreviouslyStopped = false
        }
        dashboardViewModel.onResume()
    }

    override fun onStop() {
        wasPreviouslyStopped = true
        super.onStop()
    }

    override fun onDestroyView() {
        jitmFragment = null
        super.onDestroyView()
    }

    @Composable
    private fun JitmHost(modifier: Modifier = Modifier) {
        val fragmentState = rememberFragmentState()
        val arguments = remember {
            Bundle().apply {
                putString(JitmViewModel.JITM_MESSAGE_PATH_KEY, JitmMessagePathsProvider.MY_STORE)
            }
        }

        DisposableEffect(Unit) {
            onDispose { jitmFragment = null }
        }
        // Fragment Compose creates a MATCH_PARENT-height host. Intrinsics make it measure its content height.
        AndroidFragment<JitmFragment>(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            fragmentState = fragmentState,
            arguments = arguments,
            onUpdate = { jitmFragment = it },
        )
    }

    private fun refreshJitm() {
        jitmFragment?.refreshJitms()
    }

    override fun getFragmentTitle() = getString(R.string.my_store)

    private fun handleFeedbackRequestPositiveClick() {
        // Request a ReviewInfo object from the Google Reviews API. If this fails
        // we just move on as there isn't anything we can do.
        val manager = ReviewManagerFactory.create(requireContext())
        val reviewRequest = manager.requestReviewFlow()
        reviewRequest.addOnCompleteListener {
            if (activity != null && it.isSuccessful) {
                // Request to start the Review flow so the user can be prompted to submit
                // a play store review. The prompt will only appear if the user hasn't already
                // reached their quota for how often we can ask for a review.
                val reviewInfo = it.result
                val flow = manager.launchReviewFlow(requireActivity(), reviewInfo)
                flow.addOnFailureListener { ex ->
                    WooLog.e(WooLog.T.DASHBOARD, "Error launching google review API flow.", ex)
                }
            } else {
                // There was an error, just log and continue. Google doesn't really tell you what
                // type of scenario would cause an error.
                WooLog.e(
                    WooLog.T.DASHBOARD,
                    "Error fetching ReviewInfo object from Review API to start in-app review process",
                    it.exception
                )
            }
        }
    }

    private fun navigateToGoogleAdsCreationSuccess() {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalGoogleAdsCampaignSuccessBottomSheet()
        )
    }

    override fun shouldExpandToolbar() = false

    override fun scrollToTop() {
        scrollToTopTrigger.tryEmit(Unit)
    }
}
