package com.woocommerce.android.ui.main

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.disableShiftMode
import com.woocommerce.android.ui.login.LoginActivity
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_main.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

class MainActivity : AppCompatActivity(),
        MainContract.View,
        HasSupportFragmentInjector,
        BottomNavigationView.OnNavigationItemSelectedListener {
    companion object {
        private const val REQUEST_CODE_ADD_ACCOUNT = 100

        private const val MAGIC_LOGIN = "magic-login"
        private const val TOKEN_PARAMETER = "token"
        private const val KEY_POSITION = "key-position"
    }

    @Inject lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject lateinit var presenter: MainContract.Presenter

    private var activeNavPosition: BottomNavigationPosition = BottomNavigationPosition.DASHBOARD

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        restoreSavedInstanceState(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Set the toolbar
        setSupportActionBar(toolbar as Toolbar)

        // Verify authenticated session
        presenter.takeView(this)
        if (!presenter.userIsLoggedIn()) {
            if (hasMagicLinkLoginIntent()) {
                getAuthTokenFromIntent()?.let { presenter.storeMagicLinkToken(it) }
            } else {
                showLoginScreen()
                return
            }
        }

        setupBottomNavigation()
        initFragment(savedInstanceState)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_action_bar, menu)
        return true
    }

    public override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        // Store the current bottom bar navigation position.
        outState?.putInt(KEY_POSITION, activeNavPosition.id)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle?) {
        // Restore the current navigation position
        savedInstanceState?.also {
            val id = it.getInt(KEY_POSITION, BottomNavigationPosition.DASHBOARD.id)
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
            // User selected the logout menu option
            R.id.menu_signout -> {
                presenter.logout()
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
            }
        }
    }

    override fun notifyTokenUpdated() {
        if (hasMagicLinkLoginIntent()) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.LOGIN_MAGIC_LINK_SUCCEEDED)
            // TODO Launch next screen
        }
    }

    override fun showLoginScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
        startActivityForResult(intent, REQUEST_CODE_ADD_ACCOUNT)
        finish()
    }

    override fun updateStoreList(storeList: List<SiteModel>) {
        if (storeList.isEmpty()) {
//            textView.text = "No WooCommerce sites found!"
        } else {
            val siteNameList = """
                |Found stores:
                |
                |${storeList.joinToString("\n\n") {
                "${it.name}\n(${it.url})\nType: ${if (it.isWpComStore) "WordPress.com Store" else "Jetpack Store" }"
            }}
            """.trimMargin()
//            textView.text = siteNameList
        }
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
        bottom_nav.disableShiftMode()
        bottom_nav.active(activeNavPosition.position)
        bottom_nav.setOnNavigationItemSelectedListener(this)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val navPosition = findNavigationPositionById(item.itemId)
        return switchFragment(navPosition)
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
    private fun FragmentManager.findFragment(position: BottomNavigationPosition): Fragment {
        return findFragmentByTag(position.getTag()) ?: position.createFragment()
    }

    /**
     * If the user clicked on the already displayed top-level option, pop any child
     * fragments and reset to the parent fragment.
     *
     * If the user selected an option not currently active, pop any child fragments,
     * hide the current top-level fragment and add/show the destination top-level fragment.
     *
     * Immediately execute transactions with FragmentManager#executePendingTransactions.
     */
    private fun switchFragment(navPosition: BottomNavigationPosition): Boolean {
        val activeFragment = supportFragmentManager.findFragmentByTag(activeNavPosition.getTag())

        // Remove any child fragments in the back stack
        clearFragmentBackStack(activeFragment)

        // Grab the requested top-level fragment and load if not already
        // in the current view.
        val fragment = supportFragmentManager.findFragment(navPosition)
        if (fragment.isHidden || !fragment.isAdded) {
            // Remove the active fragment and replace with this newly selected one
            hideParentFragment(activeFragment)
            showTopLevelFragment(fragment, navPosition.getTag())
            supportFragmentManager.executePendingTransactions()
            activeNavPosition = navPosition
            return true
        }
        return false
    }

    /**
     * Show the provided fragment in the fragment container. This should
     * only be used with top-level fragments.
     */
    private fun showTopLevelFragment(fragment: Fragment, tag: String) {
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
            supportFragmentManager.beginTransaction().hide(fragment).commit()
        }
    }

    /**
     * Pop all child fragments to return to the top-level view.
     */
    private fun clearFragmentBackStack(fragment: Fragment?) {
        fragment?.let {
            while (fragment.childFragmentManager.backStackEntryCount > 0) {
                fragment.childFragmentManager.popBackStackImmediate()
            }
        }
    }
    // endregion

    fun getSite(): SiteModel? {
        return presenter.getSelectedSite()
    }
}
