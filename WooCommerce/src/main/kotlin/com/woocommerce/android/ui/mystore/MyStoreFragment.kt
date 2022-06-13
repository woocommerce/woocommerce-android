package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewGroup.LayoutParams
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.FeedbackPrefs
import com.woocommerce.android.FeedbackPrefs.userFeedbackIsDue
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.R.attr
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentMyStoreBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.extensions.verticalOffsetChanges
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenTopPerformer
import com.woocommerce.android.ui.mystore.MyStoreViewModel.OrderState
import com.woocommerce.android.ui.mystore.MyStoreViewModel.RevenueStatsViewState
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformersViewState
import com.woocommerce.android.ui.mystore.MyStoreViewModel.VisitorStatsViewState
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@ExperimentalCoroutinesApi
@AndroidEntryPoint
@Suppress("ForbiddenComment")
class MyStoreFragment : TopLevelFragment(R.layout.fragment_my_store) {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName

        fun newInstance() = MyStoreFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    private val viewModel: MyStoreViewModel by viewModels()

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var dateUtils: DateUtils
    @Inject lateinit var usageTracksEventEmitter: MyStoreStatsUsageTracksEventEmitter

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
            viewModel.onStatsGranularityChanged(tab.tag as StatsGranularity)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}

        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    private val handler = Handler(Looper.getMainLooper())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        initTabLayout()

        _binding = FragmentMyStoreBinding.bind(view)

        binding.myStoreRefreshLayout.setOnRefreshListener {
            binding.myStoreRefreshLayout.isRefreshing = false
            viewModel.onSwipeToRefresh()
            binding.myStoreStats.clearStatsHeaderValues()
            binding.myStoreStats.clearChartData()
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
            viewModel.activeStatsGranularity.value ?: DEFAULT_STATS_GRANULARITY,
            selectedSite,
            dateUtils,
            currencyFormatter,
            usageTracksEventEmitter,
            viewLifecycleOwner.lifecycleScope
        )

        binding.myStoreTopPerformers.initView(selectedSite)

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        binding.myStoreStatsAvailabilityMessage.setClickableText(
            content = getString(R.string.my_store_stats_availability_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(Origin.MY_STORE) }
        )

        prepareJetpackBenefitsBanner()

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        binding.statsScrollView.scrollStartEvents()
            .onEach { usageTracksEventEmitter.interacted() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        setupStateObservers()
    }

    @Suppress("ComplexMethod", "MagicNumber")
    private fun setupStateObservers() {
        viewModel.activeStatsGranularity.observe(viewLifecycleOwner) { activeGranularity ->
            if (tabLayout.getTabAt(tabLayout.selectedTabPosition)?.tag != activeGranularity) {
                val index = StatsGranularity.values().indexOf(activeGranularity)
                // Small delay needed to ensure tablayout scrolls to the selected tab if tab is not visible on screen.
                handler.postDelayed({ tabLayout.getTabAt(index)?.select() }, 300)
            }
            binding.myStoreStats.loadDashboardStats(activeGranularity)
            binding.myStoreTopPerformers.onDateGranularityChanged(activeGranularity)
        }
        viewModel.revenueStatsState.observe(viewLifecycleOwner) { revenueStats ->
            when (revenueStats) {
                is RevenueStatsViewState.Content -> showStats(revenueStats.revenueStats)
                RevenueStatsViewState.GenericError -> showStatsError()
                RevenueStatsViewState.Loading -> showChartSkeleton(true)
                RevenueStatsViewState.PluginNotActiveError -> updateStatsAvailabilityError()
            }
        }
        viewModel.visitorStatsState.observe(viewLifecycleOwner) { stats ->
            when (stats) {
                is VisitorStatsViewState.Content -> showVisitorStats(stats.stats)
                VisitorStatsViewState.Error -> {
                    binding.jetpackBenefitsBanner.root.isVisible = false
                    binding.myStoreStats.showVisitorStatsError()
                }
                is VisitorStatsViewState.JetpackCpConnected -> onJetpackCpConnected(stats.benefitsBanner)
            }
        }
        viewModel.topPerformersState.observe(viewLifecycleOwner) { topPerformers ->
            when (topPerformers) {
                is TopPerformersViewState.Loading -> showTopPerformersLoading()
                is TopPerformersViewState.Error -> showTopPerformersError()
                is TopPerformersViewState.Content -> showTopPerformers(topPerformers.topPerformers)
            }
        }
        viewModel.hasOrders.observe(viewLifecycleOwner) { newValue ->
            when (newValue) {
                OrderState.Empty -> showEmptyView(true)
                OrderState.AtLeastOne -> showEmptyView(false)
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenTopPerformer -> findNavController().navigateSafely(
                    NavGraphMainDirections.actionGlobalProductDetailFragment(
                        remoteProductId = event.productId,
                        isTrashEnabled = false
                    )
                )
                else -> event.isHandled = false
            }
        }
    }

    private fun onJetpackCpConnected(benefitsBanner: BenefitsBannerUiModel) {
        showEmptyVisitorStatsForJetpackCP()
        if (benefitsBanner.show) {
            binding.jetpackBenefitsBanner.dismissButton.setOnClickListener {
                benefitsBanner.onDismiss()
            }
        }
        if (benefitsBanner.show && !binding.jetpackBenefitsBanner.root.isVisible) {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "shown")
            )
        }
        binding.jetpackBenefitsBanner.root.isVisible = benefitsBanner.show
    }

    private fun showTopPerformersLoading() {
        binding.myStoreTopPerformers.showErrorView(false)
        binding.myStoreTopPerformers.showSkeleton(true)
    }

    @Suppress("ForbiddenComment")
    private fun prepareJetpackBenefitsBanner() {
        binding.jetpackBenefitsBanner.root.isVisible = false
        binding.jetpackBenefitsBanner.root.setOnClickListener {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.FEATURE_JETPACK_BENEFITS_BANNER,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_BENEFITS_BANNER_ACTION to "tapped")
            )
            findNavController().navigateSafely(MyStoreFragmentDirections.actionMyStoreToJetpackBenefitsDialog())
        }
        val appBarLayout = appBarLayout ?: return
        // For the banner to be above the bottom navigation view when the toolbar is expanded
        viewLifecycleOwner.lifecycleScope.launchWhenCreated {
            // Due to this issue https://issuetracker.google.com/issues/181325977, we need to make sure
            // we are using `launchWhenCreated` here, since if this view doesn't reach the created state,
            // the scope will not get cancelled.
            // TODO: revisit this once https://issuetracker.google.com/issues/127528777 is implemented
            appBarLayout.verticalOffsetChanges()
                .collect { verticalOffset ->
                    binding.jetpackBenefitsBanner.root.translationY =
                        (abs(verticalOffset) - appBarLayout.totalScrollRange).toFloat()
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

    /*
    Hide Settings icon on My Store, because it is moved to the "More" screen.
    We temporarily comment out the code instead of deleting, because we might want to restore it later,
    based on merchants feedbacks.

    TODO: Maybe restore Settings icon on My Store, depending on merchants feedbacks.
    For more context: https://github.com/woocommerce/woocommerce-android/issues/5586

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_action_bar, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_settings -> {
                (activity as? MainNavigationRouter)?.showSettingsScreen()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
     */

    private fun showStats(revenueStatsModel: RevenueStatsUiModel?) {
        addTabLayoutToAppBar()
        binding.myStoreStats.showErrorView(false)
        showChartSkeleton(false)
        binding.myStoreStats.updateView(revenueStatsModel)
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

    private fun showEmptyVisitorStatsForJetpackCP() {
        binding.myStoreStats.showEmptyVisitorStatsForJetpackCP()
    }

    private fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == false || NetworkUtils.isNetworkAvailable(context)) {
            errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
            errorSnackbar?.show()
        }
    }

    override fun getFragmentTitle() = getString(R.string.my_store)

    override fun getFragmentSubtitle(): String = viewModel.getSelectedSiteName()

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
        if (userFeedbackIsDue && visibility == View.GONE) {
            setupFeedbackRequestCard()
        } else if (userFeedbackIsDue.not() && visibility == View.VISIBLE) {
            visibility = View.GONE
        }
    }

    private fun setupFeedbackRequestCard() {
        binding.storeFeedbackRequestCard.visibility = View.VISIBLE
        val negativeCallback = {
            mainNavigationRouter?.showFeedbackSurvey()
            binding.storeFeedbackRequestCard.visibility = View.GONE
            FeedbackPrefs.lastFeedbackDate = Calendar.getInstance().time
        }
        binding.storeFeedbackRequestCard.initView(negativeCallback, ::handleFeedbackRequestPositiveClick)
    }

    private fun handleFeedbackRequestPositiveClick() {
        // set last feedback date to now and hide the card
        FeedbackPrefs.lastFeedbackDate = Calendar.getInstance().time
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
                AnalyticsTracker.track(AnalyticsEvent.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(requireActivity(), selectedSite.get().url)
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
}
