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
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.NavHostFragment
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.getCommentId
import com.woocommerce.android.extensions.getRemoteOrderId
import com.woocommerce.android.extensions.getWooType
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.push.NotificationHandler.NotificationChannelType
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dashboard.DashboardFragment
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.DASHBOARD
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.ui.main.BottomNavigationPosition.PRODUCTS
import com.woocommerce.android.ui.main.BottomNavigationPosition.REVIEWS
import com.woocommerce.android.ui.mystore.MyStoreFragment
import com.woocommerce.android.ui.mystore.RevenueStatsAvailabilityFetcher
import com.woocommerce.android.ui.orders.details.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.list.OrderListFragment
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.reviews.ReviewDetailFragmentDirections
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.WCPromoDialog
import com.woocommerce.android.widgets.WCPromoDialog.PromoButton
import com.woocommerce.android.widgets.WCPromoTooltip
import com.woocommerce.android.widgets.WCPromoTooltip.Feature
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import kotlinx.android.synthetic.main.activity_main.*
import org.wordpress.android.fluxc.model.SiteModel
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

    interface NavigationResult {
        fun onNavigationResult(requestCode: Int, result: Bundle)
    }

    @Inject lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: MainContract.Presenter
    @Inject lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var uiMessageResolver: UIMessageResolver
    @Inject lateinit var revenueStatsAvailabilityFetcher: RevenueStatsAvailabilityFetcher

    private var isBottomNavShowing = true
    private var previousDestinationId: Int? = null
    private var unfilledOrderCount: Int = 0
    private var isMainThemeApplied = false

    private lateinit var bottomNavView: MainBottomNavigationView
    private lateinit var navController: NavController

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var progressDialog: ProgressDialog? = null

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

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        // Set the toolbar
        setSupportActionBar(toolbar as Toolbar)

        presenter.takeView(this)

        bottomNavView = bottom_nav.also { it.init(supportFragmentManager, this) }

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this)

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

        // we only have to check the new revenue stats availability
        // if the activity is starting for the first time
        if (savedInstanceState == null) {
            fetchRevenueStatsAvailability(selectedSite.get())
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // don't show the options menu unless we're at the root
        if (isAtNavigationRoot()) {
            menuInflater.inflate(R.menu.menu_action_bar, menu)
            return true
        } else {
            return false
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu?): Boolean {
        // settings icon only appears on the dashboard
        menu?.findItem(R.id.menu_settings)?.isVisible = bottomNavView.currentPosition == DASHBOARD
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        updateReviewsBadge()
        updateOrderBadge(false)

        checkConnection()
    }

    override fun onPause() {
        super.onPause()
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
        outState.putInt(KEY_BOTTOM_NAV_POSITION, bottomNavView.currentPosition.id)
        outState.putInt(KEY_UNFILLED_ORDER_COUNT, unfilledOrderCount)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.also {
            val id = it.getInt(KEY_BOTTOM_NAV_POSITION, DASHBOARD.id)
            bottomNavView.restoreSelectedItemState(id)

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
        }

        // if we're not on the dashboard make it active, otherwise allow the OS to leave the app
        if (bottomNavView.currentPosition != DASHBOARD) {
            bottomNavView.currentPosition = DASHBOARD
        } else {
            super.onBackPressed()
        }
    }

    /**
     * Returns true if the navigation controller is showing the root fragment (ie: a top level fragment is showing)
     */
    override fun isAtNavigationRoot(): Boolean {
        return if (::navController.isInitialized) {
            navController.currentDestination?.id == R.id.rootFragment
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
     * Navigates to the root fragment so only the top level fragment is showing
     */
    private fun navigateToRoot() {
        if (!isAtNavigationRoot()) {
            navController.popBackStack(R.id.rootFragment, false)
        }
    }

    /**
     * Returns the current top level fragment (ie: the one showing in the bottom nav)
     */
    internal fun getActiveTopLevelFragment(): TopLevelFragment? {
        val tag = bottomNavView.currentPosition.getTag()
        return supportFragmentManager.findFragmentByTag(tag) as? TopLevelFragment
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

    /***
     * Get the actual primary navigation Fragment from the support manager
     */
    private fun getHostChildFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.primaryNavigationFragment
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    /**
     * The current fragment in the nav controller has changed
     */
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val isAtRoot = isAtNavigationRoot()
        val isTopLevelNavigation = isAtTopLevelNavigation(isAtRoot = isAtRoot, destination = destination)

        // go no further if this is the initial navigation to the root fragment
        if (isAtRoot && previousDestinationId == null) {
            previousDestinationId = destination.id
            return
        }

        // show/hide the top level fragment container if this is a dialog destination from root or, just root itself
        if (isTopLevelNavigation) {
            container.visibility = View.VISIBLE
        } else {
            container.visibility = View.INVISIBLE
        }

        val showUpIcon: Boolean
        val showCrossIcon: Boolean
        if (isTopLevelNavigation) {
            showUpIcon = false
            showCrossIcon = false
        } else {
            showUpIcon = true
            showCrossIcon = when (destination.id) {
                R.id.productFilterListFragment,
                R.id.productShippingClassFragment,
                R.id.issueRefundFragment,
                R.id.addOrderShipmentTrackingFragment,
                R.id.addOrderNoteFragment,
                R.id.productSettingsFragment,
                R.id.addProductCategoryFragment,
                R.id.parentCategoryListFragment,
                R.id.productSelectionListFragment,
                R.id.printShippingLabelInfoFragment,
                R.id.shippingLabelFormatOptionsFragment -> {
                    true
                }
                R.id.productDetailFragment -> {
                    // show Cross icon only when product detail isn't opened from the product list
                    bottomNavView.currentPosition != PRODUCTS
                }
                else -> {
                    false
                }
            }
        }
        supportActionBar?.let { actionBar ->
            actionBar.setDisplayHomeAsUpEnabled(showUpIcon)
            @DrawableRes val icon = if (showCrossIcon) {
                R.drawable.ic_gridicons_cross_white_24dp
            } else {
                R.drawable.ic_back_white_24dp
            }
            actionBar.setHomeAsUpIndicator(icon)

            // the image viewers should be shown full screen
            if (destination.id == R.id.productImageViewerFragment || destination.id == R.id.wpMediaViewerFragment) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                actionBar.hide()
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                actionBar.show()
            }
        }

        // show bottom nav if this is a dialog destination from root or, just root itself
        if (isTopLevelNavigation) {
            showBottomNav()
        } else {
            hideBottomNav()
        }

        getActiveTopLevelFragment()?.let {
            if (isTopLevelNavigation) {
                it.updateActivityTitle()
                it.onReturnedFromChildFragment()
            } else {
                it.onChildFragmentOpened()
            }
        }

        previousDestinationId = destination.id
    }

    /**
     * Returns a Boolean value in order to set the behaviour from a root navigation type in terms of:
     * .container visibility
     * .menu items visibility
     * .top nav bar titles
     *
     * @param isAtRoot The value that tells if root fragment is in the current destination
     * @param destination The object for the next navigation destination
     */

    private fun isAtTopLevelNavigation(isAtRoot: Boolean, destination: NavDestination): Boolean {
        val isDialogDestination = destination.navigatorName == DIALOG_NAVIGATOR_NAME
        val activeChild = getHostChildFragment()
        val activeChildIsRoot = activeChild != null && activeChild is RootFragment
        return (isDialogDestination && activeChildIsRoot) || isAtRoot
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // User clicked the "up" button in the action bar
            android.R.id.home -> {
                onBackPressed()
                true
            }
            // User selected the settings menu option
            R.id.menu_settings -> {
                showSettingsScreen()
                AnalyticsTracker.track(Stat.MAIN_MENU_SETTINGS_TAPPED)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

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

        bottomNavView.init(supportFragmentManager, this)
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
        bottomNavView.showReviewsBadge(false)
        NotificationHandler.removeAllReviewNotifsFromSystemBar(this)
    }

    override fun showReviewsBadge() {
        bottomNavView.showReviewsBadge(true)
    }

    override fun updateOrderBadge(hideCountUntilComplete: Boolean) {
        if (hideCountUntilComplete) {
            bottomNavView.clearOrderBadgeCount()
        }
        presenter.fetchUnfilledOrderCount()
    }

    override fun showOrderBadge(count: Int) {
        unfilledOrderCount = count
        bottomNavView.setOrderBadgeCount(count)
    }

    override fun hideOrderBadge() {
        unfilledOrderCount = 0
        bottomNavView.setOrderBadgeCount(0)
    }

    override fun fetchRevenueStatsAvailability(site: SiteModel) {
        revenueStatsAvailabilityFetcher.fetchRevenueStatsAvailability(site)
    }

    /**
     * Method to update the `My Store` TAB based on the revenue stats availability
     *
     * if revenue stats v4 support is available but we are currently displaying the v3 stats UI,
     * display a banner to the user with the option to unload the v3 UI and display the new stats UI
     *
     * if revenue stats v4 support is NOT available but we are currently displaying the v4 stats UI,
     * display a banner [com.woocommerce.android.ui.mystore.MyStoreStatsRevertedNoticeCard]
     * to the user after unloading the v4 UI and displaying the old stats UI
     */
    override fun updateStatsView(isAvailable: Boolean) {
        val fragment = bottomNavView.getFragment(DASHBOARD)
        when (fragment.tag) {
            DashboardFragment.TAG -> {
                if (isAvailable) {
                    // display the new stats UI only if user has opted in
                    replaceStatsFragment()
                }
            }
            MyStoreFragment.TAG -> {
                // if new stats UI was enabled but is no longer enabled, display revert banner
                // and replace the new stats UI with the old UI
                if (!isAvailable) {
                    AppPrefs.setShouldDisplayV4StatsRevertedBanner(true)
                    replaceStatsFragment()
                }
            }
        }
    }

    override fun replaceStatsFragment() {
        bottomNavView.replaceStatsFragment()
    }

    override fun onNavItemSelected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_SELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_SELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_SELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)

        // if were not at the root, clear the nav controller's backstack
        if (!isAtNavigationRoot()) {
            navigateToRoot()
        }

        if (navPos == REVIEWS) {
            NotificationHandler.removeAllReviewNotifsFromSystemBar(this)
        } else if (navPos == ORDERS) {
            NotificationHandler.removeAllOrderNotifsFromSystemBar(this)
        }
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_RESELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_RESELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_RESELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
        }
        AnalyticsTracker.track(stat)

        // if we're at the root scroll the active fragment to the top, otherwise clear the nav backstack
        if (isAtNavigationRoot()) {
            getActiveTopLevelFragment()?.scrollToTop()
        } else {
            navigateToRoot()
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

                bottomNavView.currentPosition = when (notificationChannelType) {
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
                bottomNavView.currentPosition = DASHBOARD

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
                    bottomNavView.currentPosition = REVIEWS
                }
            }
        } else {
            bottomNavView.currentPosition = DASHBOARD
        }
    }
    // endregion

    override fun showOrderList(orderStatusFilter: String?) {
        showBottomNav()
        bottomNavView.updatePositionAndDeferInit(ORDERS)

        val fragment = bottomNavView.getFragment(ORDERS)
        (fragment as OrderListFragment).onOrderStatusSelected(orderStatusFilter)
    }

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
                PRODUCT_REVIEW -> showReviewDetail(note.getCommentId(), true)
                else -> { /* do nothing */
                }
            }
        }
    }

    override fun showProductDetail(remoteProductId: Long, enableTrash: Boolean) {
        showBottomNav()
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            remoteProductId,
            isTrashEnabled = enableTrash
        )
        navController.navigateSafely(action)
    }

    override fun showAddProduct() {
        showBottomNav()
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(isAddProduct = true)
        navController.navigateSafely(action)
    }

    override fun showReviewDetail(remoteReviewId: Long, launchedFromNotification: Boolean, tempStatus: String?) {
        showBottomNav()
        bottomNavView.currentPosition = REVIEWS

        val navPos = REVIEWS.position
        bottom_nav.active(navPos)

        val action = ReviewDetailFragmentDirections.actionGlobalReviewDetailFragment(
            remoteReviewId,
            tempStatus,
            launchedFromNotification
        )
        navController.navigateSafely(action)
    }

    override fun showProductFilters(stockStatus: String?, productType: String?, productStatus: String?) {
        val action = NavGraphMainDirections.actionGlobalProductFilterListFragment(
            stockStatus, productStatus, productType
        )
        navController.navigateSafely(action)
    }

    override fun showProductAddBottomSheet() {
        val action = NavGraphMainDirections.actionGlobalProductTypeBottomSheetFragment(isAddProduct = true)
        navController.navigateSafely(action)
    }

    override fun showOrderDetail(
        localSiteId: Int,
        localOrderId: Int,
        remoteOrderId: Long,
        remoteNoteId: Long,
        markComplete: Boolean
    ) {
        if (bottomNavView.currentPosition != ORDERS) {
            bottomNavView.currentPosition = ORDERS
            val navPos = ORDERS.position
            bottom_nav.active(navPos)
        }

        if (markComplete) {
            // if we're marking the order as complete, we need to inclusively pop the backstack to the existing order
            // detail fragment and then show a new one
            navController.popBackStack(R.id.orderDetailFragment, true)

            // immediately update the order badge to reflect the change
            if (unfilledOrderCount > 0) {
                showOrderBadge(unfilledOrderCount - 1)
            }
        }

        val orderId = OrderIdentifier(localOrderId, localSiteId, remoteOrderId)
        val action = OrderDetailFragmentDirections.actionGlobalOrderDetailFragment(orderId, remoteNoteId, markComplete)
        navController.navigateSafely(action)
    }

    override fun showFeedbackSurvey() {
        NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.MAIN).apply {
            navController.navigateSafely(this)
        }
    }

    override fun updateOfflineStatusBar(isConnected: Boolean) {
        if (isConnected) offline_bar.hide() else offline_bar.show()
    }

    private fun checkConnection() {
        updateOfflineStatusBar(NetworkUtils.isNetworkAvailable(this))
    }

    override fun hideBottomNav() {
        if (isBottomNavShowing) {
            isBottomNavShowing = false
            WooAnimUtils.animateBottomBar(bottom_nav, false, Duration.MEDIUM)
        }
    }

    override fun showBottomNav() {
        if (!isBottomNavShowing) {
            isBottomNavShowing = true
            WooAnimUtils.animateBottomBar(bottom_nav, true, Duration.SHORT)
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
