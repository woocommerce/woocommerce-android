package com.woocommerce.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Resources.Theme
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.R.navigation
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.extensions.getRemoteOrderId
import com.woocommerce.android.extensions.getWooType
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.navigation.KeepStateNavigator
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.MY_STORE
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.ui.main.BottomNavigationPosition.PRODUCTS
import com.woocommerce.android.ui.main.BottomNavigationPosition.REVIEWS
import com.woocommerce.android.ui.orders.list.OrderListFragmentDirections
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.products.ProductListFragmentDirections
import com.woocommerce.android.ui.reviews.ReviewListFragmentDirections
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.DisabledAppBarLayoutBehavior
import com.woocommerce.android.widgets.WCPromoDialog
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton
import com.woocommerce.android.widgets.WCPromoTooltip
import com.woocommerce.android.widgets.WCPromoTooltip.Feature
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
import java.util.Locale
import javax.inject.Inject

class MainActivity : AppUpgradeActivity(),
    MainContract.View,
    HasAndroidInjector,
    MainNavigationRouter,
    MainBottomNavigationView.MainNavigationListener,
    NavController.OnDestinationChangedListener,
    WCPromoDialog.PromoDialogListener {
    companion object {
        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"

        private const val KEY_BOTTOM_NAV_POSITION = "key-bottom-nav-position"
        private const val KEY_UNFILLED_ORDER_COUNT = "unfilled-order-count"

        private const val DIALOG_NAVIGATOR_NAME = "dialog"

        // push notification-related constants
        const val FIELD_OPENED_FROM_PUSH = "opened-from-push-notification"
        const val FIELD_REMOTE_NOTE_ID = "remote-note-id"
        const val FIELD_OPENED_FROM_PUSH_GROUP = "opened-from-push-group"
        const val FIELD_OPENED_FROM_ZENDESK = "opened-from-zendesk"
        const val FIELD_NOTIFICATION_TYPE = "notification-type"

        interface BackPressListener {
            fun onRequestAllowBackPress(): Boolean
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: MainContract.Presenter
    @Inject lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private var isBottomNavShowing = true
    private var previousDestinationId: Int? = null
    private var unfilledOrderCount: Int = 0
    private var isMainThemeApplied = false
    private var restoreToolbarHeight = 0
    private var menu: Menu? = null

    private val toolbarEnabledBehavior = AppBarLayout.Behavior()
    private val toolbarDisabledBehavior = DisabledAppBarLayoutBehavior()

    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var progressDialog: ProgressDialog? = null

    private val fragmentLifecycleObserver: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            val currentDestination = navController.currentDestination!!
            val isFullScreenFragment = currentDestination.id == R.id.productImageViewerFragment ||
                currentDestination.id == R.id.wpMediaViewerFragment

            val isDialogDestination = currentDestination.navigatorName == DIALOG_NAVIGATOR_NAME

            if (!isFullScreenFragment && !isDialogDestination) {
                // re-expand the AppBar when returning to top level fragment, collapse it when entering a child fragment
                if (f is TopLevelFragment) {
                    // We need to post this to the view handler to make sure shouldExpandToolbar returns the correct value
                    f.view?.post {
                        if (f.view != null) {
                            expandToolbar(expand = f.shouldExpandToolbar(), animate = false)
                        }
                    }
                } else {
                    expandToolbar(expand = false, animate = false)
                }

                // collapsible toolbar should only be able to expand for top-level fragments
                enableToolbarExpansion(f is TopLevelFragment)
            }
        }
    }

    /**
     * Manually set the theme here so the splash screen will be visible while this activity
     * is loading. Also setting it here ensures all fragments used in this activity will also
     * use this theme at runtime (in the case of switching the theme at runtime).
     */
    override fun getTheme(): Theme {
        return super.getTheme().also {
            // Since applying the theme overwrites all theme properties and then applies,
            // we only want to do this once per session to avoid unnecessary GC as well as
            // OOM crashes in older versions of Android.
            if (!isMainThemeApplied) {
                it.applyStyle(R.style.Theme_Woo_DayNight, true)
                isMainThemeApplied = true
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.toolbar.toolbar
        setSupportActionBar(toolbar)
        toolbar.navigationIcon = null

        presenter.takeView(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        val navigator = KeepStateNavigator(this, navHostFragment.childFragmentManager, R.id.nav_host_fragment_main)
        navController = navHostFragment.navController
        with(navController) {
            navigatorProvider.addNavigator(navigator)
            setGraph(R.navigation.nav_graph_main)
            addOnDestinationChangedListener(this@MainActivity)
        }
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleObserver, false)

        binding.bottomNav.init(navController, this)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        // fetch the site list if the database has been downgraded - otherwise the site picker will be displayed,
        // which we don't want in this situation
        if (AppPrefs.getDatabaseDowngraded()) {
            presenter.fetchSitesAfterDowngrade()
            AppPrefs.setDatabaseDowngraded(false)
            return
        }

        if (!selectedSite.exists()) {
            showSitePickerScreen()
            return
        }

        initFragment(savedInstanceState)

        // show the app rating dialog if it's time
        AppRatingDialog.showIfNeeded(this)

        // check for any new app updates only after the user has logged into the app (release builds only)
        if (!BuildConfig.DEBUG) {
            checkForAppUpdates()
        }
    }

    override fun hideProgressDialog() {
        progressDialog?.apply {
            if (isShowing) {
                cancel()
            }
        }
    }

    override fun showProgressDialog(@StringRes stringId: Int) {
        hideProgressDialog()
        progressDialog = ProgressDialog.show(this, "", getString(stringId), true)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        updateReviewsBadge()
        updateOrderBadge(false)

        checkConnection()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        setIntent(intent)
        initFragment(null)
    }

    public override fun onDestroy() {
        presenter.dropView()
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

        if (!isAtNavigationRoot()) {
            // go no further if active fragment doesn't allow back press - we use this so fragments can
            // provide confirmation before discarding the current action, such as adding an order note
            getActiveChildFragment()?.let { fragment ->
                if (fragment is BackPressListener && !(fragment as BackPressListener).onRequestAllowBackPress()) {
                    return
                }
            }
            navController.navigateUp()
            return
        } else if (binding.bottomNav.currentPosition != MY_STORE) {
            navController.navigate(R.id.dashboard)
        } else {
            finish()
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
                currentDestinationId == R.id.reviews
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

    /**
     * The current fragment in the nav controller has changed
     */
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val isAtRoot = isAtNavigationRoot()
        val isTopLevelNavigation = isAtTopLevelNavigation(isAtRoot = isAtRoot, destination = destination)

        // go no further if this is the initial navigation to the root fragment, or if the destination is
        // a dialog (since we don't need to change anything for dialogs)
        if ((isAtRoot && previousDestinationId == null) || isDialogDestination(destination)) {
            previousDestinationId = destination.id
            return
        }

        val showCrossIcon: Boolean
        if (isTopLevelNavigation) {
            if (destination.id != R.id.dashboard && destination.id != R.id.orders) {
                // MyStoreFragment and OrderListFragment handle the elevation by themselves
                binding.appBarLayout.elevation = 0f
            }
            showCrossIcon = false
        } else {
            binding.appBarLayout.elevation = resources.getDimensionPixelSize(R.dimen.appbar_elevation).toFloat()
            showCrossIcon = when (destination.id) {
                R.id.productFilterListFragment,
                R.id.issueRefundFragment,
                R.id.addOrderShipmentTrackingFragment,
                R.id.addOrderNoteFragment,
                R.id.printShippingLabelInfoFragment,
                R.id.shippingLabelFormatOptionsFragment -> {
                    true
                }
                R.id.productDetailFragment -> {
                    // show Cross icon only when product detail isn't opened from the product list
                    binding.bottomNav.currentPosition != PRODUCTS
                }
                else -> {
                    false
                }
            }
        }

        if (isAtRoot) {
            toolbar.navigationIcon = null
        } else if (showCrossIcon) {
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_gridicons_cross_24dp)
        } else {
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_back_24dp)
        }

        val isFullScreenFragment = destination.id == R.id.productImageViewerFragment ||
            destination.id == R.id.wpMediaViewerFragment

        supportActionBar?.let { actionBar ->
            // the image viewers should be shown full screen
            if (isFullScreenFragment) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                restoreToolbarHeight = binding.collapsingToolbar.layoutParams.height
                binding.collapsingToolbar.layoutParams.height = 0
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (restoreToolbarHeight > 0) {
                    binding.collapsingToolbar.layoutParams.height = restoreToolbarHeight
                    restoreToolbarHeight = 0
                }
            }
        }

        // show bottom nav if this is a dialog destination from root or, just root itself
        if (isTopLevelNavigation) {
            showBottomNav()
        } else {
            hideBottomNav()
        }

        previousDestinationId = destination.id
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        binding.collapsingToolbar.title = title
    }

    fun expandToolbar(expand: Boolean, animate: Boolean) {
        binding.appBarLayout.setExpanded(expand, animate)
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

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

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
                // restart the activity if the user returned from settings and they switched sites
                if (resultCode == AppSettingsActivity.RESULT_CODE_SITE_CHANGED) {
                    presenter.selectedSiteChanged(selectedSite.get())
                    restart()
                }

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

    override fun showLoginScreen() {
        selectedSite.reset()
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivityForResult(intent, RequestCodes.ADD_ACCOUNT)
        finish()
    }

    /**
     * displays the site picker activity and finishes this activity
     */
    override fun showSitePickerScreen() {
        SitePickerActivity.showSitePickerFromLogin(this)
        finish()
    }

    override fun showSettingsScreen() {
        AnalyticsTracker.track(Stat.MAIN_MENU_SETTINGS_TAPPED)
        val intent = Intent(this, AppSettingsActivity::class.java)
        startActivityForResult(intent, RequestCodes.SETTINGS)
    }

    override fun updateSelectedSite() {
        hideProgressDialog()

        if (!selectedSite.exists()) {
            showSitePickerScreen()
            return
        }

        // Complete UI initialization
        binding.bottomNav.init(navController, this)
        initFragment(null)
    }

    /**
     * Called when the user switches sites - restarts the activity so all fragments and child fragments are reset
     */
    private fun restart() {
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
        val host = uri?.host?.let { it } ?: ""
        return Intent.ACTION_VIEW == action && host.contains(MAGIC_LOGIN)
    }

    private fun getAuthTokenFromIntent(): String? {
        val uri = intent.data
        return uri?.getQueryParameter(TOKEN_PARAMETER)
    }

    // region Bottom Navigation
    override fun updateReviewsBadge() {
        if (AppPrefs.getHasUnseenReviews()) {
            showReviewsBadge()
        } else {
            hideReviewsBadge()
        }
    }

    override fun hideReviewsBadge() {
        binding.bottomNav.showReviewsBadge(false)
        NotificationHandler.removeAllReviewNotifsFromSystemBar(this)
    }

    override fun showReviewsBadge() {
        binding.bottomNav.showReviewsBadge(true)
    }

    override fun updateOrderBadge(hideCountUntilComplete: Boolean) {
        if (hideCountUntilComplete) {
            binding.bottomNav.clearOrderBadgeCount()
        }
        presenter.fetchUnfilledOrderCount()
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
            MY_STORE -> Stat.MAIN_TAB_DASHBOARD_SELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_SELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_SELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)

        if (navPos == REVIEWS) {
            NotificationHandler.removeAllReviewNotifsFromSystemBar(this)
        } else if (navPos == ORDERS) {
            NotificationHandler.removeAllOrderNotifsFromSystemBar(this)
        }
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> Stat.MAIN_TAB_DASHBOARD_RESELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_RESELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_RESELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
        }
        AnalyticsTracker.track(stat)

        // if we're at the root scroll the active fragment to the top, otherwise clear the nav backstack
        if (isAtNavigationRoot()) {
            // If the fragment's view is not yet created, do nothing
            if (getActiveTopLevelFragment()?.view != null) {
                getActiveTopLevelFragment()?.scrollToTop()
                expandToolbar(expand = true, animate = true)
            }
        } else {
            navController.navigate(binding.bottomNav.currentPosition.id)
        }
    }
    // endregion

    // region Fragment Processing
    private fun initFragment(savedInstanceState: Bundle?) {
        val openedFromPush = intent.getBooleanExtra(FIELD_OPENED_FROM_PUSH, false)

        if (savedInstanceState != null) {
            restoreSavedInstanceState(savedInstanceState)
        } else if (openedFromPush) {
            // Opened from a push notification
            //
            // Reset this flag now that it's being processed
            intent.removeExtra(FIELD_OPENED_FROM_PUSH)

            menu?.close()

            if (intent.getBooleanExtra(FIELD_OPENED_FROM_PUSH_GROUP, false)) {
                // Reset this flag now that it's being processed
                intent.removeExtra(FIELD_OPENED_FROM_PUSH_GROUP)

                // Send analytics for viewing all notifications
                NotificationHandler.bumpPushNotificationsTappedAllAnalytics(this)

                // Clear unread messages from the system bar
                NotificationHandler.removeAllNotificationsFromSystemBar(this)

                // User clicked on a group of notifications. Redirect to the order list screen if
                // the last notification received is a new order. Otherwise, redirect to the reviews screen
                val notificationChannelType = intent.getStringExtra(FIELD_NOTIFICATION_TYPE)?.let {
                    NotificationChannelType.valueOf(it.toUpperCase(Locale.US))
                } ?: NotificationChannelType.REVIEW

                binding.bottomNav.currentPosition = when (notificationChannelType) {
                    NotificationChannelType.NEW_ORDER -> ORDERS
                    else -> REVIEWS
                }
            } else if (intent.getBooleanExtra(FIELD_OPENED_FROM_ZENDESK, false)) {
                // Reset this flag now that it's being processed
                intent.removeExtra(FIELD_OPENED_FROM_ZENDESK)

                // Send track event for the zendesk notification id
                val remoteNoteId = intent.getIntExtra(FIELD_REMOTE_NOTE_ID, 0)
                NotificationHandler.bumpPushNotificationsTappedAnalytics(this, remoteNoteId.toString())

                // Remove single notification from the system bar
                NotificationHandler.removeNotificationWithNoteIdFromSystemBar(this, remoteNoteId.toString())

                // leave the Main activity showing the Dashboard tab, so when the user comes back from Help & Support,
                // the app is in the right section
                binding.bottomNav.currentPosition = MY_STORE

                // launch 'Tickets' page of Zendesk
                startActivity(HelpActivity.createIntent(this, Origin.ZENDESK_NOTIFICATION, null))
            } else {
                // Check for a notification ID - if one is present, open notification
                val remoteNoteId = intent.getLongExtra(FIELD_REMOTE_NOTE_ID, 0)
                if (remoteNoteId > 0) {
                    // Send track event
                    NotificationHandler.bumpPushNotificationsTappedAnalytics(this, remoteNoteId.toString())

                    // Remove single notification from the system bar
                    NotificationHandler.removeNotificationWithNoteIdFromSystemBar(this, remoteNoteId.toString())

                    showNotificationDetail(remoteNoteId)
                } else {
                    // Send analytics for viewing all notifications
                    NotificationHandler.bumpPushNotificationsTappedAllAnalytics(this)

                    // Clear unread messages from the system bar
                    NotificationHandler.removeAllNotificationsFromSystemBar(this)

                    // Just open the notifications tab
                    binding.bottomNav.currentPosition = REVIEWS
                }
            }
        } else {
            binding.bottomNav.currentPosition = MY_STORE
        }
    }
    // endregion

    override fun showNotificationDetail(remoteNoteId: Long) {
        showBottomNav()

        (presenter.getNotificationByRemoteNoteId(remoteNoteId))?.let { note ->
            when (note.getWooType()) {
                NEW_ORDER -> {
                    selectedSite.getIfExists()?.let { site ->
                        note.getRemoteOrderId()?.let { orderId ->
                            showOrderDetail(site.id, remoteOrderId = orderId, remoteNoteId = note.remoteNoteId)
                        }
                    }
                }
                PRODUCT_REVIEW -> showReviewDetail(
                    note.getCommentId(),
                    launchedFromNotification = true,
                    enableModeration = true
                )
                else -> { /* do nothing */
                }
            }
        }
    }

    override fun showProductDetail(remoteProductId: Long, remoteVariationId: Long, enableTrash: Boolean) {
        if (remoteVariationId == 0L) {
            val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
                remoteProductId,
                isTrashEnabled = enableTrash
            )
            navController.navigateSafely(action)
        } else {
            // the product nav graph has product detail as the start destination, so we have to change it to
            // the variation detail
            val navGraph = navController.navInflater.inflate(navigation.nav_graph_products)
            navGraph.startDestination = R.id.variationDetailFragment
            navController.setGraph(navGraph)

            val action = NavGraphMainDirections.actionGlobalProductVariationDetailFragment(
                remoteProductId = remoteProductId,
                remoteVariationId = remoteVariationId
            )
            navController.navigateSafely(action)
        }
    }

    override fun showAddProduct() {
        showBottomNav()
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(isAddProduct = true)
        navController.navigateSafely(action)
    }

    override fun showReviewDetail(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        tempStatus: String?
    ) {
        // make sure the review tab is active if the user came here from a notification
        if (launchedFromNotification) {
            showBottomNav()
            binding.bottomNav.currentPosition = REVIEWS
            binding.bottomNav.active(REVIEWS.position)
        }

        val action = ReviewListFragmentDirections.actionReviewListFragmentToReviewDetailFragment(
            remoteReviewId,
            tempStatus,
            launchedFromNotification,
            enableModeration
        )
        navController.navigateSafely(action)
    }

    override fun showProductFilters(stockStatus: String?, productType: String?, productStatus: String?) {
        val action = ProductListFragmentDirections.actionProductListFragmentToProductFilterListFragment(
            stockStatus, productStatus, productType
        )
        navController.navigateSafely(action)
    }

    override fun showProductAddBottomSheet() {
        val action = ProductListFragmentDirections.actionProductListFragmentToProductTypesBottomSheet(
            isAddProduct = true
        )
        navController.navigateSafely(action)
    }

    override fun showOrderDetail(
        localSiteId: Int,
        localOrderId: Int,
        remoteOrderId: Long,
        remoteNoteId: Long
    ) {
        if (binding.bottomNav.currentPosition != ORDERS) {
            binding.bottomNav.currentPosition = ORDERS
            val navPos = ORDERS.position
            binding.bottomNav.active(navPos)
        }

        val orderId = OrderIdentifier(localOrderId, localSiteId, remoteOrderId)
        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(orderId, remoteNoteId)
        navController.navigateSafely(action)
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
        if (isBottomNavShowing) {
            isBottomNavShowing = false
            WooAnimUtils.animateBottomBar(binding.bottomNav, false, Duration.MEDIUM)
        }
    }

    override fun showBottomNav() {
        if (!isBottomNavShowing) {
            isBottomNavShowing = true
            WooAnimUtils.animateBottomBar(binding.bottomNav, true, Duration.SHORT)
        }
    }

    /**
     * User tapped a button in WCPromoDialogFragment
     */
    override fun onPromoButtonClicked(promoButton: PromoButton) {
        when (promoButton) {
            PromoButton.SITE_PICKER_TRY_IT -> {
                WCPromoTooltip.setTooltipShown(this, Feature.SITE_SWITCHER, false)
                showSettingsScreen()
            }
            else -> {
            }
        }
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
}
