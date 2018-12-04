package com.woocommerce.android.ui.login

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.content.res.AppCompatResources
import android.support.v7.widget.LinearLayoutManager
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.adapter.SiteListAdapter
import com.woocommerce.android.ui.login.adapter.SiteListAdapter.OnSiteClickListener
import com.woocommerce.android.ui.login.adapter.SiteListUnsupportedAdapter
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.CrashlyticsUtils
import dagger.android.AndroidInjection
import kotlinx.android.synthetic.main.activity_login_epilogue.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

class LoginEpilogueActivity : AppCompatActivity(), LoginEpilogueContract.View, OnSiteClickListener {
    companion object {
        private const val STATE_KEY_SUPPORTED_SITE_ID_LIST = "key-supported-site-id-list"
        private const val STATE_KEY_UNSUPPORTED_SITE_ID_LIST = "key-unsupported-site-id-list"

        private const val URL_UPGRADE_WOOCOMMERCE = "https://docs.woocommerce.com/document/how-to-update-woocommerce/"
    }

    @Inject lateinit var presenter: LoginEpilogueContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite

    private lateinit var siteAdapter: SiteListAdapter
    private lateinit var unsupportedSiteAdapter: SiteListUnsupportedAdapter

    private var loginProgressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login_epilogue)

        ActivityUtils.setStatusBarColor(this, R.color.wc_grey_mid)
        presenter.takeView(this)

        supported_recycler.layoutManager = LinearLayoutManager(this)
        siteAdapter = SiteListAdapter(this, this)
        supported_recycler.adapter = siteAdapter

        unsupported_recycler.layoutManager = LinearLayoutManager(this)
        unsupportedSiteAdapter = SiteListUnsupportedAdapter(this)
        unsupported_recycler.adapter = unsupportedSiteAdapter

        showUserInfo()

        savedInstanceState?.let { bundle ->
            val supportedSites = presenter.getSitesForLocalIds(bundle.getIntArray(STATE_KEY_SUPPORTED_SITE_ID_LIST))
            val unsupportedSites = presenter.getSitesForLocalIds(bundle.getIntArray(STATE_KEY_UNSUPPORTED_SITE_ID_LIST))
            showStoreList(supportedSites, unsupportedSites)
        } ?: run {
            loginProgressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_sites))
            supported_frame_list_container.visibility = View.GONE
            presenter.checkWCVersionsForAllSites()

            AnalyticsTracker.track(
                    Stat.LOGIN_EPILOGUE_STORES_SHOWN,
                    mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size))
        }

        // show buttons side-by-side in landscape
        if (DisplayUtils.isLandscape(this)) {
            frame_bottom.orientation = LinearLayout.HORIZONTAL
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

        val supportedSiteIdList = siteAdapter.siteList.map { it.id }
        val unsupportedSiteIdList = unsupportedSiteAdapter.siteList.map { it.id }

        outState.putIntArray(STATE_KEY_SUPPORTED_SITE_ID_LIST, supportedSiteIdList.toIntArray())
        outState.putIntArray(STATE_KEY_UNSUPPORTED_SITE_ID_LIST, unsupportedSiteIdList.toIntArray())
    }

    override fun onBackPressed() {
        AnalyticsTracker.trackBackPressed(this)

        finish()
    }

    override fun showUserInfo() {
        text_displayname.text = presenter.getUserDisplayName()
        text_username.text = String.format(getString(R.string.at_username), presenter.getUserName())

        GlideApp.with(this)
                .load(presenter.getUserAvatarUrl())
                .placeholder(R.drawable.ic_placeholder_gravatar_grey_lighten_20_100dp)
                .circleCrop()
                .into(image_avatar)
    }

    override fun showStoreList(supportedWCSites: List<SiteModel>, unsupportedWCSites: List<SiteModel>) {
        loginProgressDialog?.takeIf { it.isShowing }?.dismiss()

        if (supportedWCSites.isEmpty() && unsupportedWCSites.isEmpty()) {
            showNoStoresView()
            return
        }

        if (supportedWCSites.isNotEmpty()) {
            supported_frame_list_container.visibility = View.VISIBLE
            button_update_instructions.visibility = View.GONE

            supported_text_list_label.text = if (supportedWCSites.size == 1)
                getString(R.string.login_connected_store)
            else
                getString(R.string.login_pick_store)

            if (selectedSite.isSet()) {
                siteAdapter.selectedSiteId = selectedSite.get().siteId
            } else {
                siteAdapter.selectedSiteId = supportedWCSites[0].siteId
            }

            siteAdapter.siteList = supportedWCSites

            button_continue.text = getString(R.string.continue_button)
            button_continue.setOnClickListener { _ ->
                val site = presenter.getSiteBySiteId(siteAdapter.selectedSiteId)
                site?.let { it ->
                    selectedSite.set(it)
                    CrashlyticsUtils.initSite(it)
                    AnalyticsTracker.track(
                            Stat.LOGIN_EPILOGUE_STORE_PICKER_CONTINUE_TAPPED,
                            mapOf(AnalyticsTracker.KEY_SELECTED_STORE_ID to it.id))
                    finishEpilogue()
                }
            }
        } else {
            // Show 'Update instructions' button, and replace 'Continue' button with 'Refresh'
            button_update_instructions.visibility = View.VISIBLE
            button_update_instructions.setOnClickListener {
                ActivityUtils.openUrlExternal(this, URL_UPGRADE_WOOCOMMERCE)
            }

            button_continue.text = getString(R.string.refresh_button)
            button_continue.setOnClickListener {
                loginProgressDialog = ProgressDialog.show(this, null, getString(R.string.login_verifying_sites))
                presenter.checkWCVersionsForAllSites()
            }
            supported_frame_list_container.visibility = View.GONE
        }

        if (unsupportedWCSites.isNotEmpty()) {
            unsupported_frame_list_container.visibility = View.VISIBLE
            unsupportedSiteAdapter.siteList = unsupportedWCSites
        }
    }

    override fun onSiteClick(siteId: Long) {
        val site = presenter.getSiteBySiteId(siteId)
        site?.let { selectedSite.set(it) }
    }

    private fun showNoStoresView() {
        supported_frame_list_container.visibility = View.GONE
        unsupported_frame_list_container.visibility = View.GONE
        no_stores_view.visibility = View.VISIBLE

        val noStoresImage =
                if (DisplayUtils.isLandscape(this)) null
                else AppCompatResources.getDrawable(this, R.drawable.ic_woo_no_store)
        no_stores_view.setCompoundDrawablesWithIntrinsicBounds(null, noStoresImage, null, null)

        button_continue.text = getString(R.string.login_with_a_different_account)
        button_continue.setOnClickListener {
            presenter.logout()
        }
    }

    /**
     * called by the presenter after logout completes
     */
    override fun cancel() {
        showLoginActivityAndFinish()
    }

    private fun showLoginActivityAndFinish() {
        val intent = Intent(this, LoginActivity::class.java)
        LoginMode.WPCOM_LOGIN_ONLY.putInto(intent)
        startActivity(intent)
        finish()
    }

    private fun finishEpilogue() {
        // Now that the SelectedSite is set, register the device for WordPress.com Woo push notifications for this site
        FCMRegistrationIntentService.enqueueWork(this)

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
