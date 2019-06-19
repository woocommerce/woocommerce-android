package com.woocommerce.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.DrawableRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.findNavController
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.FragmentScrollListener
import com.woocommerce.android.extensions.WooNotificationType.NEW_ORDER
import com.woocommerce.android.extensions.WooNotificationType.PRODUCT_REVIEW
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.getRemoteOrderId
import com.woocommerce.android.extensions.getWooType
import com.woocommerce.android.push.NotificationHandler
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.dashboard.DashboardFragment
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.DASHBOARD
import com.woocommerce.android.ui.main.BottomNavigationPosition.NOTIFICATIONS
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.ui.notifications.NotifsListFragment
import com.woocommerce.android.ui.orders.OrderDetailFragmentDirections
import com.woocommerce.android.ui.orders.OrderListFragment
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.products.ProductDetailFragmentDirections
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
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
        MainContract.View,
        HasSupportFragmentInjector,
        FragmentScrollListener,
        MainNavigationRouter,
        MainBottomNavigationView.MainNavigationListener,
        NavController.OnDestinationChangedListener,
        WCPromoDialog.PromoDialogListener {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100
        private const val REQUEST_CODE_SETTINGS = 200

        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"
        private const val STATE_KEY_POSITION = "key-position"

        // push notification-related constants
        const val FIELD_OPENED_FROM_PUSH = "opened-from-push-notification"
        const val FIELD_REMOTE_NOTE_ID = "remote-note-id"
        const val FIELD_OPENED_FROM_PUSH_GROUP = "opened-from-push-group"

        interface BackPressListener {
            fun onRequestAllowBackPress(): Boolean
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<androidx.fragment.app.Fragment>
    @Inject lateinit var presenter: MainContract.Presenter
    @Inject lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject lateinit var selectedSite: SelectedSite

    private var isBottomNavShowing = true
    private var previousDestinationId: Int? = null
    private lateinit var bottomNavView: MainBottomNavigationView
    private lateinit var navController: NavController

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var loginProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the toolbar
        setSupportActionBar(toolbar as Toolbar)

        presenter.takeView(this)
        bottomNavView = bottom_nav.also { it.init(supportFragmentManager, this) }

        navController = findNavController(R.id.nav_host_fragment_main)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            if (hasMagicLinkLoginIntent()) {
                // User has opened a magic link
                // Trigger an account/site info fetch, and show a 'logging in...' dialog in the meantime
                loginProgressDialog = ProgressDialog.show(this, "", getString(R.string.logging_in), true)
                getAuthTokenFromIntent()?.let { presenter.storeMagicLinkToken(it) }
            } else {
                showLoginScreen()
            }
            return
        }

        if (!selectedSite.exists()) {
            updateSelectedSite()
            return
        }

        initFragment(savedInstanceState)

        // show the app rating dialog if it's time
        AppRatingDialog.showIfNeeded(this)

        // it's important to set this listener last, otherwise if the activity is recreated the listener will fire
        // before we've had a chance to initialize the bottom nav fragments (resulting in a crash)
        navController.addOnDestinationChangedListener(this)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
        updateNotificationBadge()

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

    override fun onSaveInstanceState(outState: Bundle?) {
        // Store the current bottom bar navigation position.
        outState?.putInt(STATE_KEY_POSITION, bottomNavView.currentPosition.id)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        // Restore the current navigation position
        savedInstanceState?.also {
            val id = it.getInt(STATE_KEY_POSITION, BottomNavigationPosition.DASHBOARD.id)
            bottomNavView.restoreSelectedItemState(id)
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

        super.onBackPressed()
    }

    /**
     * Returns true if the navigation controller is showing the root fragment (ie: a top level fragment is showing)
     */
    override fun isAtNavigationRoot(): Boolean = navController.currentDestination?.id == R.id.rootFragment

    /**
     * Return true if one of the nav component fragments is showing (the opposite of the above)
     */
    override fun isChildFragmentShowing() = !isAtNavigationRoot()

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
    private fun getActiveTopLevelFragment(): TopLevelFragment? {
        val tag = when (bottomNavView.currentPosition) {
            DASHBOARD -> DashboardFragment.TAG
            ORDERS -> OrderListFragment.TAG
            NOTIFICATIONS -> NotifsListFragment.TAG
        }
        return supportFragmentManager.findFragmentByTag(tag) as? TopLevelFragment
    }

    /**
     * Returns the fragment currently shown by the navigation component, or null if we're at the root
     */
    private fun getActiveChildFragment(): Fragment? {
        return if (isChildFragmentShowing()) {
            val navHostFragment = supportFragmentManager.primaryNavigationFragment
            navHostFragment?.childFragmentManager?.fragments?.get(0)
        } else {
            null
        }
    }

    /**
     * The current fragment in the nav controller has changed
     */
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val isAtRoot = isAtNavigationRoot()

        // go no further if this is the initial navigation to the root fragment
        if (isAtRoot && previousDestinationId == null) {
            previousDestinationId = destination.id
            return
        }

        // show/hide the top level fragment container depending on whether we're at the root
        if (isAtRoot) {
            container.visibility = View.VISIBLE
        } else {
            container.visibility = View.INVISIBLE
        }

        // make sure the correct up icon appears
        val showUpIcon: Boolean
        val showCrossIcon: Boolean
        val showBottomNav: Boolean
        if (isAtRoot) {
            showUpIcon = false
            showCrossIcon = false
            showBottomNav = true
        } else {
            showUpIcon = true
            showCrossIcon = when (destination.id) {
                R.id.productDetailFragment,
                R.id.addOrderShipmentTrackingFragment,
                R.id.addOrderNoteFragment -> {
                    true
                }
                else -> {
                    false
                }
            }
            showBottomNav = when (destination.id) {
                R.id.addOrderShipmentTrackingFragment,
                R.id.addOrderNoteFragment -> {
                    false
                }
                else -> {
                    true
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
        }

        if (showBottomNav) {
            showBottomNav()
        } else {
            hideBottomNav()
        }

        if (isAtRoot) {
            getActiveTopLevelFragment()?.onReturnedFromChildFragment()
        }

        previousDestinationId = destination.id
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item!!.itemId) {
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
            R.id.menu_support -> {
                showHelpAndSupport()
                AnalyticsTracker.track(Stat.MAIN_MENU_CONTACT_SUPPORT_TAPPED)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun supportFragmentInjector(): AndroidInjector<androidx.fragment.app.Fragment> = fragmentInjector

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ADD_ACCOUNT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO Launch next screen
                }
                return
            }
            REQUEST_CODE_SETTINGS -> {
                // restart the activity if the user returned from settings and they switched sites
                if (resultCode == AppSettingsActivity.RESULT_CODE_SITE_CHANGED) {
                    presenter.selectedSiteChanged(selectedSite.get())
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
        startActivityForResult(intent, REQUEST_CODE_ADD_ACCOUNT)
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
        startActivityForResult(intent, REQUEST_CODE_SETTINGS)
    }

    override fun showHelpAndSupport() {
        startActivity(HelpActivity.createIntent(this, Origin.MAIN_ACTIVITY, null))
    }

    override fun updateSelectedSite() {
        loginProgressDialog?.apply { if (isShowing) { cancel() } }

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
        val host = if (uri != null && uri.host != null) uri.host else ""
        return Intent.ACTION_VIEW == action && host.contains(MAGIC_LOGIN)
    }

    private fun getAuthTokenFromIntent(): String? {
        val uri = intent.data
        return uri?.getQueryParameter(TOKEN_PARAMETER)
    }

    // region Bottom Navigation
    override fun updateNotificationBadge() {
        showNotificationBadge(AppPrefs.getHasUnseenNotifs())
    }

    override fun showNotificationBadge(show: Boolean) {
        bottomNavView.showNotificationBadge(show)

        if (!show) {
            NotificationHandler.removeAllNotificationsFromSystemBar(this)
        }
    }

    override fun onNavItemSelected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_SELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_SELECTED
            NOTIFICATIONS -> Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)

        // if were not at the root, clear the nav controller's backstack
        if (!isAtNavigationRoot()) {
            navigateToRoot()
        }

        // Update the unseen notifications badge visibility
        if (navPos == NOTIFICATIONS) {
            NotificationHandler.removeAllNotificationsFromSystemBar(this)
        }
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_RESELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_RESELECTED
            NOTIFICATIONS -> Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
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

                // User clicked on a group of notifications. Just show the notifications tab.
                bottomNavView.currentPosition = NOTIFICATIONS
            } else {
                // Check for a notification ID - if one is present, open notification
                val remoteNoteId = intent.getLongExtra(FIELD_REMOTE_NOTE_ID, 0)
                if (remoteNoteId > 0) {
                    // Send track event
                    NotificationHandler.bumpPushNotificationsTappedAnalytics(this, remoteNoteId.toString())

                    // Remove single notification from the system bar
                    NotificationHandler.removeNotificationWithNoteIdFromSystemBar(this, remoteNoteId.toString())

                    // Open the detail view for this notification
                    showNotificationDetail(remoteNoteId)
                } else {
                    // Send analytics for viewing all notifications
                    NotificationHandler.bumpPushNotificationsTappedAllAnalytics(this)

                    // Clear unread messages from the system bar
                    NotificationHandler.removeAllNotificationsFromSystemBar(this)

                    // Just open the notifications tab
                    bottomNavView.currentPosition = NOTIFICATIONS
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
        bottomNavView.currentPosition = NOTIFICATIONS

        val fragment = bottomNavView.getFragment(NOTIFICATIONS)
        val navPos = BottomNavigationPosition.NOTIFICATIONS.position
        bottom_nav.active(navPos)

        (presenter.getNotificationByRemoteNoteId(remoteNoteId))?.let {
            when (it.getWooType()) {
                NEW_ORDER -> {
                    it.getRemoteOrderId()?.let { orderId ->
                        showOrderDetail(it.localSiteId, orderId, it.remoteNoteId)
                    }
                }
                PRODUCT_REVIEW -> (fragment as? NotifsListFragment)?.openReviewDetail(it)
                else -> { /* do nothing */ }
            }
        }
    }

    override fun showProductDetail(remoteProductId: Long) {
        showBottomNav()
        val action = ProductDetailFragmentDirections.actionGlobalProductDetailFragment(remoteProductId)
        navController.navigate(action)
    }

    override fun showOrderDetail(localSiteId: Int, remoteOrderId: Long, remoteNoteId: Long, markComplete: Boolean) {
        // if we're marking the order as complete, we need to inclusively pop the backstack to the existing order
        // detail fragment and then show a new one
        if (markComplete) {
            navController.popBackStack(R.id.orderDetailFragment, true)
        }
        val orderId = OrderIdentifier(localSiteId, remoteOrderId)
        val action = OrderDetailFragmentDirections.actionGlobalOrderDetailFragment(orderId, remoteNoteId, markComplete)
        navController.navigate(action)
    }

    override fun updateOfflineStatusBar(isConnected: Boolean) {
        if (isConnected) offline_bar.hide() else offline_bar.show()
    }

    private fun checkConnection() {
        updateOfflineStatusBar(NetworkUtils.isNetworkAvailable(this))
    }

    override fun onFragmentScrollUp() {
        showBottomNav()
    }

    override fun onFragmentScrollDown() {
        hideBottomNav()
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
            } else -> {}
        }
    }
}
