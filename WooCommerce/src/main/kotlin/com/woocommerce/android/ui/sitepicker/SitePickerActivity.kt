package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.mystore.RevenueStatsAvailabilityFetcher
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.CrashUtils
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_site_picker.*
import kotlinx.android.synthetic.main.view_login_epilogue_button_bar.*
import kotlinx.android.synthetic.main.view_login_no_stores.*
import kotlinx.android.synthetic.main.view_login_user_info.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

class SitePickerActivity : AppCompatActivity(), SitePickerContract.View, OnSiteClickListener,
        LoginEmailHelpDialogFragment.Listener {
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
        private const val KEY_CALLED_FROM_LOGIN = "called_from_login"
        private const val KEY_LOGIN_SITE_URL = "login_site"
        private const val KEY_CLICKED_SITE_ID = "clicked_site_id"

        fun showSitePickerFromLogin(context: Context) {
            val intent = Intent(context, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, true)
            context.startActivity(intent)
        }

        fun showSitePickerForResult(fragment: Fragment) {
            val intent = Intent(fragment.activity, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, false)
            fragment.startActivityForResult(intent, RequestCodes.SITE_PICKER)
        }
    }

    @Inject lateinit var presenter: SitePickerContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    @Inject lateinit var revenueStatsAvailabilityFetcher: RevenueStatsAvailabilityFetcher

    private lateinit var siteAdapter: SitePickerAdapter

    private var progressDialog: ProgressDialog? = null
    private var calledFromLogin: Boolean = false
    private var currentSite: SiteModel? = null
    private var skeletonView = SkeletonView()
    private var clickedSiteId = 0L

    /**
     * Signin M1: The url the customer logged into the app with.
     */
    private var loginSiteUrl: String? = null

    /**
     * Signin M1: Don't display the sites for selection if this is true.
     */
    private var deferLoadingSitesIntoView: Boolean = false

    /**
     * Signin M1: Tracks whether or not there are stores for the user to view.
     * This controls the "view connected stores" button visibility.
     */
    private var hasConnectedStores: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_picker)

        currentSite = selectedSite.getIfExists()

        calledFromLogin = savedInstanceState?.getBoolean(KEY_CALLED_FROM_LOGIN)
                ?: intent.getBooleanExtra(KEY_CALLED_FROM_LOGIN, false)

        if (calledFromLogin) {
            toolbar.visibility = View.GONE
            button_help.setOnClickListener {
                startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
                AnalyticsTracker.track(Stat.SITE_PICKER_HELP_BUTTON_TAPPED)
            }
            site_list_container.elevation = resources.getDimension(R.dimen.plane_01)
        } else {
            // Opened from settings to change active store.
            toolbar.visibility = View.VISIBLE
            setSupportActionBar(toolbar as Toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            title = getString(R.string.site_picker_title)
            button_help.visibility = View.GONE
            site_list_label.visibility = View.GONE
            site_list_container.elevation = 0f
            (site_list_container.layoutParams as MarginLayoutParams).topMargin = 0
            (site_list_container.layoutParams as MarginLayoutParams).bottomMargin = 0
        }

        presenter.takeView(this)

        sites_recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        sites_recycler.adapter = siteAdapter

        showUserInfo()

        savedInstanceState?.let { bundle ->
            clickedSiteId = bundle.getLong(KEY_CLICKED_SITE_ID)

            // Signin M1: If using new login M1 flow, we skip showing the store list.
            bundle.getString(KEY_LOGIN_SITE_URL)?.let { url ->
                deferLoadingSitesIntoView = true
                loginSiteUrl = url
            }

            val ids = bundle.getIntArray(STATE_KEY_SITE_ID_LIST) ?: IntArray(0)
            val sites = presenter.getSitesForLocalIds(ids)
            if (sites.isNotEmpty()) {
                showStoreList(sites)
            } else {
                presenter.loadSites()
            }
        } ?: run {
            // Signin M1: If using a url to login, we skip showing the store list
            AppPrefs.getLoginSiteAddress().takeIf { it.isNotEmpty() }?.let { url ->
                deferLoadingSitesIntoView = true
                loginSiteUrl = url
            }

            // Signin M1: We still want the presenter to go out and fetch sites so we
            // know whether or not to show the "view connected stores" button.
            if (calledFromLogin) {
                // Sites have already been fetched as part of the login process. Just load them
                // from the db.
                presenter.loadSites()
            } else {
                presenter.loadAndFetchSites()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onDestroy() {
        presenter.dropView()
        super.onDestroy()
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        val sitesList = siteAdapter.siteList.map { it.id }
        outState.putIntArray(STATE_KEY_SITE_ID_LIST, sitesList.toIntArray())
        outState.putBoolean(KEY_CALLED_FROM_LOGIN, calledFromLogin)
        outState.putLong(KEY_CLICKED_SITE_ID, clickedSiteId)

        loginSiteUrl?.let { outState.putString(KEY_LOGIN_SITE_URL, it) }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        AnalyticsTracker.trackBackPressed(this)
        finish()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    /**
     * Show the current user info if this was called from login, otherwise hide user views
     */
    override fun showUserInfo() {
        if (calledFromLogin) {
            user_info_group.visibility = View.VISIBLE
            text_displayname.text = presenter.getUserDisplayName()

            presenter.getUserName()?.let { userName ->
                if (userName.isNotEmpty()) {
                    text_username.text = String.format(getString(R.string.at_username), userName)
                }
            }

            GlideApp.with(this)
                    .load(presenter.getUserAvatarUrl())
                    .placeholder(R.drawable.img_gravatar_placeholder)
                    .circleCrop()
                    .into(image_avatar)
        } else {
            user_info_group.visibility = View.GONE
        }
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        progressDialog?.takeIf { it.isShowing }?.dismiss()

        if (deferLoadingSitesIntoView) {
            if (wcSites.isNotEmpty()) {
                hasConnectedStores = true

                // Make "show connected stores" visible to the user
                button_secondary.visibility = View.VISIBLE
            } else {
                hasConnectedStores = false

                // Hide "show connected stores"
                button_secondary.visibility = View.GONE
            }

            loginSiteUrl?.let { processLoginSite(it) }
            return
        }
        // Hide "show connected stores" button now that we're displaying
        // the store list.
        button_secondary.visibility = View.GONE

        AnalyticsTracker.track(
                Stat.SITE_PICKER_STORES_SHOWN,
                mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size)
        )

        site_picker_root.visibility = View.VISIBLE

        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        no_stores_view.visibility = View.GONE
        site_list_container.visibility = View.VISIBLE
        button_email_help.visibility = View.GONE

        site_list_label.text = when {
            wcSites.size == 1 -> getString(R.string.login_connected_store)
            calledFromLogin -> getString(R.string.login_pick_store)
            else -> getString(R.string.site_picker_title)
        }

        siteAdapter.siteList = wcSites
        siteAdapter.selectedSiteId = if (clickedSiteId > 0) {
            clickedSiteId
        } else {
            selectedSite.getIfExists()?.siteId ?: wcSites[0].siteId
        }

        button_primary.text = getString(R.string.done)
        button_primary.isEnabled = true
        button_primary.setOnClickListener {
            presenter.getSiteBySiteId(siteAdapter.selectedSiteId)?.let { site -> siteSelected(site) }
        }
    }

    override fun onSiteClick(siteId: Long) {
        clickedSiteId = siteId
        button_primary.isEnabled = true
    }

    override fun siteSelected(site: SiteModel, isAutoLogin: Boolean) {
        // finish if user simply selected the current site
        currentSite?.let {
            if (site.siteId == it.siteId) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        }

        if (isAutoLogin) {
            AnalyticsTracker.track(
                    Stat.SITE_PICKER_AUTO_LOGIN_SUBMITTED,
                    mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id))
        } else {
            AnalyticsTracker.track(
                    Stat.SITE_PICKER_CONTINUE_TAPPED,
                    mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id))
        }

        progressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
        presenter.verifySiteApiVersion(site)

        // Preemptively also update the site settings so we have them available sooner
        presenter.updateWooSiteSettings(site)

        // also check if the site supports the new v4 revenue stats api changes
        revenueStatsAvailabilityFetcher.fetchRevenueStatsAvailability(site)
    }

    /**
     * User selected a site and it passed verification - make it the selected site and finish
     */
    override fun siteVerificationPassed(site: SiteModel) {
        progressDialog?.dismiss()

        selectedSite.set(site)
        CrashUtils.setCurrentSite(site)
        FCMRegistrationIntentService.enqueueWork(this)

        // if we came here from login, start the main activity
        if (calledFromLogin) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Clear logged in url from AppPrefs
        AppPrefs.removeLoginSiteAddress()

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun siteVerificationFailed(site: SiteModel) {
        progressDialog?.dismiss()

        // re-select the previous site, if there was one
        siteAdapter.selectedSiteId = currentSite?.siteId ?: 0L
        button_primary.isEnabled = siteAdapter.selectedSiteId != 0L

        WooUpgradeRequiredDialog().show(supportFragmentManager)
    }

    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @Suppress("WrongConstant")
    override fun siteVerificationError(site: SiteModel) {
        progressDialog?.dismiss()

        val siteName = if (!TextUtils.isEmpty(site.name)) site.name else getString(R.string.untitled)
        Snackbar.make(
                site_picker_root as ViewGroup,
                getString(R.string.login_verifying_site_error, siteName),
                BaseTransientBottomBar.LENGTH_LONG
        ).show()
    }

    override fun showNoStoresView() {
        if (deferLoadingSitesIntoView) {
            return
        }

        site_picker_root.visibility = View.VISIBLE
        site_list_container.visibility = View.GONE
        no_stores_view.visibility = View.VISIBLE

        button_primary.text = getString(R.string.login_try_another_account)
        button_primary.isEnabled = true
        button_primary.setOnClickListener {
            presenter.logout()
        }
    }

    override fun showSkeleton(show: Boolean) {
        if (deferLoadingSitesIntoView) {
            return
        }

        when (show) {
            true -> skeletonView.show(sites_recycler, R.layout.skeleton_site_picker, delayed = true)
            false -> skeletonView.hide()
        }
    }

    /**
     * called by the presenter after logout completes - this would occur if the user was logged out
     * as a result of having no stores, which would only happen during the login flow
     */
    override fun didLogout() {
        setResult(Activity.RESULT_CANCELED)
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WOO_LOGIN_MODE.putInto(intent)
        startActivity(intent)
        finish()
    }

    // region SignIn M1
    /**
     * Signin M1: User logged in with a URL. Here we check that login url to see
     * if the site is (in this order):
     * - Connected to the same account the user logged in with
     * - Has WooCommerce installed
     */
    private fun processLoginSite(url: String) {
        presenter.getSiteModelByUrl(url)?.let { site ->
            // Remove app prefs no longer needed by the login process
            AppPrefs.removeLoginUserBypassedJetpackRequired()

            if (!site.hasWooCommerce) {
                // Show not woo store message view.
                showSiteNotWooStore(site)
            } else {
                // We have a pre-validation woo store. Attempt to just
                // login with this store directly.
                siteSelected(site, isAutoLogin = true)
            }
        } ?: run {
            if (AppPrefs.getLoginUserBypassedJetpackRequired()) {
                // The user was warned that Jetpack was required during the login
                // process and continued with login anyway. It's likely we just
                // can't connect to jetpack so show a different message.
                showSiteNotConnectedJetpackView(url)
            } else {
                // The url doesn't match any sites for this account.
                showSiteNotConnectedAccountView(url)
            }
        }
    }

    /**
     * SignIn M1: Shows the list of sites connected to the logged
     * in user.
     */
    private fun showConnectedSites() {
        deferLoadingSitesIntoView = false
        presenter.loadSites()
    }

    /**
     * SignIn M1: The url the user submitted during login belongs
     * to a site that is not connected to the account the user logged
     * in with.
     */
    override fun showSiteNotConnectedAccountView(url: String) {
        AnalyticsTracker.track(
                Stat.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_TO_USER,
                mapOf(AnalyticsTracker.KEY_URL to url, AnalyticsTracker.KEY_HAS_CONNECTED_STORES to hasConnectedStores))

        site_picker_root.visibility = View.VISIBLE
        no_stores_view.visibility = View.VISIBLE
        button_email_help.visibility = View.VISIBLE
        site_list_container.visibility = View.GONE

        no_stores_view.text = getString(R.string.login_not_connected_to_account, url)

        button_email_help.setOnClickListener {
            AnalyticsTracker.track(Stat.SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED)

            LoginEmailHelpDialogFragment().show(supportFragmentManager, LoginEmailHelpDialogFragment.TAG)
        }

        with(button_primary) {
            text = getString(R.string.login_try_another_account)
            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_ACCOUNT_BUTTON_TAPPED)

                presenter.logout()
            }
        }

        with(button_secondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    showConnectedSites()
                }

                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun showSiteNotConnectedJetpackView(url: String) {
        AnalyticsTracker.track(
                Stat.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_CONNECTED_JETPACK,
                mapOf(AnalyticsTracker.KEY_URL to url))

        site_picker_root.visibility = View.VISIBLE
        no_stores_view.visibility = View.VISIBLE
        button_email_help.visibility = View.GONE
        site_list_container.visibility = View.GONE

        with(no_stores_view) {
            val refreshAppText = getString(R.string.login_refresh_app_continue)
            val notConnectedText = getString(
                    R.string.login_not_connected_jetpack,
                    url,
                    refreshAppText
            )

            val spannable = SpannableString(notConnectedText)
            spannable.setSpan(
                    WooClickableSpan {
                        AnalyticsTracker.track(Stat.SITE_PICKER_NOT_CONNECTED_JETPACK_REFRESH_APP_LINK_TAPPED)

                        progressDialog?.takeIf { !it.isShowing }?.dismiss()
                        progressDialog = ProgressDialog.show(
                                this@SitePickerActivity,
                                null,
                                getString(R.string.login_refresh_app_progress_jetpack))
                        // Tell the presenter to fetch a fresh list of
                        // sites from the API. When the results come back the login
                        // process will restart again.
                        presenter.fetchSitesFromAPI()
                    },
                    (notConnectedText.length - refreshAppText.length),
                    notConnectedText.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
        }

        with(button_primary) {
            text = getString(R.string.login_try_another_store)
            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_STORE_BUTTON_TAPPED)

                presenter.logout()
            }
        }

        with(button_secondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    showConnectedSites()
                }

                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    /**
     * SignIn M1: The user the user submitted during login belongs
     * to a site that does not have WooCommerce installed.
     */
    override fun showSiteNotWooStore(site: SiteModel) {
        AnalyticsTracker.track(
                Stat.SITE_PICKER_AUTO_LOGIN_ERROR_NOT_WOO_STORE,
                mapOf(AnalyticsTracker.KEY_URL to site.url,
                        AnalyticsTracker.KEY_HAS_CONNECTED_STORES to hasConnectedStores))

        site_picker_root.visibility = View.VISIBLE
        no_stores_view.visibility = View.VISIBLE
        site_list_container.visibility = View.GONE

        with(no_stores_view) {
            // Build and configure the error message and make part of the message
            // clickable. When clicked, we'll fetch a fresh copy of the active site from the API.
            val siteName = site.name.takeIf { !it.isNullOrEmpty() } ?: site.url
            val refreshAppText = getString(R.string.login_refresh_app)
            val notWooMessage = getString(R.string.login_not_woo_store, siteName, refreshAppText)

            val spannable = SpannableString(notWooMessage)
            spannable.setSpan(
                    WooClickableSpan {
                        AnalyticsTracker.track(Stat.SITE_PICKER_NOT_WOO_STORE_REFRESH_APP_LINK_TAPPED)

                        progressDialog?.takeIf { !it.isShowing }?.dismiss()
                        progressDialog = ProgressDialog.show(
                                this@SitePickerActivity,
                                null,
                                getString(R.string.login_refresh_app_progress))
                        presenter.fetchUpdatedSiteFromAPI(site)
                    },
                    (notWooMessage.length - refreshAppText.length),
                    notWooMessage.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)

            setText(spannable, TextView.BufferType.SPANNABLE)
            movementMethod = LinkMovementMethod.getInstance()
        }

        with(button_primary) {
            text = getString(R.string.login_try_another_account)

            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_ACCOUNT_BUTTON_TAPPED)

                presenter.logout()
            }
        }

        with(button_secondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    showConnectedSites()
                }

                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    override fun onEmailNeedMoreHelpClicked() {
        startActivity(HelpActivity.createIntent(this, Origin.LOGIN_CONNECTED_EMAIL_HELP, null))
    }
    // endregion
}
