package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
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
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.FragmentMyStoreBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.extensions.verticalOffsetChanges
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.mystore.MyStoreViewModel.*
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenTopPerformer
import com.woocommerce.android.util.*
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MyStoreFragment :
    TopLevelFragment(R.layout.fragment_my_store),
    MyStoreStatsListener {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName
        private const val STATE_KEY_TAB_POSITION = "tab-stats-position"

        fun newInstance() = MyStoreFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    private val viewModel: MyStoreViewModel by viewModels()

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var dateUtils: DateUtils

    private var _binding: FragmentMyStoreBinding? = null
    private val binding get() = _binding!!

    private var errorSnackbar: Snackbar? = null

    private var tabStatsPosition: Int = 0 // Save the current position of stats tab view
    private val activeGranularity: StatsGranularity
        get() {
            return tabLayout.getTabAt(tabStatsPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private var _tabLayout: TabLayout? = null
    private val tabLayout
        get() = _tabLayout!!

    private val appBarLayout
        get() = activity?.findViewById<View>(R.id.app_bar_layout) as? AppBarLayout

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    private val myStoreDateBar
        get() = binding.myStoreDateBar

    private var isEmptyViewVisible: Boolean = false

    private val tabSelectedListener = object : TabLayout.OnTabSelectedListener {
        override fun onTabSelected(tab: TabLayout.Tab) {
            tabStatsPosition = tab.position
            viewModel.onStatsGranularityChanged(activeGranularity)
            myStoreDateBar.clearDateRangeValues()
            binding.myStoreStats.loadDashboardStats(activeGranularity)
            binding.myStoreTopPerformers.onDateGranularityChanged(activeGranularity)
        }

        override fun onTabUnselected(tab: TabLayout.Tab) {}

        override fun onTabReselected(tab: TabLayout.Tab) {}
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        initTabLayout()

        _binding = FragmentMyStoreBinding.bind(view)

        binding.myStoreRefreshLayout.setOnRefreshListener {
            binding.myStoreRefreshLayout.isRefreshing = false
            viewModel.onSwipeToRefresh()
            binding.myStoreStats.clearLabelValues()
            binding.myStoreStats.clearChartData()
            myStoreDateBar.clearDateRangeValues()
        }

        savedInstanceState?.let { bundle ->
            tabStatsPosition = bundle.getInt(STATE_KEY_TAB_POSITION)
        }

        // Create tabs and add to appbar
        StatsGranularity.values().forEach { granularity ->
            val tab = tabLayout.newTab().apply {
                setText(binding.myStoreStats.getStringForGranularity(granularity))
                tag = granularity
            }
            tabLayout.addTab(tab)

            // Start with the given time period selected
            if (granularity == activeGranularity) {
                tab.select()
            }
        }

        myStoreDateBar.initView(dateUtils)

        binding.myStoreStats.initView(
            activeGranularity,
            listener = this,
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded,
            dateUtils = dateUtils
        )

        binding.myStoreTopPerformers.initView(
            listener = this,
            selectedSite = selectedSite
        )

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        binding.myStoreStatsAvailabilityMessage.setClickableText(
            content = getString(R.string.my_store_stats_availability_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(Origin.MY_STORE) }
        )

        prepareJetpackBenefitsBanner()

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        viewModel.revenueStatsState.observe(viewLifecycleOwner) { revenueStats ->
            when (revenueStats) {
                is RevenueStatsViewState.Content -> showStats(revenueStats.revenueStats, revenueStats.granularity)
                RevenueStatsViewState.GenericError -> showStatsError()
                RevenueStatsViewState.Loading -> showChartSkeleton(true)
                RevenueStatsViewState.PluginNotActiveError -> updateStatsAvailabilityError()
            }
        }
        viewModel.visitorStatsState.observe(viewLifecycleOwner) { visitorStats ->
            when (visitorStats) {
                is VisitorStatsViewState.Content -> showVisitorStats(visitorStats.stats)
                VisitorStatsViewState.Error -> binding.myStoreStats.showVisitorStatsError()
                VisitorStatsViewState.JetPackCPEmpty -> showEmptyVisitorStatsForJetpackCP()
            }
        }
        viewModel.topPerformersState.observe(viewLifecycleOwner) { topPerformers ->
            when (topPerformers) {
                is TopPerformersViewState.Loading -> showTopPerformersLoading()
                is TopPerformersViewState.Error -> showTopPerformersError()
                is TopPerformersViewState.Content -> showTopPerformers(
                    topPerformers.topPerformers,
                    topPerformers.granularity
                )
            }
        }
        viewModel.hasOrders.observe(viewLifecycleOwner) { newValue ->
            when (newValue) {
                OrderState.Empty -> showEmptyView(true)
                OrderState.AtLeastOne -> showEmptyView(false)
            }
        }
        viewModel.jetpackBenefitsBannerState.observe(viewLifecycleOwner) { showBanner ->
            when (showBanner) {
                JetpackBenefitsBannerState.Hide -> showJetpackBenefitsBanner(false)
                is JetpackBenefitsBannerState.Show -> {
                    binding.jetpackBenefitsBanner.dismissButton.setOnClickListener {
                        showBanner.onDismiss.invoke()
                    }
                    showJetpackBenefitsBanner(true)
                }
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

    private fun showTopPerformersLoading() {
        binding.myStoreTopPerformers.showErrorView(false)
        binding.myStoreTopPerformers.showSkeleton(true)
    }

    @Suppress("ForbiddenComment")
    private fun prepareJetpackBenefitsBanner() {
        binding.jetpackBenefitsBanner.root.setOnClickListener {
            AnalyticsTracker.track(
                stat = Stat.FEATURE_JETPACK_BENEFITS_BANNER,
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
    }

    override fun onStop() {
        errorSnackbar?.dismiss()
        super.onStop()
    }

    override fun onDestroyView() {
        binding.myStoreStats.removeListener()
        binding.myStoreTopPerformers.removeListener()
        removeTabLayoutFromAppBar()
        tabLayout.removeOnTabSelectedListener(tabSelectedListener)
        _tabLayout = null
        super.onDestroyView()
        _binding = null
    }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(STATE_KEY_TAB_POSITION, tabStatsPosition)
    }

    private fun showStats(revenueStatsModel: RevenueStatsUiModel?, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            addTabLayoutToAppBar()
            binding.myStoreStats.showErrorView(false)
            showChartSkeleton(false)
            binding.myStoreStats.updateView(revenueStatsModel)
            myStoreDateBar.updateDateRangeView(revenueStatsModel, activeGranularity)
        }
    }

    private fun showStatsError() {
        showStats(null, activeGranularity)//TODO CHECK IF CAN BE REMOVED
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

    private fun showTopPerformers(topPerformers: List<TopPerformerProductUiModel>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreTopPerformers.showSkeleton(false)
            binding.myStoreTopPerformers.showErrorView(false)
            binding.myStoreTopPerformers.updateView(topPerformers)
        }
    }

    private fun showTopPerformersError() {
        binding.myStoreTopPerformers.updateView(emptyList()) //TODO CHECK IF CAN BE REMOVED
        binding.myStoreTopPerformers.showSkeleton(false)
        binding.myStoreTopPerformers.showErrorView(true)
        showErrorSnack()
    }

    private fun showVisitorStats(visitorStats: Map<String, Int>) {
        binding.myStoreStats.showVisitorStats(visitorStats)
        if (activeGranularity == StatsGranularity.DAYS) {
            binding.emptyStatsView.updateVisitorCount(visitorStats.values.sum())
        }
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

    private fun showJetpackBenefitsBanner(show: Boolean) {
        binding.jetpackBenefitsBanner.root.isVisible = show
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

    override fun onChartValueSelected(dateString: String, period: StatsGranularity) {
        myStoreDateBar.updateDateViewOnScrubbing(dateString, period)
    }

    override fun onChartValueUnSelected(revenueStatsModel: RevenueStatsUiModel?, period: StatsGranularity) {
        myStoreDateBar.updateDateRangeView(revenueStatsModel, period)
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
                AnalyticsTracker.track(Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
                ActivityUtils.shareStoreUrl(requireActivity(), selectedSite.get().url)
            }
            binding.emptyStatsView.visibility = View.VISIBLE
        } else {
            binding.emptyView.hide()
            dashboardVisibility = View.VISIBLE
            binding.emptyStatsView.visibility = View.GONE
        }

        tabLayout.visibility = dashboardVisibility
        myStoreDateBar.visibility = dashboardVisibility
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
