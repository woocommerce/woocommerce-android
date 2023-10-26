package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.R.attr
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentMyStoreBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.extensions.verticalOffsetChanges
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.MyStoreBlazeView
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel
import com.woocommerce.android.ui.blaze.MyStoreBlazeViewModel.MyStoreBlazeCampaignState
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingCollapsed
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingViewModel
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingViewModel.NavigateToSetupPayments.taskId
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenAnalytics
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenTopPerformer
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.ShareStore
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.ShowAIProductDescriptionDialog
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.ShowPrivacyBanner
import com.woocommerce.android.ui.mystore.MyStoreViewModel.OrderState
import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsViewState
import com.woocommerce.android.ui.mystore.MyStoreViewModel.VisitorStatsViewState
import com.woocommerce.android.ui.prefs.privacy.banner.PrivacyBannerFragmentDirections
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
@OptIn(FlowPreview::class)
class MyStoreFragment :
    TopLevelFragment(R.layout.fragment_my_store),
    MenuProvider {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName

        fun newInstance() = MyStoreFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS

        private const val JITM_FRAGMENT_TAG = "jitm_fragment"
    }

    private val myStoreViewModel: MyStoreViewModel by viewModels()
    private val storeOnboardingViewModel: StoreOnboardingViewModel by activityViewModels()
    private val myStoreBlazeViewModel: MyStoreBlazeViewModel by viewModels()

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var dateUtils: DateUtils

    @Inject
    lateinit var usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var feedbackPrefs: FeedbackPrefs

    @Inject
    lateinit var addProductNavigator: AddProductNavigator

    private var _binding: FragmentMyStoreBinding? = null
    private val binding get() = _binding!!

    private var errorSnackbar: Snackbar? = null

    private var _tabLayout: TabLayout? = null
    private val tabLayout
        get() = _tabLayout!!

    private val appBarLayout
        get() = activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = null,
            hasShadow = true
        )

    private var isEmptyViewVisible: Boolean = false
    private var wasPreviouslyStopped = false

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            myStoreViewModel.onStatsGranularityChanged(tab.tag as StatsGranularity)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}

        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(myStoreViewModel.performanceObserver)
        lifecycle.addObserver(storeOnboardingViewModel)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        initTabLayout()
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)

        _binding = FragmentMyStoreBinding.bind(view)

        binding.myStoreRefreshLayout.setOnRefreshListener {
            binding.myStoreRefreshLayout.isRefreshing = false
            myStoreViewModel.onPullToRefresh()
            storeOnboardingViewModel.onPullToRefresh()
            binding.myStoreStats.clearStatsHeaderValues()
            binding.myStoreStats.clearChartData()
            refreshJitm()
        }

        // Create tabs and add to appbar
        StatsGranularity.values().forEach { granularity ->
            val tab = tabLayout.newTab().apply {
                setText(binding.myStoreStats.getStringForGranularity(granularity))
                tag = granularity
            }
            tabLayout.addTab(tab)
        }

        binding.myStoreStats.initView(
            myStoreViewModel.activeStatsGranularity.value ?: DEFAULT_STATS_GRANULARITY,
            selectedSite,
            dateUtils,
            currencyFormatter,
            usageTracksEventEmitter,
            viewLifecycleOwner.lifecycleScope
        ) { myStoreViewModel.onViewAnalyticsClicked() }

        binding.myStoreTopPerformers.initView(selectedSite, dateUtils)

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        binding.myStoreStatsAvailabilityMessage.setClickableText(
            content = getString(R.string.my_store_stats_availability_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(HelpOrigin.MY_STORE) }
        )

        prepareJetpackBenefitsBanner()

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        binding.statsScrollView.scrollStartEvents()
            .onEach { usageTracksEventEmitter.interacted() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        setupStateObservers()
        setupOnboardingView()
        setUpBlazeCampaignView()

        initJitm(savedInstanceState)
    }

    private fun setUpBlazeCampaignView() {
        myStoreBlazeViewModel.blazeCampaignState.observe(viewLifecycleOwner) { blazeCampaignState ->
            if (blazeCampaignState is MyStoreBlazeCampaignState.Hidden) binding.blazeCampaignView.hide()
            else {
                binding.blazeCampaignView.apply {
                    setContent {
                        WooThemeWithBackground {
                            MyStoreBlazeView(
                                state = blazeCampaignState
                            )
                        }
                    }
                    show()
                }
            }
        }
        myStoreBlazeViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MyStoreBlazeViewModel.LaunchBlazeCampaignCreation -> openBlazeWebView(
                    url = event.url,
                    source = event.source
                )

                is MyStoreBlazeViewModel.ShowAllCampaigns -> {
                    findNavController().navigateSafely(
                        MyStoreFragmentDirections.actionMyStoreToBlazeCampaignListFragment()
                    )
                }

                is MyStoreBlazeViewModel.ShowCampaignDetails -> {
                    findNavController().navigateSafely(
                        NavGraphMainDirections.actionGlobalWPComWebViewFragment(
                            urlToLoad = event.url,
                            urlsToTriggerExit = arrayOf(event.urlToTriggerExit),
                            title = getString(R.string.blaze_campaign_details_title)
                        )
                    )
                }
            }
        }
    }

    private fun openBlazeWebView(url: String, source: BlazeFlowSource) {
        findNavController().navigateSafely(
            NavGraphMainDirections.actionGlobalBlazeCampaignCreationFragment(
                urlToLoad = url,
                source = source
            )
        )
    }

    @Suppress("LongMethod")
    private fun setupOnboardingView() {
        storeOnboardingViewModel.viewState.observe(viewLifecycleOwner) { state ->
            when (state.show) {
                false -> binding.storeOnboardingView.hide()
                else -> {
                    binding.storeOnboardingView.apply {
                        show()
                        AnalyticsTracker.track(stat = AnalyticsEvent.STORE_ONBOARDING_SHOWN)
                        // Dispose of the Composition when the view's LifecycleOwner is destroyed
                        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                        setContent {
                            WooThemeWithBackground {
                                StoreOnboardingCollapsed(
                                    onboardingState = state,
                                    onViewAllClicked = storeOnboardingViewModel::viewAllClicked,
                                    onShareFeedbackClicked = storeOnboardingViewModel::onShareFeedbackClicked,
                                    onTaskClicked = storeOnboardingViewModel::onTaskClicked,
                                    onHideOnboardingClicked = storeOnboardingViewModel::onHideOnboardingClicked
                                )
                            }
                        }
                    }
                }
            }
        }

        storeOnboardingViewModel.event.observe(viewLifecycleOwner) { event ->
            event.handle()
        }
    }

    private fun Event.handle() {
        when (this) {
            is StoreOnboardingViewModel.NavigateToOnboardingFullScreen -> openOnboardingInFullScreen()
            is StoreOnboardingViewModel.NavigateToSurvey ->
                NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.STORE_ONBOARDING).apply {
                    findNavController().navigateSafely(this)
                }

            is StoreOnboardingViewModel.NavigateToLaunchStore ->
                findNavController().navigateSafely(
                    directions = MyStoreFragmentDirections.actionMyStoreToLaunchStoreFragment()
                )

            is StoreOnboardingViewModel.NavigateToDomains ->
                findNavController().navigateSafely(
                    directions = MyStoreFragmentDirections.actionMyStoreToNavGraphDomainChange()
                )

            is StoreOnboardingViewModel.NavigateToAddProduct ->
                with(addProductNavigator) {
                    findNavController().navigateToAddProducts(
                        aiBottomSheetAction = MyStoreFragmentDirections.actionDashboardToAddProductWithAIBottomSheet(),
                        typesBottomSheetAction = MyStoreFragmentDirections.actionMyStoreToProductTypesBottomSheet()
                    )
                }

            is StoreOnboardingViewModel.NavigateToSetupPayments ->
                findNavController().navigateSafely(
                    directions = MyStoreFragmentDirections.actionMyStoreToPaymentsPreSetupFragment(
                        taskId = taskId
                    )
                )

            is StoreOnboardingViewModel.NavigateToSetupWooPayments ->
                findNavController().navigateSafely(
                    directions = MyStoreFragmentDirections.actionMyStoreToWooPaymentsSetupInstructionsFragment()
                )

            is StoreOnboardingViewModel.NavigateToAboutYourStore ->
                findNavController().navigateSafely(
                    MyStoreFragmentDirections.actionMyStoreToAboutYourStoreFragment()
                )

            is StoreOnboardingViewModel.ShowNameYourStoreDialog -> {
                findNavController()
                    .navigateSafely(
                        MyStoreFragmentDirections.actionMyStoreToNameYourStoreDialogFragment(fromOnboarding = true)
                    )
            }

            is ShowDialog -> showDialog()
        }
    }

    private fun openOnboardingInFullScreen() {
        exitTransition = MaterialElevationScale(false).apply {
            duration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = resources.getInteger(R.integer.default_fragment_transition).toLong()
        }
        val transitionName = getString(R.string.store_onboarding_full_screen_transition_name)
        val extras = FragmentNavigatorExtras(binding.storeOnboardingView to transitionName)
        findNavController().navigateSafely(
            directions = MyStoreFragmentDirections.actionMyStoreToOnboardingFragment(),
            extras = extras
        )
    }

    @Suppress("ComplexMethod", "MagicNumber", "LongMethod")
    private fun setupStateObservers() {
        myStoreViewModel.appbarState.observe(viewLifecycleOwner) { requireActivity().invalidateOptionsMenu() }

        myStoreViewModel.activeStatsGranularity.observe(viewLifecycleOwner) { activeGranularity ->
            if (tabLayout.getTabAt(tabLayout.selectedTabPosition)?.tag != activeGranularity) {
                val index = StatsGranularity.values().indexOf(activeGranularity)
                // Small delay needed to ensure tablayout scrolls to the selected tab if tab is not visible on screen.
                handler.postDelayed({ tabLayout.getTabAt(index)?.select() }, 300)
            }
            binding.myStoreStats.loadDashboardStats(activeGranularity)
            binding.myStoreTopPerformers.onDateGranularityChanged(activeGranularity)
        }
        myStoreViewModel.revenueStatsState.observe(viewLifecycleOwner) { revenueStats ->
            when (revenueStats) {
                is RevenueStatsViewState.Content -> showStats(revenueStats.revenueStats)
                RevenueStatsViewState.GenericError -> showStatsError()
                RevenueStatsViewState.Loading -> showChartSkeleton(true)
                RevenueStatsViewState.PluginNotActiveError -> updateStatsAvailabilityError()
            }
        }
        myStoreViewModel.visitorStatsState.observe(viewLifecycleOwner) { stats ->
            when (stats) {
                is VisitorStatsViewState.Content -> showVisitorStats(stats.stats)
                VisitorStatsViewState.Error -> {
                    binding.jetpackBenefitsBanner.root.isVisible = false
                    binding.myStoreStats.showVisitorStatsError()
                }

                is VisitorStatsViewState.Unavailable -> onVisitorStatsUnavailable(stats)
            }
        }
        myStoreViewModel.topPerformersState.observe(viewLifecycleOwner) { topPerformers ->
            when {
                topPerformers.isLoading -> showTopPerformersLoading()
                topPerformers.isError -> showTopPerformersError()
                else -> showTopPerformers(topPerformers.topPerformers)
            }
        }
        myStoreViewModel.hasOrders.observe(viewLifecycleOwner) { newValue ->
            when (newValue) {
                OrderState.Empty -> showEmptyView(true)
                OrderState.AtLeastOne -> showEmptyView(false)
            }
        }
        myStoreViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenTopPerformer -> findNavController().navigateSafely(
                    NavGraphMainDirections.actionGlobalProductDetailFragment(
                        remoteProductId = event.productId,
                        isTrashEnabled = false
                    )
                )

                is OpenAnalytics -> {
                    mainNavigationRouter?.showAnalytics(event.analyticsPeriod)
                }

                is ShowPrivacyBanner ->
                    findNavController().navigate(
                        PrivacyBannerFragmentDirections.actionGlobalPrivacyBannerFragment()
                    )

                is ShareStore -> ActivityUtils.shareStoreUrl(requireActivity(), event.storeUrl)

                is ShowAIProductDescriptionDialog ->
                    findNavController().navigateSafely(
                        MyStoreFragmentDirections.actionDashboardToAIProductDescriptionDialogFragment()
                    )

                else -> event.isHandled = false
            }
        }
        myStoreViewModel.lastUpdateStats.observe(viewLifecycleOwner) { lastUpdateMillis ->
            binding.myStoreStats.showLastUpdate(lastUpdateMillis)
        }
        myStoreViewModel.lastUpdateTopPerformers.observe(viewLifecycleOwner) { lastUpdateMillis ->
            binding.myStoreTopPerformers.showLastUpdate(lastUpdateMillis)
        }
        myStoreViewModel.storeName.observe(viewLifecycleOwner) { storeName ->
            ((activity) as MainActivity).setSubtitle(storeName)
        }
    }

    private fun onVisitorStatsUnavailable(state: VisitorStatsViewState.Unavailable) {
        handleUnavailableVisitorStats()

        val jetpackBenefitsBanner = state.benefitsBanner
        if (jetpackBenefitsBanner.show) {
            binding.jetpackBenefitsBanner.dismissButton.setOnClickListener {
                jetpackBenefitsBanner.onDismiss()
            }
        }
        if (jetpackBenefitsBanner.show && !binding.jetpackBenefitsBanner.root.isVisible) {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "shown")
            )
        }
        binding.jetpackBenefitsBanner.root.isVisible = jetpackBenefitsBanner.show
    }

    private fun showTopPerformersLoading() {
        binding.myStoreTopPerformers.showErrorView(false)
        binding.myStoreTopPerformers.showSkeleton(true)
    }

    @Suppress("ForbiddenComment")
    private fun prepareJetpackBenefitsBanner() {
        appPrefsWrapper.setJetpackInstallationIsFromBanner(false)
        binding.jetpackBenefitsBanner.root.isVisible = false
        binding.jetpackBenefitsBanner.root.setOnClickListener {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "tapped")
            )
            appPrefsWrapper.setJetpackInstallationIsFromBanner(true)
            findNavController().navigateSafely(MyStoreFragmentDirections.actionMyStoreToJetpackBenefitsDialog())
        }
        val appBarLayout = appBarLayout ?: return
        // For the banner to be above the bottom navigation view when the toolbar is expanded
        viewLifecycleOwner.lifecycleScope.launch {
            // Due to this issue https://issuetracker.google.com/issues/181325977, we need to make sure
            // we are using `withCreated` here, since if this view doesn't reach the created state,
            // the scope will not get cancelled.
            // TODO: revisit this once https://issuetracker.google.com/issues/127528777 is implemented
            // (no update as of Oct 2023)
            withCreated {
                appBarLayout.verticalOffsetChanges()
                    .onEach { verticalOffset ->
                        binding.jetpackBenefitsBanner.root.translationY =
                            (abs(verticalOffset) - appBarLayout.totalScrollRange).toFloat()
                    }
                    .launchIn(this)
            }
        }
    }

    private fun initTabLayout() {
        _tabLayout = TabLayout(requireContext(), null, attr.scrollableTabStyle)
        addTabLayoutToAppBar()
    }

    override fun onResume() {
        super.onResume()
        handleFeedbackRequestCardState()
        AnalyticsTracker.trackViewShown(this)
        // Avoid executing interacted() on first load. Only when the user navigated away from the fragment.
        if (wasPreviouslyStopped) {
            usageTracksEventEmitter.interacted()
            wasPreviouslyStopped = false
        }
    }

    override fun onStop() {
        wasPreviouslyStopped = true
        errorSnackbar?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
        handler.removeCallbacksAndMessages(null)
        removeTabLayoutFromAppBar()
        tabLayout.removeOnTabSelectedListener(tabSelectedListener)
        _tabLayout = null
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        lifecycle.removeObserver(storeOnboardingViewModel)
        super.onDestroy()
    }

    private fun showStats(revenueStatsModel: RevenueStatsUiModel?) {
        addTabLayoutToAppBar()
        binding.myStoreStats.showErrorView(false)
        showChartSkeleton(false)

        binding.myStoreStats.updateView(revenueStatsModel)

        // update the stats today widget if we're viewing today's stats
        if (myStoreViewModel.activeStatsGranularity.value == StatsGranularity.DAYS) {
            (activity as? MainActivity)?.updateStatsWidgets()
        }
    }

    private fun showStatsError() {
        showChartSkeleton(false)
        binding.myStoreStats.showErrorView(true)
        showErrorSnack()
    }

    private fun updateStatsAvailabilityError() {
        binding.myStoreRefreshLayout.visibility = View.GONE
        WooAnimUtils.fadeIn(binding.statsErrorScrollView)
        removeTabLayoutFromAppBar()
        showChartSkeleton(false)
    }

    private fun showTopPerformers(topPerformers: List<TopPerformerProductUiModel>) {
        binding.myStoreTopPerformers.showSkeleton(false)
        binding.myStoreTopPerformers.showErrorView(false)
        binding.myStoreTopPerformers.updateView(topPerformers)
    }

    private fun showTopPerformersError() {
        binding.myStoreTopPerformers.showSkeleton(false)
        binding.myStoreTopPerformers.showErrorView(true)
        showErrorSnack()
    }

    private fun showVisitorStats(visitorStats: Map<String, Int>) {
        binding.jetpackBenefitsBanner.root.isVisible = false
        binding.myStoreStats.showVisitorStats(visitorStats)
    }

    private fun handleUnavailableVisitorStats() {
        binding.myStoreStats.handleUnavailableVisitorStats()
    }

    private fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == false || NetworkUtils.isNetworkAvailable(context)) {
            errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
            errorSnackbar?.show()
        }
    }

    private fun initJitm(savedInstanceState: Bundle?) {
        // Show banners only if onboarding list is not displayed
        if (!binding.storeOnboardingView.isVisible && savedInstanceState == null) {
            childFragmentManager.beginTransaction()
                .replace(
                    R.id.jitmFragment,
                    JitmFragment.newInstance(JitmMessagePathsProvider.MY_STORE),
                    JITM_FRAGMENT_TAG
                )
                .commit()
        }
    }

    private fun refreshJitm() {
        childFragmentManager.findFragmentByTag(JITM_FRAGMENT_TAG)?.let {
            (it as JitmFragment).refreshJitms()
        }
    }

    override fun getFragmentTitle() = getString(R.string.my_store)

    override fun getFragmentSubtitle(): String = myStoreViewModel.storeName.value ?: ""

    override fun scrollToTop() {
        binding.statsScrollView.smoothScrollTo(0, 0)
    }

    private fun showChartSkeleton(show: Boolean) {
        binding.myStoreStats.showErrorView(false)
        binding.myStoreStats.showSkeleton(show)
    }

    /**
     * This method verifies if the feedback card should be visible.
     *
     * If it should but it's not, the feedback card is reconfigured and presented
     * If should not and it's visible, the card visibility is changed to gone
     * If should be and it's already visible, nothing happens
     */
    private fun handleFeedbackRequestCardState() = with(binding.storeFeedbackRequestCard) {
        if (feedbackPrefs.userFeedbackIsDue && visibility == View.GONE) {
            setupFeedbackRequestCard()
        } else if (feedbackPrefs.userFeedbackIsDue.not() && visibility == View.VISIBLE) {
            visibility = View.GONE
        }
    }

    private fun setupFeedbackRequestCard() {
        binding.storeFeedbackRequestCard.visibility = View.VISIBLE
        val negativeCallback = {
            mainNavigationRouter?.showFeedbackSurvey()
            binding.storeFeedbackRequestCard.visibility = View.GONE
            feedbackPrefs.lastFeedbackDate = Calendar.getInstance().time
        }
        binding.storeFeedbackRequestCard.initView(negativeCallback, ::handleFeedbackRequestPositiveClick)
    }

    private fun handleFeedbackRequestPositiveClick() {
        // set last feedback date to now and hide the card
        feedbackPrefs.lastFeedbackDate = Calendar.getInstance().time
        binding.storeFeedbackRequestCard.visibility = View.GONE

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

    private fun showEmptyView(show: Boolean) {
        val dashboardVisibility: Int
        if (show) {
            dashboardVisibility = View.GONE
            binding.emptyView.show(EmptyViewType.DASHBOARD) {
                myStoreViewModel.onShareStoreClicked()
            }
        } else {
            binding.emptyView.hide()
            dashboardVisibility = View.VISIBLE
        }
        tabLayout.visibility = dashboardVisibility
        binding.myStoreStats.visibility = dashboardVisibility
        binding.myStoreTopPerformers.visibility = dashboardVisibility
        isEmptyViewVisible = show
    }

    private fun addTabLayoutToAppBar() {
        appBarLayout
            ?.takeIf { !it.children.contains(tabLayout) }
            ?.takeIf { AppPrefs.isV4StatsSupported() }
            ?.let { appBar ->
                appBar.addView(tabLayout, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
                appBar.post {
                    if (context != null) {
                        appBar.elevation = resources.getDimensionPixelSize(R.dimen.appbar_elevation).toFloat()
                    }
                }
            }
    }

    private fun removeTabLayoutFromAppBar() {
        appBarLayout?.let { appBar ->
            appBar.removeView(tabLayout)
            appBar.elevation = 0f
        }
    }

    override fun shouldExpandToolbar() = binding.statsScrollView.scrollY == 0

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_my_store_fragment, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_share_store -> {
                myStoreViewModel.onShareStoreClicked()
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_share_store).isVisible =
            myStoreViewModel.appbarState.value?.showShareStoreButton ?: false
    }
}
