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
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
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
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Notification
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.appwidgets.WidgetUpdater
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.common.InfoScreenFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.MORE
import com.woocommerce.android.ui.main.BottomNavigationPosition.MY_STORE
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
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
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewMyStoreStats
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewOrderList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewPayments
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewDetail
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewReviewList
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewTapToPay
import com.woocommerce.android.ui.main.MainActivityViewModel.ViewUrlInWebView
import com.woocommerce.android.ui.moremenu.MoreMenuFragmentDirections
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentArgs
import com.woocommerce.android.ui.orders.list.OrderListFragmentDirections
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderFlowParam
import com.woocommerce.android.ui.plans.di.TrialStatusBarFormatterFactory
import com.woocommerce.android.ui.plans.trial.DetermineTrialStatusBarState.TrialStatusBarState
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.prefs.RequestedAnalyticsValue
import com.woocommerce.android.ui.products.details.ProductDetailFragment
import com.woocommerce.android.ui.products.list.ProductListFragmentDirections
import com.woocommerce.android.ui.reviews.ReviewListFragmentDirections
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.PackageUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.util.WooAnimUtils.animateBottomBar
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.DisabledAppBarLayoutBehavior
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
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

    @Inject lateinit var animatorHelper: MainAnimatorHelper

    private val viewModel: MainActivityViewModel by viewModels()

    private var unfilledOrderCount: Int = 0
    private var menu: Menu? = null

    private val toolbarEnabledBehavior = AppBarLayout.Behavior()
    private val toolbarDisabledBehavior = DisabledAppBarLayoutBehavior()

    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar

    private val appBarOffsetListener by lazy {
        AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbarSubtitle.alpha = ((1.0f - abs((verticalOffset / appBarLayout.totalScrollRange.toFloat()))))
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
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            if (f is DialogFragment) return

            when (val appBarStatus = (f as? BaseFragment)?.activityAppBarStatus ?: AppBarStatus.Visible()) {
                is AppBarStatus.Visible -> {
                    showToolbar(animate = f is TopLevelFragment)
                    // re-expand the AppBar when returning to top level fragment,
                    // collapse it when entering a child fragment
                    if (f is TopLevelFragment) {
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
                    binding.appBarLayout.targetElevation = if (appBarStatus.hasShadow) {
                        resources.getDimensionPixelSize(dimen.appbar_elevation).toFloat()
                    } else {
                        0f
                    }
                    binding.appBarDivider.isVisible = appBarStatus.hasDivider
                }

                AppBarStatus.Hidden -> hideToolbar(animate = f is TopLevelFragment)
            }

            if (f is TopLevelFragment) {
                showBottomNav()
            } else {
                hideBottomNav()
            }
        }
    }

    private val launcher = this.registerForActivityResult(RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            viewModel.checkForNotificationsPermission(hasNotificationsPermission = true)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        ChromeCustomTabUtils.registerForPartialTabUsage(this)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.toolbar.toolbar
        setSupportActionBar(toolbar)
        toolbar.navigationIcon = null

        animatorHelper.toolbarHeight = binding.collapsingToolbar.layoutParams.height

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val graphInflater = navHostFragment.navController.navInflater

        val navGraph = graphInflater.inflate(R.navigation.nav_graph_main)
        navGraph.setStartDestination(viewModel.startDestination)

        navController = navHostFragment.navController
        navController.graph = navGraph
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleObserver, false)
        binding.bottomNav.init(navController, this)

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
            viewModel.handleShortcutAction(intent?.action?.toLowerCase(Locale.ROOT))
            handleIncomingImages()
        }
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
        viewModel.showFeatureAnnouncementIfNeeded()
    }

    override fun onPause() {
        binding.appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
        super.onPause()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        setIntent(intent)
        initFragment(null)

        viewModel.handleIncomingAppLink(intent?.data)
        handleIncomingImages()
    }

    public override fun onDestroy() {
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

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        getActiveChildFragment()?.let { fragment ->
            if (fragment is BackPressListener && !(fragment as BackPressListener).onRequestAllowBackPress()) {
                return
            }
        }

        super.onBackPressed()
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
        val tag = binding.bottomNav.currentPosition.getTag()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.childFragmentManager.findFragmentByTag(tag) as? TopLevelFragment
    }

    /**
     * Returns the fragment currently shown by the navigation component, or null if we're at the root
     */
    private fun getActiveChildFragment(): Fragment? {
        return if (isChildFragmentShowing()) {
            getHostChildFragment()
        } else {
            null
        }
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

    private fun showToolbar(animate: Boolean) {
        // Cancel any pending toolbar animations
        animatorHelper.cancelToolbarAnimation()

        if (binding.collapsingToolbar.layoutParams.height == animatorHelper.toolbarHeight) return
        if (animate) {
            animatorHelper.animateToolbarHeight(show = true) {
                binding.collapsingToolbar.updateLayoutParams {
                    height = it
                }
            }
        } else {
            binding.collapsingToolbar.updateLayoutParams {
                height = animatorHelper.toolbarHeight
            }
        }
    }

    private fun hideToolbar(animate: Boolean) {
        // Cancel any pending toolbar animations
        animatorHelper.cancelToolbarAnimation()

        if (binding.collapsingToolbar.layoutParams.height == 0) return
        if (animate) {
            animatorHelper.animateToolbarHeight(show = false) {
                binding.collapsingToolbar.updateLayoutParams {
                    height = it
                }
            }
        } else {
            binding.collapsingToolbar.updateLayoutParams {
                height = 0
            }
        }
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        binding.collapsingToolbar.title = title
    }

    fun expandToolbar(expand: Boolean, animate: Boolean) {
        binding.appBarLayout.setExpanded(expand, animate)
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
        if (binding.toolbarSubtitle.visibility == View.GONE) return
        if (binding.collapsingToolbar.layoutParams.height != 0) {
            binding.toolbarSubtitle.collapse(duration = 200L)
            animatorHelper.animateCollapsingToolbarMarginBottom(show = false) {
                binding.collapsingToolbar.expandedTitleMarginBottom = it
            }
        } else {
            binding.toolbarSubtitle.hide()
        }
    }

    private fun setFadingSubtitleOnCollapsingToolbar(subtitle: CharSequence) {
        binding.appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
        binding.toolbarSubtitle.text = subtitle
        if (binding.toolbarSubtitle.visibility == View.VISIBLE) return
        binding.toolbarSubtitle.expand(duration = 200L)
        animatorHelper.animateCollapsingToolbarMarginBottom(show = true) {
            binding.collapsingToolbar.expandedTitleMarginBottom = it
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

    override fun onNavItemSelected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> AnalyticsEvent.MAIN_TAB_DASHBOARD_SELECTED
            ORDERS -> AnalyticsEvent.MAIN_TAB_ORDERS_SELECTED
            PRODUCTS -> AnalyticsEvent.MAIN_TAB_PRODUCTS_SELECTED
            MORE -> AnalyticsEvent.MAIN_TAB_HUB_MENU_SELECTED
        }
        AnalyticsTracker.track(stat, mapOf(KEY_HORIZONTAL_SIZE_CLASS to deviceTypeToAnalyticsString))

        if (navPos == ORDERS) {
            viewModel.removeOrderNotifications()
        }
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> AnalyticsEvent.MAIN_TAB_DASHBOARD_RESELECTED
            ORDERS -> AnalyticsEvent.MAIN_TAB_ORDERS_RESELECTED
            PRODUCTS -> AnalyticsEvent.MAIN_TAB_PRODUCTS_RESELECTED
            MORE -> AnalyticsEvent.MAIN_TAB_HUB_MENU_RESELECTED
        }
        AnalyticsTracker.track(stat, mapOf(KEY_HORIZONTAL_SIZE_CLASS to deviceTypeToAnalyticsString))

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

            viewModel.handleIncomingNotification(localPushId, notification)
        } else if (localNotification != null) {
            intent.removeExtra(FIELD_LOCAL_NOTIFICATION)
            viewModel.onLocalNotificationTapped(localNotification)
        }
    }
    // endregion

    @Suppress("ComplexMethod")
    private fun setupObservers() {
        viewModel.event.observe(this) { event ->
            when (event) {
                is ViewMyStoreStats -> binding.bottomNav.currentPosition = MY_STORE
                is ViewOrderList -> binding.bottomNav.currentPosition = ORDERS
                is ViewOrderDetail -> showOrderDetail(event)
                is ViewReviewDetail -> showReviewDetail(event.uniqueId, launchedFromNotification = true)
                is ViewReviewList -> showReviewList()
                is RestartActivityEvent -> onRestartActivityEvent(event)
                is ShowFeatureAnnouncement -> navigateToFeatureAnnouncement(event)
                is ViewUrlInWebView -> navigateToWebView(event)
                is RequestNotificationsPermission -> requestNotificationsPermission()
                ViewPayments -> showPayments()
                ViewTapToPay -> showTapToPaySummary()
                ShortcutOpenPayments -> shortcutShowPayments()
                ShortcutOpenOrderCreation -> shortcutOpenOrderCreation()
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
            }
        }

        observeNotificationsPermissionBarVisibility()
        observeMoreMenuBadgeStateEvent()
        observeTrialStatus()
        observeBottomBarState()
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

    private fun requestNotificationsPermission() {
        if (VERSION.SDK_INT >= VERSION_CODES.TIRAMISU) {
            WooPermissionUtils.requestNotificationsPermission(launcher)
        }
    }

    private fun navigateToFeatureAnnouncement(event: ShowFeatureAnnouncement) {
        if (!PackageUtils.isTesting()) {
            val action = NavGraphMainDirections.actionOpenWhatsnewFromMain(event.announcement)
            navController.navigateSafely(action)
        }
    }

    private fun navigateToWebView(event: ViewUrlInWebView) {
        navController.navigate(
            NavGraphMainDirections.actionGlobalWPComWebViewFragment(
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
            remoteNoteId = event.remoteNoteId,
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

    override fun showProductDetail(remoteProductId: Long, enableTrash: Boolean) {
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
            isTrashEnabled = enableTrash
        )
        navController.navigateSafely(action)
    }

    override fun showProductDetailWithSharedTransition(remoteProductId: Long, sharedView: View, enableTrash: Boolean) {
        val productCardDetailTransitionName = getString(R.string.product_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to productCardDetailTransitionName)

        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            mode = ProductDetailFragment.Mode.ShowProduct(remoteProductId),
            isTrashEnabled = enableTrash
        )
        navController.navigateSafely(directions = action, extras = extras)
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
            OrderCreateEditViewModel.Mode.Creation(),
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
        giftCardAmount: BigDecimal?
    ) {
        NavGraphMainDirections.actionGlobalToOrderCreationFragment(
            mode = mode,
            giftCardCode = giftCardCode,
            giftCardAmount = giftCardAmount,
        ).apply {
            navController.navigateSafely(this)
        }
    }

    override fun showOrderDetail(
        orderId: Long,
        navHostFragment: NavHostFragment?,
        remoteNoteId: Long,
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
            arrayOf(orderId).toLongArray(),
            remoteNoteId
        )
        navHostFragment?.navController?.let { navController ->
            val bundle = OrderDetailFragmentArgs(
                orderId,
                longArrayOf(orderId),
                remoteNoteId,
                startPaymentsFlow
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
        remoteNoteId: Long,
        sharedView: View
    ) {
        val orderCardDetailTransitionName = getString(R.string.order_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to orderCardDetailTransitionName)

        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(
            orderId,
            allOrderIds.toLongArray(),
            remoteNoteId
        )
        crashLogging.recordEvent("Opening order $orderId")
        navController.navigateSafely(directions = action, extras = extras, navOptions = NavOptions.Builder().setLaunchSingleTop(true).setRestoreState(false).build())
    }

    override fun showFeedbackSurvey() {
        NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.MAIN).apply {
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
