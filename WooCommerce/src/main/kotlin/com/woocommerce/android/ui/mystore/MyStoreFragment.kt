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
import com.woocommerce.android.ui.mystore.MyStoreViewModel.MyStoreEvent.OpenTopPerformer
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformerProductUiModel
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformersViewState.Content
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformersViewState.Error
import com.woocommerce.android.ui.mystore.MyStoreViewModel.TopPerformersViewState.Loading
import com.woocommerce.android.util.*
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.NetworkUtils
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.abs

@AndroidEntryPoint
class MyStoreFragment :
    TopLevelFragment(R.layout.fragment_my_store),
    MyStoreContract.View,
    MyStoreStatsListener {
    companion object {
        val TAG: String = MyStoreFragment::class.java.simpleName
        private const val STATE_KEY_TAB_POSITION = "tab-stats-position"
        private const val STATE_KEY_REFRESH_PENDING = "is-refresh-pending"
        private const val STATE_KEY_IS_EMPTY_VIEW_SHOWING = "is-empty-view-showing"

        fun newInstance() = MyStoreFragment()

        val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    private val viewModel: MyStoreViewModel by viewModels()

    @Inject lateinit var presenter: MyStoreContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var dateUtils: DateUtils

    private var _binding: FragmentMyStoreBinding? = null
    private val binding get() = _binding!!

    override var isRefreshPending: Boolean = false // If true, the fragment will refresh its data when it's visible
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
            myStoreDateBar.clearDateRangeValues()
            binding.myStoreStats.loadDashboardStats(activeGranularity)
            binding.myStoreTopPerformers.loadTopPerformerStats(activeGranularity)
            viewModel.onStatsGranularityChanged(activeGranularity)
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
            MyStorePresenter.resetForceRefresh()
            binding.myStoreRefreshLayout.isRefreshing = false
            refreshMyStoreStats(forced = true)
            viewModel.onSwipeToRefresh()
        }

        savedInstanceState?.let { bundle ->
            isRefreshPending = bundle.getBoolean(STATE_KEY_REFRESH_PENDING, false)
            tabStatsPosition = bundle.getInt(STATE_KEY_TAB_POSITION)
            if (bundle.getBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING)) {
                showEmptyView(true)
            }
        }

        presenter.takeView(this)

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
            selectedSite = selectedSite,
            formatCurrencyForDisplay = currencyFormatter::formatCurrencyRounded,
            statsCurrencyCode = presenter.getStatsCurrency().orEmpty()
        )

        val contactUsText = getString(R.string.my_store_stats_availability_contact_us)
        binding.myStoreStatsAvailabilityMessage.setClickableText(
            content = getString(R.string.my_store_stats_availability_description, contactUsText),
            clickableContent = contactUsText,
            clickAction = WooClickableSpan { activity?.startHelpActivity(Origin.MY_STORE) }
        )

        prepareJetpackBenefitsBanner()

        tabLayout.addOnTabSelectedListener(tabSelectedListener)

        refreshMyStoreStats(forced = this.isRefreshPending)























        viewModel.topPerformersState.observe(viewLifecycleOwner) { _, newValue ->
            when (newValue) {
                is Loading -> showTopPerformersLoading()
                is Error -> showTopPerformersError(activeGranularity) //TODO check why granularity is needed here
                is Content -> showTopPerformers(newValue.topPerformers, activeGranularity)
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is OpenTopPerformer -> mainNavigationRouter?.showProductDetail(event.productId)
                else -> event.isHandled = false
            }
        }
    }

    private fun showTopPerformersLoading() {
        binding.myStoreTopPerformers.showErrorView(false)
        binding.myStoreTopPerformers.showSkeleton(true)
    }

    private fun prepareJetpackBenefitsBanner() {
        binding.jetpackBenefitsBanner.dismissButton.setOnClickListener {
            presenter.dismissJetpackBenefitsBanner()
        }
        binding.jetpackBenefitsBanner.root.setOnClickListener {
            findNavController().navigateSafely(MyStoreFragmentDirections.actionMyStoreToJetpackBenefitsDialog())
        }
        val appBarLayout = appBarLayout ?: return
        // For the banner to be above the bottom navigation view when the toolbar is expanded
        appBarLayout.verticalOffsetChanges()
            .onEach { verticalOffset ->
                binding.jetpackBenefitsBanner.root.translationY =
                    (abs(verticalOffset) - appBarLayout.totalScrollRange).toFloat()
            }
            .launchIn(viewLifecycleOwner.lifecycleScope)
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
        presenter.dropView()
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
        outState.putBoolean(STATE_KEY_REFRESH_PENDING, isRefreshPending)
        outState.putInt(STATE_KEY_TAB_POSITION, tabStatsPosition)
        outState.putBoolean(STATE_KEY_IS_EMPTY_VIEW_SHOWING, isEmptyViewVisible)
    }

    override fun showStats(
        revenueStatsModel: WCRevenueStatsModel?,
        granularity: StatsGranularity
    ) {
        addTabLayoutToAppBar()
        // Only update the order stats view if the new stats match the currently selected timeframe
        if (activeGranularity == granularity) {
            binding.myStoreStats.showErrorView(false)
            binding.myStoreStats.updateView(revenueStatsModel, presenter.getStatsCurrency())
            myStoreDateBar.updateDateRangeView(revenueStatsModel, granularity)
        }
    }

    override fun showStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            showStats(null, granularity)
            binding.myStoreStats.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun updateStatsAvailabilityError() {
        binding.myStoreRefreshLayout.visibility = View.GONE
        WooAnimUtils.fadeIn(binding.statsErrorScrollView)
        removeTabLayoutFromAppBar()
    }

    override fun showTopPerformers(
        topPerformers: List<TopPerformerProductUiModel>,
        granularity: StatsGranularity
    ) {
        if (activeGranularity == granularity) {
            binding.myStoreTopPerformers.showSkeleton(false)
            binding.myStoreTopPerformers.showErrorView(false)
            binding.myStoreTopPerformers.updateView(topPerformers)
        }
    }

    override fun showTopPerformersError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreTopPerformers.updateView(emptyList())
            binding.myStoreTopPerformers.showSkeleton(false)
            binding.myStoreTopPerformers.showErrorView(true)
            showErrorSnack()
        }
    }

    override fun showVisitorStats(visitorStats: Map<String, Int>, granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreStats.showVisitorStats(visitorStats)
            if (granularity == StatsGranularity.DAYS) {
                binding.emptyStatsView.updateVisitorCount(visitorStats.values.sum())
            }
        }
    }

    override fun showVisitorStatsError(granularity: StatsGranularity) {
        if (activeGranularity == granularity) {
            binding.myStoreStats.showVisitorStatsError()
        }
    }

    override fun showEmptyVisitorStatsForJetpackCP() {
        binding.myStoreStats.showEmptyVisitorStatsForJetpackCP()
    }

    override fun showErrorSnack() {
        if (errorSnackbar?.isShownOrQueued == false || NetworkUtils.isNetworkAvailable(context)) {
            errorSnackbar = uiMessageResolver.getSnack(R.string.dashboard_stats_error)
            errorSnackbar?.show()
        }
    }

    override fun showJetpackBenefitsBanner(show: Boolean) {
        binding.jetpackBenefitsBanner.root.isVisible = show
    }

    override fun getFragmentTitle() = getString(R.string.my_store)

    override fun getFragmentSubtitle(): String = presenter.getSelectedSiteName() ?: ""

    override fun scrollToTop() {
        binding.statsScrollView.smoothScrollTo(0, 0)
    }

    override fun refreshMyStoreStats(forced: Boolean) {
        // If this fragment is currently active, force a refresh of data. If not, set
        // a flag to force a refresh when it becomes active
        if (forced) {
            binding.myStoreStats.clearLabelValues()
            binding.myStoreStats.clearChartData()
            myStoreDateBar.clearDateRangeValues()
        }
        presenter.run {
            loadStats(activeGranularity, forced)
        }
    }

    override fun showChartSkeleton(show: Boolean) {
        binding.myStoreStats.showSkeleton(show)
    }

    override fun onRequestLoadStats(period: StatsGranularity) {
        binding.myStoreStats.showErrorView(false)
        presenter.loadStats(period)
    }

    override fun onChartValueSelected(dateString: String, period: StatsGranularity) {
        myStoreDateBar.updateDateViewOnScrubbing(dateString, period)
    }

    override fun onChartValueUnSelected(revenueStatsModel: WCRevenueStatsModel?, period: StatsGranularity) {
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

    override fun showEmptyView(show: Boolean) {
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
