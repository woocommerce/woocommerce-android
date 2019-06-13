package com.woocommerce.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.SparseArray
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
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
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.notifications.NotifsListFragment
import com.woocommerce.android.ui.orders.OrderListFragment
import com.woocommerce.android.ui.prefs.AppSettingsActivity
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
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
        MainContract.View,
        HasSupportFragmentInjector,
        FragmentScrollListener,
        MainBottomNavigationView.MainBottomNavigationListener,
        WCPromoDialog.PromoDialogListener {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100
        private const val REQUEST_CODE_SETTINGS = 200

        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"

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
    private lateinit var bottomNavView: MainBottomNavigationView
    private lateinit var navController: NavController

    private val topLevelFragmentStates = SparseArray<Bundle>()

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var loginProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the toolbar
        setSupportActionBar(toolbar as Toolbar)

        presenter.takeView(this)
        bottomNavView = bottom_nav.also { it.init(this) }
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

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        getActiveTopLevelFragment()?.let { topFragment ->
            if (topFragment.isAdded) {
                if (topFragment.childFragmentManager.backStackEntryCount > 0) {
                    // go no further if active fragment doesn't allow back press - we use this so fragments can
                    // provide confirmation before discarding the current action, such as adding an order note
                    val fragment = topFragment.childFragmentManager.findFragmentById(R.id.container)
                    if (fragment is BackPressListener && !fragment.onRequestAllowBackPress()) {
                        return
                    }
                    topFragment.childFragmentManager.popBackStack()
                } else if (bottomNavView.selectedItemId != R.id.dashboardFragment) {
                    // no child fragments, so navigate to the dashboard
                    navigateToTopLevelDestination(R.id.dashboardFragment)
                } else {
                    //  we're already on the dashboard so finish the app
                    finish()
                }
                return
            }
        }

        super.onBackPressed()
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
            // TODO Launch next screen
        }
    }

    override fun showLoginScreen() {
        selectedSite.reset()
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
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
        bottomNavView.init(this)
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

    /**
     * Navigates to one of the top level fragments and enables us to save/restore state
     * since (annoyingly) the navigation component doesn't do that for bottom nav fragments
     */
    private fun navigateToTopLevelDestination(@IdRes destId: Int, args: Bundle? = null) {
        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        navController.currentDestination?.let { destination ->
            // do nothing if this is already the current destination
            if (destination.id == destId) {
                return
            }

            getActiveTopLevelFragment()?.let { fragmentOut ->
                // save the state of the outgoing fragment
                val stateOut = Bundle()
                fragmentOut.onSaveInstanceState(stateOut)
                topLevelFragmentStates.put(destination.id, stateOut)

                // remove any child fragments of the outgoing fragment
                fragmentOut.childFragmentManager.popBackStack()
            }
        }

        // restore incoming state combined with passed arguments
        val stateIn = topLevelFragmentStates.get(destId) ?: Bundle()
        args?.let {
            stateIn.putAll(it)
        }

        // navigate to the destination and make sure the correct bottom nav item is selected
        navController.navigate(destId, stateIn)
        bottomNavView.ensureSelectedItemId(destId)

        // remove the badge if this is the notification list
        if (destId == R.id.notifsListFragment) {
            showNotificationBadge(false)
        }
    }

    override fun onBottomNavItemSelected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            BottomNavigationPosition.DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_SELECTED
            BottomNavigationPosition.ORDERS -> Stat.MAIN_TAB_ORDERS_SELECTED
            BottomNavigationPosition.NOTIFICATIONS -> Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)
        navigateToTopLevelDestination(navPos.id)
    }

    override fun onBottomNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            BottomNavigationPosition.DASHBOARD -> Stat.MAIN_TAB_DASHBOARD_RESELECTED
            BottomNavigationPosition.ORDERS -> Stat.MAIN_TAB_ORDERS_RESELECTED
            BottomNavigationPosition.NOTIFICATIONS -> Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
        }
        AnalyticsTracker.track(stat)

        // if the user reselected a bottom nav item while a child fragment is showing, clear the back stack
        // so they see the top level fragment, otherwise scroll the top level fragment to the top
        (getActiveTopLevelFragment() as? TopLevelFragment)?.let { topFragment ->
            if (topFragment.childFragmentManager.backStackEntryCount > 0) {
                topFragment.childFragmentManager.popBackStack()
            } else {
                topFragment.scrollToTop()
            }
        }
    }
    // endregion

    // region Fragment Processing
    private fun initFragment(savedInstanceState: Bundle?) {
        val openedFromPush = intent.getBooleanExtra(FIELD_OPENED_FROM_PUSH, false)

        if (savedInstanceState == null) {
            if (openedFromPush) {
                // Opened from a push notificaton
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
                    navigateToTopLevelDestination(R.id.notifsListFragment)
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
                        navigateToTopLevelDestination(R.id.notifsListFragment)
                    }
                }
            } else {
                navigateToTopLevelDestination(R.id.dashboardFragment)
            }
        }
    }
    // endregion

    // TODO: this is a hack that will be dropped once we fully switch over to the navigation component
    private fun getActiveTopLevelFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main)
        return navHostFragment?.getChildFragmentManager()?.getFragments()?.get(0)
    }

    override fun showOrderList(orderStatusFilter: String?) {
        showBottomNav()
        val args = Bundle().also { it.putString(OrderListFragment.ARG_ORDER_STATUS_FILTER, orderStatusFilter) }
        navigateToTopLevelDestination(R.id.orderListFragment, args)
    }

    override fun showNotificationDetail(remoteNoteId: Long) {
        showBottomNav()
        navigateToTopLevelDestination(R.id.notifsListFragment)

        (getActiveTopLevelFragment() as? NotifsListFragment)?.let { fragment ->
            val navPos = BottomNavigationPosition.NOTIFICATIONS.position
            bottom_nav.active(navPos)

            (presenter.getNotificationByRemoteNoteId(remoteNoteId))?.let { notif ->
                when (notif.getWooType()) {
                    NEW_ORDER -> {
                        notif.getRemoteOrderId()?.let { orderId ->
                            fragment.openOrderDetail(notif.localSiteId, orderId, notif.remoteNoteId)
                        }
                    }
                    PRODUCT_REVIEW -> fragment.openReviewDetail(notif)
                    else -> { /* do nothing */
                    }
                }
            }
        }
    }

    override fun showProductDetail(remoteProductId: Long) {
        showBottomNav()
        (getActiveTopLevelFragment() as? TopLevelFragment)?.openProductDetail(remoteProductId)
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
