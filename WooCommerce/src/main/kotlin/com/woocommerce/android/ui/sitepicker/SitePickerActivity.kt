package com.woocommerce.android.ui.sitepicker

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import android.text.TextUtils
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.util.CrashUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_site_picker.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class SitePickerActivity : AppCompatActivity(), SitePickerContract.View, OnSiteClickListener {
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
        private const val KEY_CALLED_FROM_LOGIN = "called_from_login"

        fun showSitePickerFromLogin(context: Context) {
            val intent = Intent(context, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, true)
            context.startActivity(intent)
        }

        fun showSitePickerForResult(activity: Activity, requestCode: Int) {
            val intent = Intent(activity, SitePickerActivity::class.java)
            intent.putExtra(KEY_CALLED_FROM_LOGIN, false)
            activity.startActivityForResult(intent, requestCode)
        }
    }

    @Inject lateinit var presenter: SitePickerContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SitePickerAdapter

    private var progressDialog: ProgressDialog? = null
    private var calledFromLogin: Boolean = false
    private var currentSite: SiteModel? = null
    private var skeletonView = SkeletonView()

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_site_picker)

        currentSite = selectedSite.getIfExists()

        calledFromLogin = savedInstanceState?.getBoolean(KEY_CALLED_FROM_LOGIN)
                ?: intent.getBooleanExtra(KEY_CALLED_FROM_LOGIN, false)

        if (calledFromLogin) {
            toolbar.visibility = View.GONE
            ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
            button_help.setOnClickListener {
                startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
                AnalyticsTracker.track(Stat.SITE_PICKER_HELP_BUTTON_TAPPED)
            }
        } else {
            site_picker_root.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.white))

            toolbar.visibility = View.VISIBLE
            setSupportActionBar(toolbar as Toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            title = getString(R.string.site_picker_title)
            button_help.visibility = View.GONE
            site_list_label.visibility = View.GONE
            site_list_container.cardElevation = 0f
            (site_list_container.layoutParams as MarginLayoutParams).topMargin = 0
            (site_list_container.layoutParams as MarginLayoutParams).bottomMargin = 0
            sites_recycler.setPadding(
                    resources.getDimensionPixelSize(R.dimen.margin_extra_large),
                    resources.getDimensionPixelSize(R.dimen.margin_large),
                    resources.getDimensionPixelSize(R.dimen.margin_extra_large),
                    resources.getDimensionPixelSize(R.dimen.margin_large))
        }

        presenter.takeView(this)

        sites_recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        sites_recycler.adapter = siteAdapter

        showUserInfo()

        savedInstanceState?.let { bundle ->
            val sites = presenter.getSitesForLocalIds(bundle.getIntArray(STATE_KEY_SITE_ID_LIST))
            showStoreList(sites)
        } ?: run {
            presenter.loadAndFetchSites()

            AnalyticsTracker.track(
                    Stat.SITE_PICKER_STORES_SHOWN,
                    mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size)
            )
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
            text_displayname.text = presenter.getUserDisplayName()

            presenter.getUserName()?.let { userName ->
                if (userName.isNotEmpty()) {
                    text_username.text = String.format(getString(R.string.at_username), userName)
                }
            }

            GlideApp.with(this)
                    .load(presenter.getUserAvatarUrl())
                    .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                    .circleCrop()
                    .into(image_avatar)
        } else {
            text_displayname.visibility = View.GONE
            text_username.visibility = View.GONE
            image_avatar.visibility = View.GONE
        }
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        progressDialog?.takeIf { it.isShowing }?.dismiss()

        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        no_stores_view.visibility = View.GONE
        site_list_container.visibility = View.VISIBLE

        site_list_label.text = if (wcSites.size == 1) {
            getString(R.string.login_connected_store)
        } else if (calledFromLogin) {
            getString(R.string.login_pick_store)
        } else {
            getString(R.string.site_picker_title)
        }

        siteAdapter.siteList = wcSites
        siteAdapter.selectedSiteId = selectedSite.getIfExists()?.siteId ?: wcSites[0].siteId

        button_continue.text = getString(R.string.continue_button)
        button_continue.isEnabled = true
        button_continue.setOnClickListener {
            presenter.getSiteBySiteId(siteAdapter.selectedSiteId)?.let { site -> siteSelected(site) }
        }
    }

    override fun onSiteClick(siteId: Long) {
        button_continue.isEnabled = true
    }

    override fun siteSelected(site: SiteModel) {
        // finish if user simply selected the current site
        currentSite?.let {
            if (site.siteId == it.siteId) {
                setResult(Activity.RESULT_CANCELED)
                finish()
                return
            }
        }

        AnalyticsTracker.track(
                Stat.SITE_PICKER_CONTINUE_TAPPED,
                mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to site.id)
        )

        progressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_site))
        presenter.verifySiteApiVersion(site)

        // Preemptively also update the site settings so we have them available sooner
        presenter.updateWooSiteSettings(site)
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

        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun siteVerificationFailed(site: SiteModel) {
        progressDialog?.dismiss()

        // re-select the previous site, if there was one
        siteAdapter.selectedSiteId = currentSite?.siteId ?: 0L
        button_continue.isEnabled = siteAdapter.selectedSiteId != 0L

        WooUpgradeRequiredDialog().show(supportFragmentManager)
    }

    override fun siteVerificationError(site: SiteModel) {
        progressDialog?.dismiss()

        val siteName = if (!TextUtils.isEmpty(site.name)) site.name else getString(R.string.untitled)
        Snackbar.make(
                site_picker_root as ViewGroup,
                getString(R.string.login_verifying_site_error, siteName),
                Snackbar.LENGTH_LONG
        ).show()
    }

    override fun showNoStoresView() {
        site_list_container.visibility = View.GONE
        no_stores_view.visibility = View.VISIBLE

        val noStoresImage =
                if (DisplayUtils.isLandscape(this)) null
                else AppCompatResources.getDrawable(this, R.drawable.ic_woo_no_store)
        no_stores_view.setCompoundDrawablesWithIntrinsicBounds(null, noStoresImage, null, null)

        button_continue.text = getString(R.string.login_with_a_different_account)
        button_continue.isEnabled = true
        button_continue.setOnClickListener {
            presenter.logout()
        }
    }

    override fun showSkeleton(show: Boolean) {
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
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
        startActivity(intent)
        finish()
    }
}
