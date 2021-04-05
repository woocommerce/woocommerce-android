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
import android.view.Gravity
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.RequestCodes
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.ActivitySitePickerBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.push.FCMRegistrationIntentService
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.login.LoginEmailHelpDialogFragment
import com.woocommerce.android.ui.login.LoginWhatIsJetpackDialogFragment
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Click
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Flow
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Source
import com.woocommerce.android.ui.login.UnifiedLoginTracker.Step
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.OnSiteClickListener
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.CrashUtils
import com.woocommerce.android.widgets.SkeletonView
import com.woocommerce.android.widgets.WooClickableSpan
import dagger.android.AndroidInjection
import dagger.android.AndroidInjector
import dagger.android.DispatchingAndroidInjector
import dagger.android.HasAndroidInjector
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.login.LoginMode
import javax.inject.Inject

class SitePickerActivity : AppCompatActivity(), SitePickerContract.View, OnSiteClickListener,
        LoginEmailHelpDialogFragment.Listener, HasAndroidInjector {
    companion object {
        private const val STATE_KEY_SITE_ID_LIST = "key-supported-site-id-list"
        private const val KEY_CALLED_FROM_LOGIN = "called_from_login"
        private const val KEY_LOGIN_SITE_URL = "login_site"
        private const val KEY_CLICKED_SITE_ID = "clicked_site_id"
        private const val KEY_UNIFIED_TRACKER_SOURCE = "KEY_UNIFIED_TRACKER_SOURCE"
        private const val KEY_UNIFIED_TRACKER_FLOW = "KEY_UNIFIED_TRACKER_FLOW"
        private const val KEY_UNIFIED_TRACKER_STEP = "KEY_UNIFIED_TRACKER_STEP"

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

    @Inject internal lateinit var androidInjector: DispatchingAndroidInjector<Any>
    @Inject lateinit var presenter: SitePickerContract.Presenter
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var unifiedLoginTracker: UnifiedLoginTracker

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

    private var _binding: ActivitySitePickerBinding? = null
    private val binding get() = _binding!!

    override fun androidInjector(): AndroidInjector<Any> = androidInjector

    override fun onCreate(savedInstanceState: Bundle?) {
        AndroidInjection.inject(this)
        super.onCreate(savedInstanceState)

        _binding = ActivitySitePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentSite = selectedSite.getIfExists()

        calledFromLogin = savedInstanceState?.getBoolean(KEY_CALLED_FROM_LOGIN)
                ?: intent.getBooleanExtra(KEY_CALLED_FROM_LOGIN, false)

        if (calledFromLogin) {
            binding.toolbar.toolbar.visibility = View.GONE
            binding.buttonHelp.setOnClickListener {
                startActivity(HelpActivity.createIntent(this, Origin.LOGIN_EPILOGUE, null))
                AnalyticsTracker.track(Stat.SITE_PICKER_HELP_BUTTON_TAPPED)
                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.SHOW_HELP)
                }
            }
            binding.siteListContainer.elevation = resources.getDimension(R.dimen.plane_01)
        } else {
            // Opened from settings to change active store.
            overridePendingTransition(R.anim.activity_slide_in_from_right, R.anim.activity_slide_out_to_left)

            binding.toolbar.toolbar.visibility = View.VISIBLE
            setSupportActionBar(binding.toolbar.toolbar as Toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            title = getString(R.string.site_picker_title)
            binding.buttonHelp.visibility = View.GONE
            binding.siteListLabel.visibility = View.GONE
            binding.siteListContainer.elevation = 0f
            (binding.siteListContainer.layoutParams as MarginLayoutParams).topMargin = 0
            (binding.siteListContainer.layoutParams as MarginLayoutParams).bottomMargin = 0
        }

        presenter.takeView(this)

        binding.sitesRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        siteAdapter = SitePickerAdapter(this, this)
        binding.sitesRecycler.adapter = siteAdapter

        loadUserInfo()

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

            // Restore state for the unified login tracker
            if (calledFromLogin) {
                unifiedLoginTracker.setSource(bundle.getString(KEY_UNIFIED_TRACKER_SOURCE, Source.DEFAULT.value))
                unifiedLoginTracker.setFlow(bundle.getString(KEY_UNIFIED_TRACKER_FLOW))
                bundle.getString(KEY_UNIFIED_TRACKER_STEP)?.let { stepString ->
                    Step.fromValue(stepString)?.let { unifiedLoginTracker.setStep(it) }
                }
            }
        } ?: run {
            // Set unified login tracker source and flow
            if (calledFromLogin) {
                AppPrefs.getUnifiedLoginLastSource()?.let { unifiedLoginTracker.setSource(it) }
                unifiedLoginTracker.track(Flow.EPILOGUE, Step.START)
            }

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

        // Save state for the unified login tracker
        if (calledFromLogin) {
            unifiedLoginTracker.getFlow()?.value?.let {
                outState.putString(KEY_UNIFIED_TRACKER_FLOW, it)
            }
            outState.putString(KEY_UNIFIED_TRACKER_FLOW, unifiedLoginTracker.getSource().value)
            outState.putString(KEY_UNIFIED_TRACKER_STEP, unifiedLoginTracker.currentStep?.value)
        }

        loginSiteUrl?.let { outState.putString(KEY_LOGIN_SITE_URL, it) }
    }

    override fun onBackPressed() {
        setResult(Activity.RESULT_CANCELED)
        AnalyticsTracker.trackBackPressed(this)
        finish()
        if (!calledFromLogin) {
            overridePendingTransition(R.anim.activity_slide_in_from_left, R.anim.activity_slide_out_to_right)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return false
    }

    /**
     * Load the user info view with user information and gravatar.
     */
    private fun loadUserInfo() {
        binding.loginUserInfo.textDisplayname.text = presenter.getUserDisplayName()

        presenter.getUserName()?.let { userName ->
            if (userName.isNotEmpty()) {
                binding.loginUserInfo.textUsername.text = String.format(getString(R.string.at_username), userName)
            }
        }

        GlideApp.with(this)
            .load(presenter.getUserAvatarUrl())
            .placeholder(R.drawable.img_gravatar_placeholder)
            .circleCrop()
            .into(binding.loginUserInfo.imageAvatar)
    }

    /**
     * Show the current user info if this was called from login, otherwise hide user views. This method
     * will also format the views for the type of layout requested.
     *
     * @param centered: If true, center the user info views for display in error messages, else, left
     * align the views.
     */
    private fun showUserInfo(centered: Boolean) {
        if (calledFromLogin) {
            binding.loginUserInfo.loginUserInfo.visibility = View.VISIBLE
            if (centered) {
                binding.loginUserInfo.loginUserInfo.gravity = Gravity.CENTER
                with(binding.loginUserInfo.imageAvatar) {
                    layoutParams.height = resources.getDimensionPixelSize(R.dimen.image_major_64)
                    layoutParams.width = resources.getDimensionPixelSize(R.dimen.image_major_64)
                    requestLayout()
                }
            } else {
                binding.loginUserInfo.loginUserInfo.gravity = Gravity.START
                with(binding.loginUserInfo.imageAvatar) {
                    layoutParams.height = resources.getDimensionPixelSize(R.dimen.image_major_72)
                    layoutParams.width = resources.getDimensionPixelSize(R.dimen.image_major_72)
                    requestLayout()
                }
            }
        } else {
            binding.loginUserInfo.loginUserInfo.visibility = View.GONE
        }
    }

    override fun showStoreList(wcSites: List<SiteModel>) {
        progressDialog?.takeIf { it.isShowing }?.dismiss()
        showUserInfo(centered = false)

        if (deferLoadingSitesIntoView) {
            if (wcSites.isNotEmpty()) {
                hasConnectedStores = true

                // Make "show connected stores" visible to the user
                binding.loginEpilogueButtonBar.buttonSecondary.visibility = View.VISIBLE
                binding.loginEpilogueButtonBar.buttonSecondary.text = getString(R.string.login_view_connected_stores)
            } else {
                hasConnectedStores = false

                // Hide "show connected stores"
                binding.loginEpilogueButtonBar.buttonSecondary.visibility = View.GONE
            }

            loginSiteUrl?.let {
                // hide the site list and validate the url if we already know the connected store, which will happen
                // if the user logged in by entering their store address
                binding.siteListContainer.visibility = View.GONE
                processLoginSite(it)
            }
            return
        }

        if (calledFromLogin) {
            unifiedLoginTracker.track(step = Step.SITE_LIST)

            // Show the 'try another account' button in case the user
            // doesn't see the store they want to log into.
            with(binding.loginEpilogueButtonBar.buttonSecondary) {
                visibility = View.VISIBLE
                text = getString(R.string.login_try_another_account)
                setOnClickListener {
                    presenter.logout()
                    if (calledFromLogin) {
                        unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                    }
                }
            }
        } else {
            // Called from settings. Hide the button.
            binding.loginEpilogueButtonBar.buttonSecondary.isVisible = false
        }

        AnalyticsTracker.track(
                Stat.SITE_PICKER_STORES_SHOWN,
                mapOf(AnalyticsTracker.KEY_NUMBER_OF_STORES to presenter.getWooCommerceSites().size)
        )

        binding.sitePickerRoot.visibility = View.VISIBLE

        if (wcSites.isEmpty()) {
            showNoStoresView()
            return
        }

        binding.noStoresView.noStoresView.visibility = View.GONE
        binding.siteListContainer.visibility = View.VISIBLE

        binding.siteListLabel.text = when {
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

        with(binding.loginEpilogueButtonBar.buttonPrimary) {
            text = getString(R.string.done)
            isEnabled = true
            setOnClickListener {
                presenter.getSiteBySiteId(siteAdapter.selectedSiteId)?.let { site -> siteSelected(site) }

                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.SUBMIT)
                }
            }
        }
    }

    override fun onSiteClick(siteId: Long) {
        clickedSiteId = siteId
        binding.loginEpilogueButtonBar.buttonPrimary.isEnabled = true
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
            // Track login flow completed successfully
            unifiedLoginTracker.track(step = Step.SUCCESS)

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
        binding.loginEpilogueButtonBar.buttonPrimary.isEnabled = siteAdapter.selectedSiteId != 0L

        WooUpgradeRequiredDialog().show(supportFragmentManager)
    }

    // BaseTransientBottomBar.LENGTH_LONG is pointing to Snackabr.LENGTH_LONG which confuses checkstyle
    @Suppress("WrongConstant")
    override fun siteVerificationError(site: SiteModel) {
        progressDialog?.dismiss()

        val siteName = if (!TextUtils.isEmpty(site.name)) site.name else getString(R.string.untitled)
        Snackbar.make(
                binding.sitePickerRoot as ViewGroup,
                getString(R.string.login_verifying_site_error, siteName),
                BaseTransientBottomBar.LENGTH_LONG
        ).show()
    }

    override fun showNoStoresView() {
        if (deferLoadingSitesIntoView) {
            return
        }

        if (calledFromLogin) {
            unifiedLoginTracker.track(step = Step.NO_WOO_STORES)
        }

        showUserInfo(centered = true)
        binding.sitePickerRoot.visibility = View.VISIBLE
        binding.siteListContainer.visibility = View.GONE
        binding.noStoresView.noStoresViewText.visibility = View.VISIBLE

        with(binding.noStoresView.btnSecondaryAction) {
            text = getString(R.string.login_jetpack_what_is)
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED)
                LoginWhatIsJetpackDialogFragment().show(supportFragmentManager, LoginWhatIsJetpackDialogFragment.TAG)
            }
            visibility = View.VISIBLE
        }

        with(binding.loginEpilogueButtonBar.buttonPrimary) {
            text = getString(R.string.login_jetpack_view_instructions_alt)
            isEnabled = true
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_VIEW_INSTRUCTIONS_BUTTON_TAPPED)
                ChromeCustomTabUtils.launchUrl(context, AppUrls.JETPACK_INSTRUCTIONS)
            }
        }

        with(binding.loginEpilogueButtonBar.buttonSecondary) {
            visibility = View.VISIBLE
            text = getString(R.string.login_try_another_account)
            isEnabled = true
            setOnClickListener {
                presenter.logout()

                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                }
            }
        }
    }

    override fun showSkeleton(show: Boolean) {
        if (deferLoadingSitesIntoView) {
            return
        }

        when (show) {
            true -> skeletonView.show(binding.sitesRecycler, R.layout.skeleton_site_picker, delayed = true)
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

        if (calledFromLogin) {
            unifiedLoginTracker.track(step = Step.WRONG_WP_ACCOUNT)
        }

        showUserInfo(centered = true)
        binding.sitePickerRoot.visibility = View.VISIBLE
        binding.noStoresView.noStoresViewText.visibility = View.VISIBLE
        binding.siteListContainer.visibility = View.GONE

        binding.noStoresView.noStoresViewText.text = getString(R.string.login_not_connected_to_account, url)

        with(binding.noStoresView.btnSecondaryAction) {
            text = getString(R.string.login_need_help_finding_email)
            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_HELP_FINDING_CONNECTED_EMAIL_LINK_TAPPED)
                unifiedLoginTracker.trackClick(Click.HELP_FINDING_CONNECTED_EMAIL)

                LoginEmailHelpDialogFragment().show(supportFragmentManager, LoginEmailHelpDialogFragment.TAG)
            }
            visibility = View.VISIBLE
        }

        with(binding.loginEpilogueButtonBar.buttonPrimary) {
            text = getString(R.string.login_try_another_account)
            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_ACCOUNT_BUTTON_TAPPED)
                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                }
                presenter.logout()
            }
        }

        with(binding.loginEpilogueButtonBar.buttonSecondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    if (calledFromLogin) {
                        unifiedLoginTracker.trackClick(Click.VIEW_CONNECTED_STORES)
                    }
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

        if (calledFromLogin) {
            unifiedLoginTracker.track(step = Step.JETPACK_NOT_CONNECTED)
        }

        showUserInfo(centered = true)
        binding.sitePickerRoot.visibility = View.VISIBLE
        binding.noStoresView.noStoresViewText.visibility = View.VISIBLE
        binding.siteListContainer.visibility = View.GONE

        with(binding.noStoresView.noStoresViewText) {
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

        with(binding.noStoresView.btnSecondaryAction) {
            text = getString(R.string.login_jetpack_what_is)
            setOnClickListener {
                AnalyticsTracker.track(Stat.LOGIN_JETPACK_REQUIRED_WHAT_IS_JETPACK_LINK_TAPPED)
                LoginWhatIsJetpackDialogFragment().show(supportFragmentManager, LoginWhatIsJetpackDialogFragment.TAG)
            }
            visibility = View.VISIBLE
        }

        with(binding.loginEpilogueButtonBar.buttonPrimary) {
            text = getString(R.string.login_try_another_store)
            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_STORE_BUTTON_TAPPED)
                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                }
                presenter.logout()
            }
        }

        with(binding.loginEpilogueButtonBar.buttonSecondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    if (calledFromLogin) {
                        unifiedLoginTracker.trackClick(Click.VIEW_CONNECTED_STORES)
                    }
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

        if (calledFromLogin) {
            unifiedLoginTracker.track(step = Step.NOT_WOO_STORE)
        }

        showUserInfo(centered = true)
        binding.sitePickerRoot.visibility = View.VISIBLE
        binding.noStoresView.noStoresView.visibility = View.VISIBLE
        binding.siteListContainer.visibility = View.GONE
        binding.noStoresView.btnSecondaryAction.visibility = View.GONE

        with(binding.noStoresView.noStoresViewText) {
            // Build and configure the error message and make part of the message
            // clickable. When clicked, we'll fetch a fresh copy of the active site from the API.
            val siteName = site.name.takeIf { !it.isNullOrEmpty() } ?: site.url
            val refreshAppText = getString(R.string.login_refresh_app)
            val notWooMessage = getString(R.string.login_not_woo_store, siteName, refreshAppText)

            val spannable = SpannableString(notWooMessage)
            spannable.setSpan(
                    WooClickableSpan {
                        AnalyticsTracker.track(Stat.SITE_PICKER_NOT_WOO_STORE_REFRESH_APP_LINK_TAPPED)
                        unifiedLoginTracker.trackClick(Click.REFRESH_APP)

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

        with(binding.loginEpilogueButtonBar.buttonPrimary) {
            text = getString(R.string.login_try_another_account)

            setOnClickListener {
                AnalyticsTracker.track(Stat.SITE_PICKER_TRY_ANOTHER_ACCOUNT_BUTTON_TAPPED)
                if (calledFromLogin) {
                    unifiedLoginTracker.trackClick(Click.TRY_ANOTHER_ACCOUNT)
                }
                presenter.logout()
            }
        }

        with(binding.loginEpilogueButtonBar.buttonSecondary) {
            visibility = if (hasConnectedStores) {
                text = getString(R.string.login_view_connected_stores)

                setOnClickListener {
                    AnalyticsTracker.track(Stat.SITE_PICKER_VIEW_CONNECTED_STORES_BUTTON_TAPPED)
                    if (calledFromLogin) {
                        unifiedLoginTracker.trackClick(Click.VIEW_CONNECTED_STORES)
                    }
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
