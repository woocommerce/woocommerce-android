package com.woocommerce.android.ui.main

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.woocommerce.android.BuildConfig
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.extensions.active
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.support.SupportHelper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.TopLevelFragment.FragmentScrollListener
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEpilogueActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.ORDERS
import com.woocommerce.android.ui.orders.OrderListFragment
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
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
        BottomNavigationView.OnNavigationItemSelectedListener,
        BottomNavigationView.OnNavigationItemReselectedListener {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100
        private const val REQUEST_CODE_SETTINGS = 200

        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"
        private const val STATE_KEY_POSITION = "key-position"

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenter: MainContract.Presenter
    @Inject lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var supportHelper: SupportHelper

    private var activeNavPosition: BottomNavigationPosition = BottomNavigationPosition.DASHBOARD
    private var isBottomNavShowing = false

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var loginProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        restoreSavedInstanceState(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the toolbar
        setSupportActionBar(toolbar as Toolbar)

        presenter.takeView(this)

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

        setupBottomNavigation()
        initFragment(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        if (!ActivityUtils.isEmailClientAvailable(this)) {
            menu?.removeItem(R.id.menu_support)
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        checkConnection()
    }

    public override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        // Store the current bottom bar navigation position.
        outState?.putInt(STATE_KEY_POSITION, activeNavPosition.id)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        // Restore the current navigation position
        savedInstanceState?.also {
            val id = it.getInt(STATE_KEY_POSITION, BottomNavigationPosition.DASHBOARD.id)
            activeNavPosition = findNavigationPositionById(id)
        }
    }

    /**
     * Send the onBackPressed request to the current active fragment to pop any
     * child fragments it may have on its back stack.
     *
     * Currently prevents the user from hitting back and exiting the app.
     */
    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        val fragment = supportFragmentManager.findFragmentByTag(activeNavPosition.getTag())
        if (!fragment.childFragmentManager.popBackStackImmediate()) {
            super.onBackPressed()
        }
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
                contactSupport()
                AnalyticsTracker.track(Stat.MAIN_MENU_CONTACT_SUPPORT_TAPPED)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_CODE_ADD_ACCOUNT -> {
                if (resultCode == Activity.RESULT_OK) {
                    // TODO Launch next screen
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
     * displays the login epilogue activity which enables choosing a site
     */
    override fun showLoginEpilogueScreen() {
        val intent = Intent(this, LoginEpilogueActivity::class.java)
        startActivity(intent)
        finish()
    }

    override fun showSettingsScreen() {
        val intent = Intent(this, AppSettingsActivity::class.java)
        startActivityForResult(intent, REQUEST_CODE_SETTINGS)
    }

    override fun contactSupport() {
        // TODO: only use Zendesk in internal debug builds - this will change once Zendesk integration is completed
        if (BuildConfig.DEBUG) {
            startActivity(HelpActivity.createIntent(this, Origin.MAIN_ACTIVITY, null))
        } else {
            supportHelper.emailSupport(this)
        }
    }

    override fun updateSelectedSite() {
        loginProgressDialog?.apply { if (isShowing) { cancel() } }

        if (!selectedSite.exists()) {
            showLoginEpilogueScreen()
            return
        }

        // Complete UI initialization
        setupBottomNavigation()
        initFragment(null)
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
    private fun setupBottomNavigation() {
        bottom_nav.active(activeNavPosition.position)
        bottom_nav.setOnNavigationItemSelectedListener(this)
        bottom_nav.setOnNavigationItemReselectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPosition = findNavigationPositionById(item.itemId)

        val stat = when (navPosition) {
            BottomNavigationPosition.DASHBOARD -> AnalyticsTracker.Stat.MAIN_TAB_DASHBOARD_SELECTED
            BottomNavigationPosition.ORDERS -> AnalyticsTracker.Stat.MAIN_TAB_ORDERS_SELECTED
            BottomNavigationPosition.NOTIFICATIONS -> AnalyticsTracker.Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)

        return switchFragment(navPosition)
    }

    /**
     * when a bottom nav item is reselected we clear the active fragment's backstack,
     * or if there is no backstack we scroll the fragment to the top
     */
    override fun onNavigationItemReselected(item: MenuItem) {
        val activeFragment = supportFragmentManager.findFragmentByTag(activeNavPosition.getTag())
        if (!clearFragmentBackStack(activeFragment)) {
            (activeFragment as? TopLevelFragment)?.scrollToTop()
        }

        val stat = when (activeNavPosition) {
            BottomNavigationPosition.DASHBOARD -> AnalyticsTracker.Stat.MAIN_TAB_DASHBOARD_RESELECTED
            BottomNavigationPosition.ORDERS -> AnalyticsTracker.Stat.MAIN_TAB_ORDERS_RESELECTED
            BottomNavigationPosition.NOTIFICATIONS -> AnalyticsTracker.Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
        }
        AnalyticsTracker.track(stat)
    }

    // endregion

    // region Fragment Processing
    private fun initFragment(savedInstanceState: Bundle?) {
        savedInstanceState ?: switchFragment(BottomNavigationPosition.DASHBOARD)
    }

    /**
     * Extension function for retrieving an existing fragment from the [FragmentManager]
     * if one exists, if not, create a new instance of the requested fragment.
     */
    private fun FragmentManager.findFragment(position: BottomNavigationPosition): TopLevelFragment? {
        return (findFragmentByTag(position.getTag()) ?: position.createFragment()) as? TopLevelFragment
    }

    /**
     * If the user clicked on the already displayed top-level option, pop any child
     * fragments and resets to the parent fragment.
     *
     * If the user selected an option not currently active, pop any child fragments,
     * hide the current top-level fragment and add/show the destination top-level fragment.
     *
     * Immediately execute transactions with FragmentManager#executePendingTransactions.
     *
     * @param navPosition The [BottomNavigationPosition] to activate
     * @param deferInit If true, the [TopLevelFragment] may use this variable to defer a part of its
     * normal initialization until manually requested.
     */
    private fun switchFragment(navPosition: BottomNavigationPosition, deferInit: Boolean = false): Boolean {
        val activeFragment = supportFragmentManager.findFragmentByTag(activeNavPosition.getTag())

        // Remove any child fragments in the back stack
        clearFragmentBackStack(activeFragment)

        // Grab the requested top-level fragment and load if not already
        // in the current view.
        supportFragmentManager.findFragment(navPosition)?.let { frag ->
            frag.deferInit = deferInit
            if (frag.isHidden || !frag.isAdded) {
                // Remove the active fragment and replace with this newly selected one
                hideParentFragment(activeFragment)
                showTopLevelFragment(frag, navPosition.getTag())
                supportFragmentManager.executePendingTransactions()
                activeNavPosition = navPosition
                return true
            }
        }

        return false
    }

    /**
     * Show the provided fragment in the fragment container. This should
     * only be used with top-level fragments.
     */
    private fun showTopLevelFragment(fragment: TopLevelFragment, tag: String) {
        if (fragment.isHidden) {
            supportFragmentManager.beginTransaction().show(fragment).commit()
        } else {
            supportFragmentManager.beginTransaction().add(R.id.container, fragment, tag).commit()
        }
    }

    /**
     * Hide the provided fragment in the fragment container. This
     * should only be used with top-level fragments.
     */
    private fun hideParentFragment(fragment: Fragment?) {
        fragment?.let {
            with(supportFragmentManager) {
                if (isStateSaved) {
                    // fragmentManager state already saved, no changes to state allowed
                    return
                }
                beginTransaction().hide(it).commit()
            }
        }
    }

    /**
     * Pop all child fragments to return to the top-level view.
     * returns true if child fragments existed.
     */
    private fun clearFragmentBackStack(fragment: Fragment?): Boolean {
        fragment?.let {
            with(it.childFragmentManager) {
                // If the fragment manager's state has already been saved,
                // exit to avoid the IllegalStateException
                if (isStateSaved) {
                    return true
                }

                if (backStackEntryCount > 0) {
                    popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    return true
                }
            }
        }
        return false
    }
    // endregion

    override fun showOrderList(orderStatusFilter: String?) {
        val navPos = BottomNavigationPosition.ORDERS.position

        if (switchFragment(ORDERS, true)) {
            // Set the active bottom bar selection without firing its changed event
            bottom_nav.active(navPos)

            val fragment = supportFragmentManager.findFragment(ORDERS)
            (fragment as? OrderListFragment)?.onFilterSelected(orderStatusFilter)
        }
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
}
