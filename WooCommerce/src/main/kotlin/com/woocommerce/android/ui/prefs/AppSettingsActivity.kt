package com.woocommerce.android.ui.prefs

import android.app.AlertDialog
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.view.ContextThemeWrapper
import android.support.v7.widget.Toolbar
import android.view.MenuItem
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.ui.prefs.AppSettingsFragment.AppSettingsListener
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.support.HasSupportFragmentInjector
import kotlinx.android.synthetic.main.activity_app_settings.*
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder
import org.wordpress.android.fluxc.generated.SiteActionBuilder
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.AccountStore.OnAuthenticationChanged
import javax.inject.Inject

class AppSettingsActivity : AppCompatActivity(), AppSettingsListener, HasSupportFragmentInjector {
    @Inject internal lateinit var fragmentInjector: DispatchingAndroidInjector<Fragment>
    @Inject internal lateinit var dispatcher: Dispatcher
    @Inject internal lateinit var accountStore: AccountStore

    override fun supportFragmentInjector(): AndroidInjector<Fragment> = fragmentInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_app_settings)
        setSupportActionBar(toolbar as Toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        dispatcher.register(this)

        if (savedInstanceState == null) {
            showAppSettingsFragment()
        }
    }

    override fun onDestroy() {
        dispatcher.unregister(this)
        super.onDestroy()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount == 1) {
            finish()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRequestLogout() {
        confirmLogout()
    }

    override fun onRequestShowPrivacySettings() {
        showPrivacySettingsFragment()
    }

    private fun showAppSettingsFragment() {
        val fragment = AppSettingsFragment.newInstance()
        showFragment(fragment, AppSettingsFragment.TAG, false)
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_SETTINGS)
    }

    private fun showPrivacySettingsFragment() {
        val fragment = PrivacySettingsFragment.newInstance()
        showFragment(fragment, PrivacySettingsFragment.TAG, true)
        AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_PRIVACY_SETTINGS)
    }

    private fun confirmLogout() {
        AlertDialog.Builder(ContextThemeWrapper(this, R.style.Woo_Dialog))
                .setMessage(R.string.settings_confirm_signout)
                .setPositiveButton(R.string.signout) { dialog, whichButton -> logout() }
                .setNegativeButton(R.string.cancel, null)
                .setCancelable(true)
                .create()
                .show()
    }

    private fun logout() {
        // Reset default account
        dispatcher.dispatch(AccountActionBuilder.newSignOutAction())
        // Delete wpcom and jetpack sites
        dispatcher.dispatch(SiteActionBuilder.newRemoveWpcomAndJetpackSitesAction())
    }

    private fun showFragment(fragment: Fragment, tag: String, animate: Boolean) {
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        if (animate) {
            fragmentTransaction.setCustomAnimations(
                    R.anim.activity_slide_in_from_right,
                    R.anim.activity_slide_out_to_left,
                    R.anim.activity_slide_in_from_left,
                    R.anim.activity_slide_out_to_right)
        }
        fragmentTransaction.replace(R.id.fragment_container, fragment, tag)
                .addToBackStack(null)
                .commitAllowingStateLoss()
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onAuthenticationChanged(event: OnAuthenticationChanged) {
        // if user logged out, return to the main activity so it can show the login screen
        if (!accountStore.hasAccessToken()) {
            finish()
        }
    }
}
