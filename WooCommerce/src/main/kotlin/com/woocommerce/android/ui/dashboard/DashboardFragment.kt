package com.woocommerce.android.ui.dashboard

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.annotation.OptIn
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.view.MenuProvider
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withCreated
import androidx.navigation.fragment.findNavController
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.badge.BadgeUtils
import com.google.android.material.badge.ExperimentalBadgeUtils
import com.google.android.play.core.review.ReviewManagerFactory
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.FragmentDashboardBinding
import com.woocommerce.android.extensions.getColorCompat
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.scrollStartEvents
import com.woocommerce.android.extensions.showDateRangePicker
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.extensions.verticalOffsetChanges
import com.woocommerce.android.model.DashboardWidget
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.blaze.BlazeUrlsHelper.BlazeFlowSource
import com.woocommerce.android.ui.blaze.creation.BlazeCampaignCreationDispatcher
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ContactSupport
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.FeedbackNegativeAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.FeedbackPositiveAction
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenEditWidgets
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.OpenRangePicker
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShareStore
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowAIProductDescriptionDialog
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardEvent.ShowPrivacyBanner
import com.woocommerce.android.ui.dashboard.DashboardViewModel.DashboardWidgetUiModel
import com.woocommerce.android.ui.jitm.JitmFragment
import com.woocommerce.android.ui.jitm.JitmMessagePathsProvider
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.ui.prefs.privacy.banner.PrivacyBannerFragmentDirections
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.wordpress.android.util.ToastUtils
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

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var usageTracksEventEmitter: DashboardStatsUsageTracksEventEmitter

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Inject
    lateinit var blazeCampaignCreationDispatcher: BlazeCampaignCreationDispatcher

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val editButtonBadge by lazy {
        BadgeDrawable.create(requireContext()).apply {
            backgroundColor = requireContext().getColorCompat(R.color.color_primary)
        }
    }

    private val mainNavigationRouter
        get() = activity as? MainNavigationRouter

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Visible(
            navigationIcon = null,
            hasShadow = true,
        )

    private var wasPreviouslyStopped = false

    override fun onCreate(savedInstanceState: Bundle?) {
        lifecycle.addObserver(dashboardViewModel.performanceObserver)
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
                        dashboardViewModel = dashboardViewModel,
                        blazeCampaignCreationDispatcher = blazeCampaignCreationDispatcher
                    )
                }
            }
        }

        binding.myStoreRefreshLayout.setOnRefreshListener {
            binding.myStoreRefreshLayout.isRefreshing = false
            dashboardViewModel.onPullToRefresh()
            refreshJitm()
        }

        prepareJetpackBenefitsBanner()

        binding.statsScrollView.scrollStartEvents()
            .onEach { usageTracksEventEmitter.interacted() }
            .launchIn(viewLifecycleOwner.lifecycleScope)

        setupStateObservers()
    }

    @Suppress("ComplexMethod", "MagicNumber", "LongMethod")
    private fun setupStateObservers() {
        dashboardViewModel.appbarState.observe(viewLifecycleOwner) { requireActivity().invalidateOptionsMenu() }

        dashboardViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
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

                is OpenRangePicker -> showDateRangePicker(event.start, event.end, event.callback)

                is ContactSupport -> activity?.startHelpActivity(HelpOrigin.MY_STORE)

                is FeedbackPositiveAction -> handleFeedbackRequestPositiveClick()

                is FeedbackNegativeAction -> mainNavigationRouter?.showFeedbackSurvey()

                is ShowSnackbar -> ToastUtils.showToast(requireContext(), event.message)

                else -> event.isHandled = false
            }
        }
        dashboardViewModel.storeName.observe(viewLifecycleOwner) { storeName ->
            ((activity) as MainActivity).setSubtitle(storeName)
        }
        dashboardViewModel.jetpackBenefitsBannerState.observe(viewLifecycleOwner) { jetpackBenefitsBanner ->
            onVisitorStatsUnavailable(jetpackBenefitsBanner)
        }
        dashboardViewModel.dashboardWidgets.observe(viewLifecycleOwner) { widgets ->
            // Show banners only if onboarding list is NOT displayed
            if (
                widgets.none {
                    (it as? DashboardWidgetUiModel.ConfigurableWidget)?.widget?.type == DashboardWidget.Type.ONBOARDING
                }
            ) {
                initJitm()
            }
        }
        dashboardViewModel.hasNewWidgets.observe(viewLifecycleOwner) { hasNewWidgets ->
            editButtonBadge.isVisible = hasNewWidgets
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
            // TODO revisit this once https://issuetracker.google.com/issues/127528777 is implemented
            // (no update as of Oct 2023)
            val appBarLayout = requireActivity().findViewById<View>(R.id.app_bar_layout) as? AppBarLayout
            viewLifecycleOwner.withCreated {
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
        AnalyticsTracker.trackViewShown(this)
        // Avoid executing interacted() on first load. Only when the user navigated away from the fragment.
        if (wasPreviouslyStopped) {
            usageTracksEventEmitter.interacted()
            wasPreviouslyStopped = false
        }
    }

    override fun onStop() {
        wasPreviouslyStopped = true
        super.onStop()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun initJitm() {
        childFragmentManager.beginTransaction()
            .replace(
                R.id.jitmFragment,
                JitmFragment.newInstance(JitmMessagePathsProvider.MY_STORE),
                JITM_FRAGMENT_TAG
            )
            .commit()
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

    override fun shouldExpandToolbar() = binding.statsScrollView.scrollY == 0

    @OptIn(ExperimentalBadgeUtils::class)
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_dashboard_fragment, menu)

        // Attach the badge to the top-left corner of the edit widgets button
        editButtonBadge.badgeGravity = if (resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
            BadgeDrawable.TOP_END
        } else {
            BadgeDrawable.TOP_START
        }
        BadgeUtils.attachBadgeDrawable(
            editButtonBadge,
            requireActivity().findViewById(R.id.toolbar),
            R.id.menu_edit_screen_widgets
        )
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
