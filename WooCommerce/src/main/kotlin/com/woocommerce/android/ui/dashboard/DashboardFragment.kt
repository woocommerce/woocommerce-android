package com.woocommerce.android.ui.dashboard

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
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
import com.google.android.material.transition.MaterialElevationScale
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentDashboardBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.extensions.verticalOffsetChanges
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenEditWidgets
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenTopPerformer
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShareStore
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowAIProductDescriptionDialog
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPluginUnavailableError
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPrivacyBanner
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowStatsError
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.onboarding.StoreOnboardingCollapsed
import com.woocommerce.android.ui.onboarding.StoreOnboardingViewModel
import com.woocommerce.android.ui.onboarding.StoreOnboardingViewModel.NavigateToSetupPayments.taskId
import com.woocommerce.android.ui.prefs.privacy.banner.PrivacyBannerFragmentDirections
import com.woocommerce.android.ui.products.AddProductNavigator
import com.woocommerce.android.ui.products.ProductDetailFragment
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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class DashboardFragment :
    TopLevelFragment(R.layout.fragment_dashboard),
    MenuProvider {
    companion object {
        val TAG: String = DashboardFragment::class.java.simpleName
        fun newInstance() = DashboardFragment()
        private const val JITM_FRAGMENT_TAG = "jitm_fragment"
    }

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val storeOnboardingViewModel: StoreOnboardingViewModel by activityViewModels()

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var dateUtils: DateUtils

    @Inject
    lateinit var usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var feedbackPrefs: FeedbackPrefs

    @Inject
    lateinit var addProductNavigator: AddProductNavigator

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private var errorSnackbar: Snackbar? = null

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = null,
            hasShadow = true,
        )

    private var isEmptyViewVisible: Boolean = false
    private var wasPreviouslyStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(dashboardViewModel.performanceObserver)
        lifecycle.addObserver(storeOnboardingViewModel)
        super.onCreate(savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        requireActivity().addMenuProvider(this, viewLifecycleOwner, Lifecycle.State.RESUMED)
        blazeCampaignCreationDispatcher.attachFragment(this, BlazeFlowSource.MY_STORE_SECTION)

        _binding = FragmentDashboardBinding.bind(view)

        binding.dashboardContainer.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    DashboardContainer(
                        dateUtils = dateUtils,
                        currencyFormatter = currencyFormatter,
                        usageTracksEventEmitter = usageTracksEventEmitter,
                        dashboardViewModel = dashboardViewModel,
                        blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher
                    )
                }
            }
        }

        binding.myStoreRefreshLayout.setOnRefreshListener {
            binding.myStoreRefreshLayout.isRefreshing = false
            dashboardViewModel.onPullToRefresh()
            storeOnboardingViewModel.onPullToRefresh()
            refreshJitm()
        }

        binding.myStoreTopPerformers.initView(selectedSite, dateUtils)

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        binding.myStoreStatsAvailabilityMessage.setClickableText(
            content = getString(R.string.my_store_stats_availability_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(HelpOrigin.MY_STORE) }
        )

        prepareJetpackBenefitsBanner()

        binding.statsScrollView.scrollStartEvents()
            .onEach { usageTracksEventEmitter.interacted() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        setupStateObservers()
        setupOnboardingView()

        initJitm(savedInstanceState)
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
                    directions = DashboardFragmentDirections.actionDashboardToLaunchStoreFragment()
                )

            is StoreOnboardingViewModel.NavigateToDomains ->
                findNavController().navigateSafely(
                    directions = DashboardFragmentDirections.actionDashboardToNavGraphDomainChange()
                )

            is StoreOnboardingViewModel.NavigateToAddProduct ->
                with(addProductNavigator) {
                    findNavController().navigateToAddProducts(
                        aiBottomSheetAction = DashboardFragmentDirections
                            .actionDashboardToAddProductWithAIBottomSheet(),
                        typesBottomSheetAction = DashboardFragmentDirections.actionDashboardToProductTypesBottomSheet()
                    )
                }

            is StoreOnboardingViewModel.NavigateToSetupPayments ->
                findNavController().navigateSafely(
                    directions = DashboardFragmentDirections.actionDashboardToPaymentsPreSetupFragment(
                        taskId = taskId
                    )
                )

            is StoreOnboardingViewModel.NavigateToSetupWooPayments ->
                findNavController().navigateSafely(
                    directions = DashboardFragmentDirections.actionDashboardToWooPaymentsSetupInstructionsFragment()
                )

            is StoreOnboardingViewModel.NavigateToAboutYourStore ->
                findNavController().navigateSafely(
                    DashboardFragmentDirections.actionDashboardToAboutYourStoreFragment()
                )

            is StoreOnboardingViewModel.ShowNameYourStoreDialog -> {
                findNavController()
                    .navigateSafely(
                        DashboardFragmentDirections.actionDashboardToNameYourStoreDialogFragment(fromOnboarding = true)
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
            directions = DashboardFragmentDirections.actionDashboardToOnboardingFragment(),
            extras = extras
        )
    }

    @Suppress("ComplexMethod", "MagicNumber", "LongMethod")
    private fun setupStateObservers() {
        dashboardViewModel.appbarState.observe(viewLifecycleOwner) { requireActivity().invalidateOptionsMenu() }

        dashboardViewModel.selectedDateRange.observe(viewLifecycleOwner) { statsTimeRangeSelection ->
            binding.myStoreTopPerformers.onDateGranularityChanged(statsTimeRangeSelection.selectionType)
        }
        dashboardViewModel.topPerformersState.observe(viewLifecycleOwner) { topPerformers ->
            when {
                topPerformers.isLoading -> showTopPerformersLoading()
                topPerformers.isError -> showTopPerformersError()
                else -> showTopPerformers(topPerformers.topPerformers)
            }
        }
        dashboardViewModel.hasOrders.observe(viewLifecycleOwner) { newValue ->
            when (newValue) {
                DashboardViewModel.OrderState.Empty -> showEmptyView(true)
                DashboardViewModel.OrderState.AtLeastOne -> showEmptyView(false)
            }
        }
        dashboardViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenTopPerformer -> findNavController().navigateSafely(
                    NavGraphMainDirections.actionGlobalProductDetailFragment(
                        mode = ProductDetailFragment.Mode.ShowProduct(event.productId),
                        isTrashEnabled = false
                    )
                )

                is ShowPrivacyBanner ->
                    findNavController().navigate(
                        PrivacyBannerFragmentDirections.actionGlobalPrivacyBannerFragment()
                    )

                is ShareStore -> ActivityUtils.shareStoreUrl(requireActivity(), event.storeUrl)

                is ShowAIProductDescriptionDialog ->
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToAIProductDescriptionDialogFragment()
                    )

                is OpenEditWidgets -> {
                    findNavController().navigateSafely(
                        DashboardFragmentDirections.actionDashboardToEditWidgetsFragment()
                    )
                }

                is ShowStatsError -> showErrorSnack()

                is OpenRangePicker -> showDateRangePicker(event.start, event.end, event.callback)

                is ShowPluginUnavailableError -> showPluginUnavailableError()

                else -> event.isHandled = false
            }
        }
        dashboardViewModel.lastUpdateTopPerformers.observe(viewLifecycleOwner) { lastUpdateMillis ->
            binding.myStoreTopPerformers.showLastUpdate(lastUpdateMillis)
        }
        dashboardViewModel.storeName.observe(viewLifecycleOwner) { storeName ->
            ((activity) as MainActivity).setSubtitle(storeName)
        }
        dashboardViewModel.jetpackBenefitsBannerState.observe(viewLifecycleOwner) { jetpackBenefitsBanner ->
            onVisitorStatsUnavailable(jetpackBenefitsBanner)
        }
    }

    private fun onVisitorStatsUnavailable(jetpackBenefitsBanner: DashboardViewModel.JetpackBenefitsBannerUiModel?) {
        if (jetpackBenefitsBanner == null) {
            binding.jetpackBenefitsBanner.root.isVisible = false
            return
        }

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
            findNavController().navigateSafely(DashboardFragmentDirections.actionDashboardToJetpackBenefitsDialog())
        }
        // For the banner to be above the bottom navigation view when the toolbar is expanded
        viewLifecycleOwner.lifecycleScope.launch {
            // Due to this issue https://issuetracker.google.com/issues/181325977, we need to make sure
            // we are using `withCreated` here, since if this view doesn't reach the created state,
            // the scope will not get cancelled.
            // TODO: revisit this once https://issuetracker.google.com/issues/127528777 is implemented
            // (no update as of Oct 2023)
            val appBarLayout = requireActivity().findViewById<View>(R.id.app_bar_layout) as? AppBarLayout
            withCreated {
                appBarLayout?.verticalOffsetChanges()
                    ?.onEach { verticalOffset ->
                        binding.jetpackBenefitsBanner.root.translationY =
                            (abs(verticalOffset) - appBarLayout.totalScrollRange).toFloat()
                    }
                    ?.launchIn(this)
            }
        }
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
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        lifecycle.removeObserver(storeOnboardingViewModel)
        super.onDestroy()
    }

    private fun showPluginUnavailableError() {
        binding.myStoreRefreshLayout.visibility = View.GONE
        WooAnimUtils.fadeIn(binding.pluginUnavailableErrorScrollView)
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

    override fun getFragmentSubtitle(): String = dashboardViewModel.storeName.value ?: ""

    override fun scrollToTop() {
        binding.statsScrollView.smoothScrollTo(0, 0)
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
                dashboardViewModel.onShareStoreClicked()
            }
        } else {
            binding.emptyView.hide()
            dashboardVisibility = View.VISIBLE
        }
        binding.dashboardContainer.visibility = dashboardVisibility
        binding.myStoreTopPerformers.visibility = dashboardVisibility
        isEmptyViewVisible = show
    }

    override fun shouldExpandToolbar() = binding.statsScrollView.scrollY == 0

    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_dashboard_fragment, menu)
    }

    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        return when (menuItem.itemId) {
            R.id.menu_edit_screen_widgets -> {
                dashboardViewModel.onEditWidgetsClicked()
                true
            }

            R.id.menu_share_store -> {
                dashboardViewModel.onShareStoreClicked()
                true
            }

            else -> false
        }
    }

    override fun onPrepareMenu(menu: Menu) {
        menu.findItem(R.id.menu_share_store).isVisible =
            dashboardViewModel.appbarState.value?.showShareStoreButton ?: false
    }
}
