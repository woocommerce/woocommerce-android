@file:Suppress("DEPRECATION")

package com.woocommerce.android.ui.main

import NotificationsPermissionCard
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Parcelable
import android.text.method.LinkMovementMethod
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.navOptions
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.android.material.appbar.AppBarLayout
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.R.dimen
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HORIZONTAL_SIZE_CLASS
import com.woocommerce.android.analytics.deviceTypeToAnalyticsString
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.extensions.EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.show
import com.woocommerce.android.extensions.startHelpActivity
import com.woocommerce.android.model.Notification
import com.woocommerce.android.support.help.HelpOrigin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.InfoScreenFragment
import com.woocommerce.android.ui.compose.theme.WooTheme
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.dashboard.StoreConnectionErrorDialog
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.MORE
import com.woocommerce.android.ui.main.BottomNavigationPosition.MY_STORE
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.ui.main.BottomNavigationPosition.POS
import com.woocommerce.android.ui.main.BottomNavigationPosition.PRODUCTS
import com.woocommerce.android.ui.main.MainActivityViewModel.BottomBarState
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.Hidden
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.NewFeature
import com.woocommerce.android.ui.main.MainActivityViewModel.MoreMenuBadgeState.UnseenReviews
import com.woocommerce.android.ui.main.MainActivityViewModel.RequestNotificationsPermission
import com.woocommerce.android.ui.main.MainActivityViewModel.RestartActivityEvent
import com.woocommerce.android.ui.main.MainActivityViewModel.RestartActivityForAppLink
import com.woocommerce.android.ui.main.MainActivityViewModel.RestartActivityForLocalNotification
import com.woocommerce.android.ui.main.MainActivityViewModel.RestartActivityForPushNotification
import com.woocommerce.android.ui.main.MainActivityViewModel.ShortcutOpenOrderCreation
import com.woocommerce.android.ui.main.MainActivityViewModel.ShortcutOpenPayments
import com.woocommerce.android.ui.main.MainActivityViewModel.ShowFeatureAnnouncement
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewBlazeCampaignDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewBlazeCampaignList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewMyStoreStats
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewPayments
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewProductDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewTapToPay
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewUrlInWebView
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewWooPosPromo
import com.woocommerce.android.ui.moremenu.MoreMenuFragmentDirections
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.list.OrderListFragmentDirections
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.plans.di.TrialStatusBarFormatterFactory
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState.TrialStatusBarState
import com.woocommerce.android.ui.pospromo.PosPromoDialogFragment
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.list.ProductListFragmentDirections
import com.woocommerce.android.ui.reviews.ReviewListFragmentDirections
import com.woocommerce.android.ui.sitepicker.SitePickerFragmentArgs
import com.woocommerce.android.ui.woopos.tab.WooPosTabController
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.util.WooAnimUtils.animateBottomBar
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.DisabledAppBarLayoutBehavior
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs

// TODO Extract logic out of MainActivity to reduce size
@Suppress("LargeClass")
@AndroidEntryPoint
class MainActivity :
    AppUpgradeActivity(),
    MainContract.View,
    MainNavigationRouter,
    MainBottomNavigationView.MainNavigationListener {
    companion object {
        private const val MAGIC_LOGIN = "magic-login"

        private const val KEY_BOTTOM_NAV_POSITION = "key-bottom-nav-position"
        private const val KEY_UNFILLED_ORDER_COUNT = "unfilled-order-count"

        private const val DIALOG_NAVIGATOR_NAME = "dialog"

        // push notification-related constants
        const val FIELD_OPENED_FROM_PUSH = "opened-from-push-notification"
        const val FIELD_REMOTE_NOTIFICATION = "remote-notification"
        const val FIELD_LOCAL_NOTIFICATION = "local-notification"
        const val FIELD_PUSH_ID = "local-push-id"

        // widget-related constants
        const val FIELD_OPENED_FROM_WIDGET = "opened-from-push-widget"
        const val FIELD_WIDGET_NAME = "widget-name"

        const val NOTIFICATIONS_PERMISSION_BAR_DISPLAY_DELAY = 2000L

        interface BackPressListener {
            fun onRequestAllowBackPress(): Boolean
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject
    lateinit var presenter: MainContract.Presenter

    @Inject
    lateinit var loginAnalyticsListener: LoginAnalyticsListener

    @Inject
    lateinit var selectedSite: SelectedSite

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var crashLogging: CrashLogging

    @Inject
    lateinit var appWidgetUpdaters: WidgetUpdater.StatsWidgetUpdaters

    @Inject
    lateinit var trialStatusBarFormatterFactory: TrialStatusBarFormatterFactory

    @Inject
    lateinit var animatorHelper: MainAnimatorHelper

    @Inject
    lateinit var edgeToEdgeHelper: MainActivityEdgeToEdgeHelper

    @Inject
    lateinit var posTabController: WooPosTabController

    @Inject
    lateinit var backPressTracker: BackPressTracker

    private val viewModel: MainActivityViewModel by viewModels()

    private val appBackgroundObserver = object : DefaultLifecycleObserver {
        override fun onStop(owner: LifecycleOwner) {
            viewModel.onAppBackgrounded()
        }
    }

    private var unfilledOrderCount: Int = 0
    private var menu: Menu? = null

    private val toolbarEnabledBehavior = AppBarLayout.Behavior()
    private val toolbarDisabledBehavior = DisabledAppBarLayoutBehavior()

    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar

    // Drives the collapsing toolbar's elevation shadow from its own offset (see setupAppBarElevation).
    private var appBarVerticalOffset = 0
    private var appBarHasShadow = true

    private val appBarOffsetListener by lazy {
        AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
            applySubtitleFade(verticalOffset)
        }
    }

    // Fades the toolbar subtitle out as the toolbar collapses. Guards against a zero scroll range (which would
    // make the alpha NaN, e.g. while the toolbar is being re-shown after a Hidden screen) so the subtitle can't
    // get stuck invisible.
    private fun applySubtitleFade(verticalOffset: Int) {
        val totalScrollRange = binding.appBarLayout.totalScrollRange
        binding.toolbarSubtitle.alpha = if (totalScrollRange > 0) {
            1f - abs(verticalOffset / totalScrollRange.toFloat())
        } else {
            1f
        }
    }

    private val handler = Handler(Looper.getMainLooper())
    private val notificationPermissionBarRunnable = Runnable {
        animateBottomBar(binding.notificationsPermissionBar, show = true)
    }

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    @Suppress("DEPRECATION")
    private var progressDialog: ProgressDialog? = null

    private val fragmentLifecycleObserver: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        private var lastBottomNavFragment = WeakReference<Fragment>(null)
        private var lastToolbarFragment = WeakReference<Fragment>(null)

        // The bottom navigation is updated as soon as the destination's view is created/started so it hides/shows
        // promptly during the navigation transition instead of looking delayed.
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            updateBottomNav(f)
        }

        override fun onFragmentStarted(fm: FragmentManager, f: Fragment) {
            // Handles quickly navigating A -> B -> A: A's view isn't recreated so onFragmentViewCreated isn't
            // called, but onFragmentStarted is, and lastBottomNavFragment still points at B.
            if (lastBottomNavFragment.get() != f) {
                updateBottomNav(f)
            }
        }

        override fun onFragmentResumed(fm: FragmentManager, f: Fragment) {
            // The shared collapsing toolbar is updated only when the destination reaches RESUMED, i.e. once the
            // navigation is committed. During a predictive-back gesture the destination's view is created and
            // started (and it even becomes the primary navigation fragment) to render the peek, but it only
            // reaches RESUMED on commit — so updating here prevents the toolbar from expanding/collapsing
            // mid-gesture, e.g. when dragging back from Order Details or the Analytics Hub.
            if (lastToolbarFragment.get() != f) {
                updateToolbar(f)
            }
        }

        private fun updateBottomNav(f: Fragment) {
            if (f is DialogFragment) return
            lastBottomNavFragment = WeakReference(f)
            if ((f as? TopLevelFragment)?.shouldShowBottomNavigation == true) {
                showBottomNav()
            } else {
                hideBottomNav()
            }
        }

        private fun updateToolbar(f: Fragment) {
            if (f is DialogFragment) return
            lastToolbarFragment = WeakReference(f)
            val shouldShowBottomNavigation = (f as? TopLevelFragment)?.shouldShowBottomNavigation ?: false

            when (val appBarStatus = (f as? BaseFragment)?.activityAppBarStatus ?: AppBarStatus.Visible()) {
                is AppBarStatus.Visible -> {
                    showToolbar()
                    // re-expand the AppBar when returning to top level fragment,
                    // collapse it when entering a child fragment
                    if (f is TopLevelFragment && shouldShowBottomNavigation) {
                        // Post this to the view handler to make sure shouldExpandToolbar returns the correct value
                        f.view?.post {
                            if (f.view != null) {
                                expandToolbar(expand = f.shouldExpandToolbar(), animate = false)
                            }
                        }
                        enableToolbarExpansion(true)
                    } else {
                        expandToolbar(expand = false, animate = false)
                        enableToolbarExpansion(false)
                    }

                    toolbar.navigationIcon = appBarStatus.navigationIcon?.let {
                        ContextCompat.getDrawable(this@MainActivity, it)
                    }
                    appBarHasShadow = appBarStatus.hasShadow
                    updateAppBarElevation()
                    binding.appBarDivider.isVisible = appBarStatus.hasDivider
                }

                AppBarStatus.Hidden -> {
                    hideToolbar()
                    appBarHasShadow = false
                    updateAppBarElevation()
                }
            }
        }
    }

    private val launcher = this.registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.onNotificationOSAlertAllowed()
            viewModel.checkForNotificationsPermission(hasNotificationsPermission = true)
        } else {
            viewModel.onNotificationOSAlertDenied()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        // Drop stale main-flow state when no site is selected so login can take over cleanly.
        val bundle = if (SelectedSite.hasSelectedSiteId(this)) savedInstanceState else null
        super.onCreate(bundle)
        ChromeCustomTabUtils.registerForPartialTabUsage(this)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStoreConnectionErrorDialog()

        edgeToEdgeHelper.applyEdgeToEdgeSettings(binding)

        toolbar = binding.toolbar.toolbar

        setSupportActionBar(toolbar)
        toolbar.navigationIcon = null

        setupAppBarElevation()

        animatorHelper.toolbarHeight = binding.collapsingToolbar.layoutParams.height

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater

        val navGraph = graphInflater.inflate(R.navigation.nav_graph_main)
        navGraph.setStartDestination(viewModel.startDestination)

        navController = navHostFragment.navController
        // When recovering from a selected-site error, open the picker as a store switcher (not from
        // login) so it doesn't auto-select the failing store
        val startDestinationArgs = if (viewModel.isRecoveringSelectedSite) {
            SitePickerFragmentArgs(openedFromLogin = false).toBundle()
        } else {
            null
        }
        navController.setGraph(navGraph, startDestinationArgs)
        backPressTracker.register(this, navHostFragment.childFragmentManager)
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleObserver, false)
        binding.bottomNav.init(navController, this)

        posTabController.initialize(this, binding, navController)

        presenter.takeView(this)

        // fetch the site list if the database has been downgraded - otherwise the site picker will be displayed,
        // which we don't want in this situation
        if (AppPrefs.getDatabaseDowngraded()) {
            presenter.fetchSitesAfterDowngrade()
            AppPrefs.setDatabaseDowngraded(false)
            return
        }

        if (selectedSite.exists() && !presenter.isUserEligible()) {
            showUserEligibilityErrorScreen()
            return
        }

        initFragment(savedInstanceState)

        // show the app rating dialog if it's time
        AppRatingDialog.showIfNeeded(this)

        // check for any new app updates only after the user has logged into the app (release builds only)
        if (!BuildConfig.DEBUG) {
            checkForAppUpdates()
        }

        if (savedInstanceState == null) {
            viewModel.handleIncomingAppLink(intent?.data)
            viewModel.handleShortcutAction(intent?.action?.lowercase(Locale.ROOT))
            handleIncomingImages()
        }

        viewModel.showFeatureAnnouncementIfNeeded()
    }

    private fun handleIncomingImages() {
        viewModel.handleIncomingImages(
            intent?.clipData?.let {
                (0 until it.itemCount).map { index -> it.getItemAt(index).uri.toString() }
            }
        )
    }

    override fun hideProgressDialog() {
        progressDialog?.apply {
            if (isShowing) {
                cancel()
            }
        }
    }

    @Suppress("DEPRECATION")
    override fun showProgressDialog(@StringRes stringId: Int) {
        hideProgressDialog()
        progressDialog = ProgressDialog.show(this, "", getString(stringId), true)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        // Track if App was opened from a widget
        trackIfOpenedFromWidget()

        if (selectedSite.exists()) {
            if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
                viewModel.checkForNotificationsPermission(WooPermissionUtils.hasNotificationsPermission(this))
            }
        }

        checkConnection()
    }

    override fun onPause() {
        binding.appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        setIntent(intent)
        initFragment(null)

        viewModel.handleIncomingAppLink(intent.data)
        handleIncomingImages()
    }

    public override fun onDestroy() {
        ProcessLifecycleOwner.get().lifecycle.removeObserver(appBackgroundObserver)
        presenter.dropView()
        handler.removeCallbacks(notificationPermissionBarRunnable)
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(KEY_BOTTOM_NAV_POSITION, binding.bottomNav.currentPosition.id)
        outState.putInt(KEY_UNFILLED_ORDER_COUNT, unfilledOrderCount)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.also {
            val id = it.getInt(KEY_BOTTOM_NAV_POSITION, MY_STORE.id)
            binding.bottomNav.restoreSelectedItemState(id)

            val count = it.getInt(KEY_UNFILLED_ORDER_COUNT)
            if (count > 0) {
                showOrderBadge(count)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Returns true if the navigation controller is showing the root fragment (ie: a top level fragment is showing)
     */
    override fun isAtNavigationRoot(): Boolean {
        return if (::navController.isInitialized) {
            val currentDestinationId = navController.currentDestination?.id
            currentDestinationId == R.id.dashboard ||
                currentDestinationId == R.id.orders ||
                currentDestinationId == R.id.products ||
                currentDestinationId == R.id.moreMenu ||
                currentDestinationId == R.id.analytics
        } else {
            true
        }
    }

    /**
     * Return true if one of the nav component fragments is showing (the opposite of the above)
     */
    override fun isChildFragmentShowing(): Boolean {
        return navController.currentDestination?.let {
            !isAtTopLevelNavigation(isAtRoot = isAtNavigationRoot(), destination = it)
        } ?: run {
            !isAtNavigationRoot()
        }
    }

    /**
     * Returns the current top level fragment (ie: the one showing in the bottom nav)
     */
    private fun getActiveTopLevelFragment(): TopLevelFragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.childFragmentManager.primaryNavigationFragment as? TopLevelFragment
    }

    /**
     * Get the actual primary navigation Fragment from the support manager
     */
    private fun getHostChildFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.primaryNavigationFragment
        if (navHostFragment?.childFragmentManager?.fragments?.isNotEmpty() == true) {
            return navHostFragment.childFragmentManager.fragments[0]
        }
        return null
    }

    private fun showToolbar() {
        binding.collapsingToolbar.show()
    }

    private fun hideToolbar() {
        binding.collapsingToolbar.hide()
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        binding.collapsingToolbar.title = title
    }

    fun expandToolbar(expand: Boolean, animate: Boolean) {
        binding.appBarLayout.setExpanded(expand, animate)
    }

    // The collapsing toolbar draws its elevation shadow only in the "lifted" state, which AppBarLayout derives
    // from the scrolling child's canScrollVertically(). The dashboard's ComposeView doesn't report its internal
    // scroll, so the shadow flickered off on layout changes. Instead we disable the automatic elevation animation
    // and drive the shadow directly from the app bar's own vertical offset: a shadow is shown whenever the toolbar
    // is collapsed (offset != 0) on any screen that opts into a shadow.
    private fun setupAppBarElevation() {
        binding.appBarLayout.isLiftOnScroll = false
        binding.appBarLayout.stateListAnimator = null
        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { _, verticalOffset ->
                appBarVerticalOffset = verticalOffset
                updateAppBarElevation()
            }
        )
    }

    private fun updateAppBarElevation() {
        binding.appBarLayout.elevation = if (appBarHasShadow && appBarVerticalOffset != 0) {
            resources.getDimensionPixelSize(dimen.appbar_elevation).toFloat()
        } else {
            0f
        }
    }

    fun setSubtitle(subtitle: CharSequence) {
        if (subtitle.isBlank()) {
            removeSubtitle()
        } else {
            setFadingSubtitleOnCollapsingToolbar(subtitle)
        }
    }

    private fun removeSubtitle() {
        binding.appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
        if (binding.toolbarSubtitle.isGone) return
        binding.toolbarSubtitle.clearAnimation()
        if (binding.collapsingToolbar.isVisible) {
            binding.toolbarSubtitle.collapse(duration = 200L)
            animatorHelper.animateCollapsingToolbarMarginBottom(show = false) {
                binding.collapsingToolbar.expandedTitleMarginBottom = it
            }
        } else {
            binding.toolbarSubtitle.hide()
        }
    }

    private fun setFadingSubtitleOnCollapsingToolbar(subtitle: CharSequence) {
        // Cancel any in-flight collapse animation started by removeSubtitle. When navigating to a screen that
        // hides the shared toolbar, that collapse stalls (the view is never drawn to completion) and only
        // finishes once the toolbar is shown again on return — hiding the subtitle right after we set it here.
        // Clearing it keeps the store name visible.
        binding.toolbarSubtitle.clearAnimation()
        // removeSubtitle collapses the subtitle by shrinking its height and never restores it; the reveal below
        // animates scaleY/visibility instead, so a left-over collapsed height would keep the view invisible even
        // once it is shown again. Restore the natural height before re-showing.
        binding.toolbarSubtitle.updateLayoutParams { height = ViewGroup.LayoutParams.WRAP_CONTENT }
        binding.appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
        // The offset listener only fires on an offset *change*, so refresh the fade for the current state to
        // avoid a stale alpha (applySubtitleFade also guards a zero scroll range that would produce NaN).
        applySubtitleFade(appBarVerticalOffset)
        // Check to ensure expand anim is not triggered twice for same subtitle value
        if (binding.toolbarSubtitle.text == subtitle && binding.toolbarSubtitle.isVisible) {
            // The subtitle is already shown (e.g. a stalled collapse was just cancelled on return), so the expand
            // animation below is skipped — but removeSubtitle collapsed the expanded title margin, so restore it
            // directly here, otherwise there is no space between the title and the subtitle.
            binding.collapsingToolbar.expandedTitleMarginBottom =
                resources.getDimensionPixelSize(R.dimen.expanded_toolbar_bottom_margin_with_subtitle)
            return
        }
        binding.toolbarSubtitle.text = subtitle
        animatorHelper.animateCollapsingToolbarMarginBottom(show = true) {
            binding.collapsingToolbar.expandedTitleMarginBottom = it
            if (binding.collapsingToolbar.expandedTitleMarginBottom ==
                resources.getDimensionPixelSize(R.dimen.expanded_toolbar_bottom_margin_with_subtitle)
            ) {
                binding.toolbarSubtitle.show()
                binding.toolbarSubtitle.animate()
                    .scaleY(1f)
                    .setDuration(EXPAND_COLLAPSE_ANIMATION_DURATION_MILLIS)
                    .start()
            }
        }
    }

    fun enableToolbarExpansion(enable: Boolean) {
        if (!enable) {
            toolbar.title = title
        }
        binding.collapsingToolbar.isTitleEnabled = enable

        val params = (binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams)
        params.behavior = if (enable) {
            toolbarEnabledBehavior
        } else {
            toolbarDisabledBehavior
        }
    }

    /**
     * Returns a Boolean value in order to set the behaviour from a root navigation type in terms of:
     * .menu items visibility
     * .top nav bar titles
     *
     * @param isAtRoot The value that tells if root fragment is in the current destination
     * @param destination The object for the next navigation destination
     */
    private fun isAtTopLevelNavigation(isAtRoot: Boolean, destination: NavDestination): Boolean {
        val activeChild = getHostChildFragment()
        val activeChildIsRoot = activeChild != null && activeChild is TopLevelFragment
        return (isDialogDestination(destination) && activeChildIsRoot) || isAtRoot
    }

    private fun isDialogDestination(destination: NavDestination) = destination.navigatorName == DIALOG_NAVIGATOR_NAME

    @Suppress("DEPRECATION")
    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            RequestCodes.ADD_ACCOUNT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO Launch next screen
                }
                return
            }

            RequestCodes.SETTINGS -> {
                // beta features have changed. Restart activity for changes to take effect
                if (resultCode == AppSettingsActivity.RESULT_CODE_BETA_OPTIONS_CHANGED) {
                    restart()
                }
                return
            }
        }
    }

    override fun notifyTokenUpdated() {
        if (hasMagicLinkLoginIntent()) {
            loginAnalyticsListener.trackLoginMagicLinkSucceeded()
        }
    }

    @Suppress("DEPRECATION")
    override fun showLoginScreen() {
        selectedSite.reset()
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivityForResult(intent, RequestCodes.ADD_ACCOUNT)
        finish()
    }

    override fun showUserEligibilityErrorScreen() {
        val action = NavGraphMainDirections.actionGlobalUserEligibilityErrorFragment()
        navController.navigateSafely(action)
    }

    @Suppress("DEPRECATION")
    override fun showSettingsScreen() {
        AnalyticsTracker.track(AnalyticsEvent.MAIN_MENU_SETTINGS_TAPPED)
        val intent = Intent(this, AppSettingsActivity::class.java)
        startActivityForResult(intent, RequestCodes.SETTINGS)
    }

    private fun showPrivacySettingsScreen(requestedAnalyticsValue: Parcelable) {
        val intent = Intent(this, AppSettingsActivity::class.java).apply {
            putExtra(AppSettingsActivity.EXTRA_SHOW_PRIVACY_SETTINGS, true)
            putExtra(
                AppSettingsActivity.EXTRA_REQUESTED_ANALYTICS_VALUE_FROM_ERROR,
                requestedAnalyticsValue
            )
        }
        startActivityForResult(intent, RequestCodes.SETTINGS)
    }

    override fun updateSelectedSite() {
        hideProgressDialog()

        // Complete UI initialization
        binding.bottomNav.init(navController, this)
        initFragment(null)
    }

    fun startSitePicker() {
        navController.navigateSafely(
            MoreMenuFragmentDirections.actionGlobalLoginToSitePickerFragment(openedFromLogin = false)
        )
    }

    fun handleSitePickerResult() {
        presenter.selectedSiteChanged(selectedSite.get())
        restart()
    }

    /**
     * Called when the user switches sites - restarts the activity so all fragments and child fragments are reset
     */
    override fun restart() {
        val intent = intent
        intent.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_NO_ANIMATION
        )
        finish()
        startActivity(intent)
    }

    private fun hasMagicLinkLoginIntent(): Boolean {
        val action = intent.action
        val uri = intent.data
        val host = uri?.host ?: ""
        return Intent.ACTION_VIEW == action && host.contains(MAGIC_LOGIN)
    }

    override fun showOrderBadge(count: Int) {
        unfilledOrderCount = count
        binding.bottomNav.setOrderBadgeCount(count)
    }

    override fun hideOrderBadge() {
        unfilledOrderCount = 0
        binding.bottomNav.setOrderBadgeCount(0)
    }

    override fun onNavItemSelected(navPos: BottomNavigationPosition): Boolean {
        val stat = when (navPos) {
            MY_STORE -> AnalyticsEvent.MAIN_TAB_DASHBOARD_SELECTED
            ORDERS -> AnalyticsEvent.MAIN_TAB_ORDERS_SELECTED
            PRODUCTS -> AnalyticsEvent.MAIN_TAB_PRODUCTS_SELECTED
            POS -> AnalyticsEvent.MAIN_TAB_POS_SELECTED
            MORE -> AnalyticsEvent.MAIN_TAB_HUB_MENU_SELECTED
        }
        AnalyticsTracker.track(stat, mapOf(KEY_HORIZONTAL_SIZE_CLASS to deviceTypeToAnalyticsString))

        if (navPos == ORDERS) {
            viewModel.removeOrderNotifications()
        }

        if (navPos == POS) {
            posTabController.navigateToPOS()

            // Do not keep the tab selected for POS
            return false
        }

        return true
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> AnalyticsEvent.MAIN_TAB_DASHBOARD_RESELECTED
            ORDERS -> AnalyticsEvent.MAIN_TAB_ORDERS_RESELECTED
            PRODUCTS -> AnalyticsEvent.MAIN_TAB_PRODUCTS_RESELECTED
            MORE -> AnalyticsEvent.MAIN_TAB_HUB_MENU_RESELECTED
            POS -> null
        }
        stat?.let {
            AnalyticsTracker.track(it, mapOf(KEY_HORIZONTAL_SIZE_CLASS to deviceTypeToAnalyticsString))
        }

        // if we're at the root scroll the active fragment to the top
        // TODO bring back clearing the backstack when the navgraphs are fixed to support multiple backstacks:
        // https://github.com/woocommerce/woocommerce-android/issues/7183
        if (isAtNavigationRoot()) {
            // If the fragment's view is not yet created, do nothing
            if (getActiveTopLevelFragment()?.view != null) {
                getActiveTopLevelFragment()?.scrollToTop()
                expandToolbar(expand = true, animate = true)
            }
        }
    }
    // endregion

    // region Fragment Processing
    private fun initFragment(savedInstanceState: Bundle?) {
        setupObservers()
        val openedFromPush = intent.getBooleanExtra(FIELD_OPENED_FROM_PUSH, false)
        val localNotification = intent.getParcelableExtra<Notification>(FIELD_LOCAL_NOTIFICATION)

        // Reset this flag now that it's being processed
        intent.removeExtra(FIELD_OPENED_FROM_PUSH)

        if (savedInstanceState != null) {
            restoreSavedInstanceState(savedInstanceState)
        } else if (openedFromPush) {
            // Opened from a push notification
            menu?.close()

            val localPushId = intent.getIntExtra(FIELD_PUSH_ID, 0)
            val notification = intent.getParcelableExtra<Notification>(FIELD_REMOTE_NOTIFICATION)
            // Reset this flag now that it's being processed
            intent.removeExtra(FIELD_REMOTE_NOTIFICATION)
            intent.removeExtra(FIELD_PUSH_ID)

            viewModel.onPushNotificationTapped(localPushId, notification)
        } else if (localNotification != null) {
            intent.removeExtra(FIELD_LOCAL_NOTIFICATION)
            viewModel.onLocalNotificationTapped(localNotification)
        }
    }
    // endregion

    private fun setupStoreConnectionErrorDialog() {
        // Reset the dialog snooze when the app goes to the background (process lifecycle), so a
        // still-unreachable store re-alerts on the next foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(appBackgroundObserver)

        binding.storeConnectionErrorComposeView.setViewCompositionStrategy(
            ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed
        )
        binding.storeConnectionErrorComposeView.setContent {
            WooTheme {
                val showDialog by viewModel.showStoreConnectionErrorDialog.observeAsState(false)
                if (showDialog) {
                    StoreConnectionErrorDialog(
                        onContactSupportClick = viewModel::onStoreConnectionErrorContactSupportClicked,
                        onDismissClick = viewModel::onStoreConnectionErrorDismissed,
                    )
                }
            }
        }
    }

    @Suppress("ComplexMethod")
    private fun setupObservers() {
        viewModel.event.observe(this) { event ->
            when (event) {
                is ViewMyStoreStats -> binding.bottomNav.currentPosition = MY_STORE
                is ViewOrderList -> binding.bottomNav.currentPosition = ORDERS
                is ViewOrderDetail -> showOrderDetail(event)
                is ViewReviewDetail -> showReviewDetail(event.uniqueId, launchedFromNotification = true)
                is ViewProductDetail -> showProductDetail(event.uniqueId)
                is ViewReviewList -> showReviewList()
                is ViewBlazeCampaignDetail -> showBlazeCampaignList(event.campaignId)
                ViewBlazeCampaignList -> showBlazeCampaignList(campaignId = null)
                is RestartActivityEvent -> onRestartActivityEvent(event)
                is ShowFeatureAnnouncement -> navigateToFeatureAnnouncement(event)
                is ViewUrlInWebView -> navigateToWebView(event)
                is RequestNotificationsPermission -> requestNotificationsPermission()
                ViewPayments -> showPayments()
                ViewTapToPay -> showTapToPaySummary()
                ViewWooPosPromo -> showWooPosPromoCarousel()
                ShortcutOpenPayments -> shortcutShowPayments()
                ShortcutOpenOrderCreation -> shortcutOpenOrderCreation()
                is MainActivityViewModel.ContactSupportForStoreConnection ->
                    startHelpActivity(
                        HelpOrigin.CONNECTION_ERROR,
                        extraSupportTags = listOf(WooError.REST_INVALID_SIGNATURE_CODE)
                    )
                is MainActivityViewModel.ShowPrivacyPreferenceUpdatedFailed -> {
                    uiMessageResolver.getIndefiniteActionSnack(
                        R.string.privacy_banner_error_save,
                        actionText = getString(R.string.retry)
                    ) {
                        viewModel.onRequestPrivacyUpdate(event.analyticsEnabled)
                    }.show()
                }

                MainActivityViewModel.ShowPrivacySettings -> {
                    showPrivacySettingsScreen(RequestedAnalyticsValue.NONE)
                }

                is MainActivityViewModel.ShowPrivacySettingsWithError -> {
                    showPrivacySettingsScreen(event.requestedAnalyticsValue)
                }

                is MainActivityViewModel.CreateNewProductUsingImages -> showAddProduct(event.imageUris)
                is MultiLiveEvent.Event.ShowDialog -> event.showIn(this)
                MainActivityViewModel.LaunchBlazeCampaignCreation -> {
                    // Propagate it to the DashboardBlazeCard
                    event.isHandled = false
                }

                is MainActivityViewModel.ViewSurvey -> showSurvey(event.surveyType)
            }
        }

        observeNotificationsPermissionBarVisibility()
        observeMoreMenuBadgeStateEvent()
        observeTrialStatus()
        observeBottomBarState()
        observeUserAgeEligibilityState()
    }

    private fun showBlazeCampaignList(campaignId: String?) {
        binding.bottomNav.currentPosition = MORE
        binding.bottomNav.active(MORE.position)

        navController.navigateSafely(
            MoreMenuFragmentDirections.actionMoreMenuToBlazeCampaignListFragment(
                campaignId = campaignId
            ),
        )
    }

    private fun observeNotificationsPermissionBarVisibility() {
        viewModel.isNotificationsPermissionCardVisible.observe(this) { isVisible ->
            if (isVisible) {
                binding.notificationsPermissionBar.apply {
                    setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                    setContent {
                        WooThemeWithBackground {
                            NotificationsPermissionCard()
                        }
                    }
                }
                handler.postDelayed(
                    notificationPermissionBarRunnable,
                    NOTIFICATIONS_PERMISSION_BAR_DISPLAY_DELAY
                )
            } else {
                animateBottomBar(binding.notificationsPermissionBar, show = false)
            }
        }
    }

    private fun observeBottomBarState() {
        viewModel.bottomBarState.observe(this) { bottomBarState ->
            val show = when (bottomBarState) {
                BottomBarState.Hidden -> false
                BottomBarState.Visible -> true
            }

            animateBottomBar(binding.bottomNav, show, Duration.MEDIUM)
        }
    }

    private fun observeMoreMenuBadgeStateEvent() {
        viewModel.moreMenuBadgeState.observe(this) { moreMenuBadgeState ->
            when (moreMenuBadgeState) {
                is UnseenReviews -> binding.bottomNav.showMoreMenuUnseenReviewsBadge(moreMenuBadgeState.count)
                NewFeature -> binding.bottomNav.showMoreMenuNewFeatureBadge()
                Hidden -> binding.bottomNav.hideMoreMenuBadge()
            }
        }
    }

    private fun observeTrialStatus() {
        viewModel.trialStatusBarState.observe(this) { trialStatusBarState ->
            when (trialStatusBarState) {
                TrialStatusBarState.Hidden ->
                    animateBottomBar(binding.trialBar, show = false)

                is TrialStatusBarState.Visible -> {
                    binding.trialBar.text = trialStatusBarFormatterFactory.create(
                        context = this
                    ).format(trialStatusBarState.daysLeft)
                    binding.trialBar.movementMethod = LinkMovementMethod.getInstance()
                    animateBottomBar(binding.trialBar, show = true)
                }
            }
        }
    }

    private fun observeUserAgeEligibilityState() {
        viewModel.isUserAgeRangeEligible.observe(this) { ageEligibilityState ->
            if (ageEligibilityState.isUserAgeRangeEligible.not()) {
                showLoginScreen()
            }
        }
    }

    private fun requestNotificationsPermission() {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            WooPermissionUtils.requestNotificationsPermission(launcher)
        }
    }

    private fun navigateToFeatureAnnouncement(event: ShowFeatureAnnouncement) {
        if (!PackageUtils.isTesting()) {
            val action = NavGraphMainDirections.actionGlobalFeatureAnnouncementDialogFragmentOnMain(event.announcement)
            navController.navigateSafely(action)
        }
    }

    private fun navigateToWebView(event: ViewUrlInWebView) {
        navController.navigate(
            NavGraphMainDirections.actionGlobalAuthenticatedWebViewFragment(
                urlToLoad = event.url
            )
        )
    }

    @Suppress("LongParameterList")
    fun navigateToGlobalInfoScreenFragment(
        screenTitle: Int,
        heading: Int,
        message: Int,
        linkTitle: Int,
        imageResource: Int,
        linkAction: InfoScreenFragment.InfoScreenLinkAction
    ) {
        val action = NavGraphMainDirections.actionGlobalInfoScreenFragment(
            screenTitle = screenTitle,
            heading = heading,
            message = message,
            linkTitle = linkTitle,
            imageResource = imageResource,
            linkAction = linkAction
        )
        navController.navigate(action)
    }

    private fun showOrderDetail(event: ViewOrderDetail) {
        intent.data = null
        showOrderDetail(
            orderId = event.uniqueId,
            launchedFromNotification = true
        )
    }

    private fun onRestartActivityEvent(event: RestartActivityEvent) {
        intent.apply {
            when (event) {
                is RestartActivityForAppLink -> data = event.data
                is RestartActivityForLocalNotification -> putExtra(FIELD_LOCAL_NOTIFICATION, event.notification)
                is RestartActivityForPushNotification -> {
                    putExtra(FIELD_OPENED_FROM_PUSH, true)
                    putExtra(FIELD_REMOTE_NOTIFICATION, event.notification)
                    putExtra(FIELD_PUSH_ID, event.pushId)
                }
            }
        }
        restart()
    }

    override fun showProductDetail(remoteProductId: Long, popUpToProductList: Boolean, sharedView: View?) {
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
        )
        val extras = if (sharedView != null) {
            val productCardDetailTransitionName = getString(R.string.product_card_detail_transition_name)
            FragmentNavigatorExtras(sharedView to productCardDetailTransitionName)
        } else {
            null
        }
        navController.navigateSafely(
            directions = action,
            extras = extras,
            navOptions = navOptions {
                if (popUpToProductList) {
                    popUpTo(R.id.products)
                }
            }
        )
    }

    override fun showProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        // variation detail is part of the products navigation graph, and product detail is the starting destination
        // for that graph, so we have to use a deep link to navigate to variation detail
        val query = "?remoteProductId=$remoteProductId&remoteVariationId=$remoteVariationId"
        val deeplink = "wcandroid://variationDetail$query"
        navController.navigate(Uri.parse(deeplink))
    }

    override fun showAddProduct(imageUris: List<String>) {
        showBottomNav()
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.AddNewProduct,
            images = imageUris.toTypedArray()
        )
        navController.navigateSafely(action)
    }

    private fun showReviewList() {
        showBottomNav()
        binding.bottomNav.currentPosition = MORE
        binding.bottomNav.active(MORE.position)
        val action = MoreMenuFragmentDirections.actionMoreMenuToReviewList()
        navController.navigateSafely(action)
    }

    override fun showReviewDetail(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        tempStatus: String?
    ) {
        if (launchedFromNotification) {
            binding.bottomNav.currentPosition = MORE
            binding.bottomNav.active(MORE.position)
        }

        val action = NavGraphMainDirections.actionGlobalReviewDetailFragment(
            remoteReviewId = remoteReviewId,
            tempStatus = tempStatus,
            launchedFromNotification = launchedFromNotification
        )
        navController.navigateSafely(action)
    }

    private fun shortcutOpenOrderCreation() {
        /**
         * set the intent action to null so that when the OS recreates the activity
         * by redelivering the same intent, it won't redirect to the shortcut screen.
         *
         * Example:
         * 1. Open the payments shortcut by long pressing the app icon
         * 2. Navigate back from the payments screen into the main screen (MyStore screen)
         * 3. Rotate the device.
         * 6. The OS redelivers the intent with the intent action set to order creation shortcut and as a result
         * the app redirects to the order creation screen as soon as the app is opened.
         *
         * Setting the intent action to null avoids this bug.
         */
        intent.action = null
        binding.bottomNav.currentPosition = ORDERS
        binding.bottomNav.active(ORDERS.position)
        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderCreationFragment(
            OrderCreateEditViewModel.Mode.Creation,
            null,
            null,
        )
        navController.navigateSafely(action)
    }

    private fun shortcutShowPayments() {
        /**
         * set the intent action to null so that when the OS recreates the activity
         * by redelivering the same intent, it won't redirect to the shortcut screen.
         *
         * Example:
         * 1. Open the payments shortcut by long pressing the app icon
         * 2. Navigate back from the payments screen into the main screen (MyStore screen)
         * 3. Rotate the device.
         * 6. The OS redelivers the intent with the intent action set to payments shortcut and as a result
         * the app redirects to the payments screen as soon as the app is opened.
         *
         * Setting the intent action to null avoids this bug.
         */
        intent.action = null
        showPayments()
    }

    private fun showPayments(
        openInHub: CardReaderFlowParam.CardReadersHub.OpenInHub = CardReaderFlowParam.CardReadersHub.OpenInHub.NONE
    ) {
        showBottomNav()
        binding.bottomNav.currentPosition = MORE
        binding.bottomNav.active(MORE.position)
        val action = MoreMenuFragmentDirections.actionMoreMenuToPaymentFlow(
            CardReaderFlowParam.CardReadersHub(openInHub)
        )
        navController.navigateSafely(action)
    }

    private fun showWooPosPromoCarousel() {
        intent.data = null
        PosPromoDialogFragment.show(supportFragmentManager)
    }

    private fun showTapToPaySummary() {
        /**
         * set the intent data to null so that when the OS recreates the activity
         * by redelivering the same intent, it won't redirect to the tap to pay summary screen.
         *
         * Example:
         * 1. Open the Tap to pay summary screen via universal linking
         * 2. Navigate back from the payments screen and go to the settings screen
         * 3. Try to switch to any other store.
         * 6. The OS redelivers the same intent with the intent data set to TTP URI and as a result
         * the app redirects to the TTP summary screen as soon as the app restarts.
         *
         * Setting the intent data to null avoids this bug.
         */
        intent.data = null
        showPayments(CardReaderFlowParam.CardReadersHub.OpenInHub.TAP_TO_PAY_SUMMARY)
    }

    override fun showReviewDetailWithSharedTransition(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        sharedView: View,
        tempStatus: String?
    ) {
        val reviewCardDetailTransitionName = getString(R.string.review_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to reviewCardDetailTransitionName)
        val action = ReviewListFragmentDirections.actionReviewListFragmentToReviewDetailFragment(
            remoteReviewId = remoteReviewId,
            tempStatus = tempStatus,
            launchedFromNotification = launchedFromNotification
        )
        navController.navigateSafely(directions = action, extras = extras)
    }

    override fun showProductFilters(
        stockStatus: String?,
        productType: String?,
        productStatus: String?,
        productCategory: String?,
        productCategoryName: String?
    ) {
        val action = ProductListFragmentDirections.actionProductListFragmentToProductFilterListFragment(
            selectedStockStatus = stockStatus,
            selectedProductStatus = productStatus,
            selectedProductType = productType,
            selectedProductCategoryId = productCategory,
            selectedProductCategoryName = productCategoryName
        )
        navController.navigateSafely(action)
    }

    fun showOrderCreation(
        mode: OrderCreateEditViewModel.Mode,
        giftCardCode: String?,
        giftCardAmount: BigDecimal?,
        orderCurrency: String? = null,
    ) {
        NavGraphMainDirections.actionGlobalToOrderCreationFragment(
            mode = mode,
            giftCardCode = giftCardCode,
            giftCardAmount = giftCardAmount,
            orderCurrency = orderCurrency
        ).apply {
            navController.navigateSafely(this)
        }
    }

    override fun showOrderDetail(
        orderId: Long,
        navHostFragment: NavHostFragment?,
        launchedFromNotification: Boolean,
        startPaymentsFlow: Boolean,
    ) {
        if (launchedFromNotification) {
            binding.bottomNav.currentPosition = ORDERS
            binding.bottomNav.active(ORDERS.position)
            navController.popBackStack(R.id.orders, false)
        }

        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(
            orderId,
            longArrayOf(orderId)
        )
        navHostFragment?.navController?.let { navController ->
            val bundle = OrderDetailFragmentArgs(
                orderId = orderId,
                allOrderIds = longArrayOf(orderId),
                startPaymentFlow = startPaymentsFlow
            ).toBundle()
            navController.navigate(
                R.id.orderDetailFragment,
                bundle,
                navOptions = NavOptions.Builder().setLaunchSingleTop(true).build()
            )
        } ?: run {
            navController.navigateSafely(action)
        }
        crashLogging.recordEvent("Opening order $orderId")
    }

    override fun showOrderDetailWithSharedTransition(
        orderId: Long,
        allOrderIds: List<Long>,
        sharedView: View
    ) {
        val orderCardDetailTransitionName = getString(R.string.order_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to orderCardDetailTransitionName)

        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(
            orderId,
            allOrderIds.toLongArray()
        )
        crashLogging.recordEvent("Opening order $orderId")
        navController.navigateSafely(
            directions = action,
            extras = extras,
        )
    }

    @Deprecated("Deprecated in Java")
    override fun onAttachFragment(fragment: Fragment) {
        super.onAttachFragment(fragment)
        updateAppBarVisibility(fragment)
    }

    private fun updateAppBarVisibility(fragment: Fragment) {
        (fragment as? BaseFragment)?.let {
            when (it.activityAppBarStatus) {
                is AppBarStatus.Hidden -> supportActionBar?.hide()
                is AppBarStatus.Visible -> supportActionBar?.show()
            }
        }
    }

    override fun showFeedbackSurvey() {
        NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.MAIN).apply {
            navController.navigateSafely(this)
        }
    }

    private fun showSurvey(surveyType: SurveyType) {
        NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(
            surveyType = surveyType
        ).apply {
            navController.navigateSafely(this)
        }
    }

    override fun updateOfflineStatusBar(isConnected: Boolean) {
        if (isConnected) binding.offlineBar.hide() else binding.offlineBar.show()
    }

    private fun checkConnection() {
        updateOfflineStatusBar(NetworkUtils.isNetworkAvailable(this))
    }

    override fun hideBottomNav() {
        viewModel.hideBottomNav()
    }

    override fun showBottomNav() {
        viewModel.showBottomNav()
    }

    /**
     * The Flexible in app update is successful.
     * Display a success snack bar and ask users to manually restart the app
     */
    override fun showAppUpdateSuccessSnack(actionListener: View.OnClickListener) {
        uiMessageResolver.getRestartSnack(
            stringResId = R.string.update_downloaded,
            actionListener = actionListener
        )
            .show()
    }

    /**
     * The Flexible in app update was not successful.
     * Display a failure snack bar and ask users to retry
     */
    override fun showAppUpdateFailedSnack(actionListener: View.OnClickListener) {
        uiMessageResolver.getRetrySnack(
            R.string.update_failed,
            actionListener = actionListener
        )
            .show()
    }

    override fun updateStatsWidgets() {
        appWidgetUpdaters.updateTodayWidget()
    }

    private fun trackIfOpenedFromWidget() {
        if (intent.getBooleanExtra(FIELD_OPENED_FROM_WIDGET, false)) {
            val widgetName = intent.getStringExtra(FIELD_WIDGET_NAME)
            AnalyticsTracker.track(
                stat = AnalyticsEvent.WIDGET_TAPPED,
                properties = mapOf(AnalyticsTracker.KEY_NAME to widgetName)
            )
            // Reset these flag now that they have being processed
            intent.removeExtra(FIELD_OPENED_FROM_WIDGET)
            intent.removeExtra(FIELD_WIDGET_NAME)
        }
    }
}
