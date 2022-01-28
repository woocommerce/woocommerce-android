package com.woocommerce.android.ui.main

import android.animation.ValueAnimator
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.content.res.Resources.Theme
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.Toolbar
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.appbar.AppBarLayout
import com.woocommerce.android.*
import com.woocommerce.android.R.dimen
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.extensions.active
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.model.Notification
import com.woocommerce.android.support.HelpActivity
import com.woocommerce.android.support.HelpActivity.Origin
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.TopLevelFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.feedback.SurveyType
import com.woocommerce.android.ui.login.LoginActivity
import com.woocommerce.android.ui.main.BottomNavigationPosition.*
import com.woocommerce.android.ui.main.MainActivityViewModel.*
import com.woocommerce.android.ui.orders.list.OrderListFragmentDirections
import com.woocommerce.android.ui.prefs.AppSettingsActivity
import com.woocommerce.android.ui.products.ProductListFragmentDirections
import com.woocommerce.android.ui.reviews.ReviewListFragmentDirections
import com.woocommerce.android.ui.sitepicker.SitePickerActivity
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.AppRatingDialog
import com.woocommerce.android.widgets.DisabledAppBarLayoutBehavior
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.login.LoginAnalyticsListener
import org.wordpress.android.login.LoginMode
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject
import kotlin.math.abs

// TODO Extract logic out of MainActivity to reduce size
@Suppress("LargeClass")
@AndroidEntryPoint
class MainActivity :
    AppUpgradeActivity(),
    MainContract.View,
    MainNavigationRouter,
    MainBottomNavigationView.MainNavigationListener,
    NavController.OnDestinationChangedListener {
    companion object {
        private const val MAGIC_LOGIN = "magic-login"

        private const val KEY_BOTTOM_NAV_POSITION = "key-bottom-nav-position"
        private const val KEY_UNFILLED_ORDER_COUNT = "unfilled-order-count"

        private const val DIALOG_NAVIGATOR_NAME = "dialog"

        // push notification-related constants
        const val FIELD_OPENED_FROM_PUSH = "opened-from-push-notification"
        const val FIELD_REMOTE_NOTIFICATION = "remote-notification"
        const val FIELD_PUSH_ID = "local-push-id"

        interface BackPressListener {
            fun onRequestAllowBackPress(): Boolean
        }

        init {
            AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        }
    }

    @Inject lateinit var presenter: MainContract.Presenter
    @Inject lateinit var loginAnalyticsListener: LoginAnalyticsListener
    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: MainActivityViewModel by viewModels()

    private var isBottomNavShowing = true
    private var previousDestinationId: Int? = null
    private var unfilledOrderCount: Int = 0
    private var isMainThemeApplied = false
    private var restoreToolbarHeight = 0
    private var menu: Menu? = null

    private val toolbarEnabledBehavior = AppBarLayout.Behavior()
    private val toolbarDisabledBehavior = DisabledAppBarLayoutBehavior()

    private lateinit var navController: NavController

    private lateinit var binding: ActivityMainBinding
    private lateinit var toolbar: Toolbar

    private val appBarOffsetListener by lazy {
        AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.toolbarSubtitle.alpha = ((1.0f - abs((verticalOffset / appBarLayout.totalScrollRange.toFloat()))))
        }
    }

    private val showSubtitleAnimator by lazy {
        createCollapsingToolbarMarginBottomAnimator(
            from = resources.getDimensionPixelSize(dimen.expanded_toolbar_bottom_margin),
            to = resources.getDimensionPixelSize(dimen.expanded_toolbar_bottom_margin_with_subtitle),
            duration = 200L
        )
    }

    private val hideSubtitleAnimator by lazy {
        createCollapsingToolbarMarginBottomAnimator(
            from = resources.getDimensionPixelSize(dimen.expanded_toolbar_bottom_margin_with_subtitle),
            to = resources.getDimensionPixelSize(dimen.expanded_toolbar_bottom_margin),
            duration = 200L
        )
    }

    // TODO: Using deprecated ProgressDialog temporarily - a proper post-login experience will replace this
    private var progressDialog: ProgressDialog? = null

    private val fragmentLifecycleObserver: FragmentLifecycleCallbacks = object : FragmentLifecycleCallbacks() {
        override fun onFragmentViewCreated(fm: FragmentManager, f: Fragment, v: View, savedInstanceState: Bundle?) {
            val currentDestination = navController.currentDestination!!
            val isFullScreenFragment = currentDestination.id == R.id.productImageViewerFragment
            val isDialogDestination = currentDestination.navigatorName == DIALOG_NAVIGATOR_NAME

            if (!isFullScreenFragment && !isDialogDestination) {
                // re-expand the AppBar when returning to top level fragment, collapse it when entering a child fragment
                if (f is TopLevelFragment) {
                    // We need to post this to the view handler to make sure shouldExpandToolbar returns the correct value
                    f.view?.post {
                        if (f.view != null) {
                            expandToolbar(expand = f.shouldExpandToolbar(), animate = false)
                        }
                    }
                } else {
                    expandToolbar(expand = false, animate = false)
                }

                // collapsible toolbar should only be able to expand for top-level fragments
                enableToolbarExpansion(f is TopLevelFragment)
            }
        }
    }

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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        this.menu = menu
        return super.onCreateOptionsMenu(menu)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verify authenticated session
        if (!presenter.userIsLoggedIn()) {
            showLoginScreen()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        toolbar = binding.toolbar.toolbar
        setSupportActionBar(toolbar)
        toolbar.navigationIcon = null

        presenter.takeView(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        navController = navHostFragment.navController
        navController.addOnDestinationChangedListener(this@MainActivity)
        navHostFragment.childFragmentManager.registerFragmentLifecycleCallbacks(fragmentLifecycleObserver, false)
        binding.bottomNav.init(navController, this)

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

        if (!presenter.isUserEligible()) {
            showUserEligibilityErrorScreen()
            return
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

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)

        updateReviewsBadge()
        updateOrderBadge(false)

        checkConnection()
        viewModel.showFeatureAnnouncementIfNeeded()
    }

    override fun onPause() {
        binding.appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
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
        outState.putInt(KEY_BOTTOM_NAV_POSITION, binding.bottomNav.currentPosition.id)
        outState.putInt(KEY_UNFILLED_ORDER_COUNT, unfilledOrderCount)
        super.onSaveInstanceState(outState)
    }

    private fun restoreSavedInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.also {
            val id = it.getInt(KEY_BOTTOM_NAV_POSITION, MY_STORE.id)
            binding.bottomNav.restoreSelectedItemState(id)

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
        } else {
            super.onBackPressed()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Returns true if the navigation controller is showing the root fragment (ie: a top level fragment is showing)
     */
    override fun isAtNavigationRoot(): Boolean {
        return if (::navController.isInitialized) {
            val currentDestinationId = navController.currentDestination?.id
            currentDestinationId == R.id.dashboard ||
                currentDestinationId == R.id.orders ||
                currentDestinationId == R.id.products ||
                currentDestinationId == R.id.reviews ||
                currentDestinationId == R.id.analytics
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
     * Returns the current top level fragment (ie: the one showing in the bottom nav)
     */
    private fun getActiveTopLevelFragment(): TopLevelFragment? {
        val tag = binding.bottomNav.currentPosition.getTag()
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_main) as NavHostFragment
        return navHostFragment.childFragmentManager.findFragmentByTag(tag) as? TopLevelFragment
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

    /**
     * Get the actual primary navigation Fragment from the support manager
     */
    private fun getHostChildFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.primaryNavigationFragment
        if (navHostFragment?.childFragmentManager?.fragments?.isNotEmpty() == true) {
            return navHostFragment.childFragmentManager.fragments[0]
        }
        return null
    }

    /**
     * The current fragment in the nav controller has changed
     */
    override fun onDestinationChanged(controller: NavController, destination: NavDestination, arguments: Bundle?) {
        val isAtRoot = isAtNavigationRoot()
        val isTopLevelNavigation = isAtTopLevelNavigation(isAtRoot = isAtRoot, destination = destination)

        // go no further if this is the initial navigation to the root fragment, or if the destination is
        // a dialog (since we don't need to change anything for dialogs)
        if ((isAtRoot && previousDestinationId == null) || isDialogDestination(destination)) {
            previousDestinationId = destination.id
            return
        }

        val showCrossIcon: Boolean
        if (isTopLevelNavigation) {
            if (destination.id != R.id.dashboard) {
                // MyStoreFragment handle the elevation by themselves
                binding.appBarLayout.elevation = 0f
            }
            showCrossIcon = false
        } else {
            binding.appBarLayout.elevation =
                resources.getDimensionPixelSize(R.dimen.appbar_elevation).toFloat()

            showCrossIcon = when (destination.id) {
                R.id.productFilterListFragment,
                R.id.issueRefundFragment,
                R.id.addOrderShipmentTrackingFragment,
                R.id.addOrderNoteFragment,
                R.id.printShippingLabelInfoFragment,
                R.id.shippingLabelFormatOptionsFragment,
                R.id.printingInstructionsFragment,
                R.id.editCustomerOrderNoteFragment,
                R.id.shippingAddressEditingFragment,
                R.id.billingAddressEditingFragment,
                R.id.orderFilterCategoriesFragment,
                R.id.orderCreationProductDetailsFragment -> {
                    true
                }
                R.id.productDetailFragment -> {
                    // show Cross icon only when product detail isn't opened from the product list
                    binding.bottomNav.currentPosition != PRODUCTS
                }
                else -> {
                    false
                }
            }
        }

        if (isAtRoot) {
            toolbar.navigationIcon = null
        } else if (showCrossIcon) {
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_gridicons_cross_24dp)
        } else {
            toolbar.navigationIcon = ContextCompat.getDrawable(this, R.drawable.ic_back_24dp)
        }

        val isFullScreenFragment = destination.id == R.id.productImageViewerFragment

        supportActionBar?.let {
            // the image viewers should be shown full screen
            if (isFullScreenFragment) {
                window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                restoreToolbarHeight = binding.collapsingToolbar.layoutParams.height
                binding.collapsingToolbar.layoutParams.height = 0
            } else {
                window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
                if (restoreToolbarHeight > 0) {
                    binding.collapsingToolbar.layoutParams.height = restoreToolbarHeight
                    restoreToolbarHeight = 0
                }
            }
        }

        // show bottom nav if this is a dialog destination from root or, just root itself
        if (isTopLevelNavigation) {
            showBottomNav()
        } else {
            hideBottomNav()
        }

        previousDestinationId = destination.id
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        binding.collapsingToolbar.title = title
    }

    fun expandToolbar(expand: Boolean, animate: Boolean) {
        binding.appBarLayout.setExpanded(expand, animate)
    }

    fun setSubtitle(subtitle: CharSequence) {
        if (subtitle.isBlank()) {
            removeSubtitle()
        } else {
            setFadingSubtitleOnCollapsingToolbar(subtitle)
        }
    }

    private fun createCollapsingToolbarMarginBottomAnimator(from: Int, to: Int, duration: Long): ValueAnimator {
        return ValueAnimator.ofInt(from, to)
            .also { valueAnimator ->
                valueAnimator.duration = duration
                valueAnimator.interpolator = AccelerateDecelerateInterpolator()
                valueAnimator.addUpdateListener {
                    binding.collapsingToolbar.expandedTitleMarginBottom = it.animatedValue as Int
                }
            }
    }

    private fun removeSubtitle() {
        binding.appBarLayout.removeOnOffsetChangedListener(appBarOffsetListener)
        if (binding.toolbarSubtitle.visibility == View.GONE) return
        binding.toolbarSubtitle.collapse(duration = 200L)
        hideSubtitleAnimator.start()
    }

    private fun setFadingSubtitleOnCollapsingToolbar(subtitle: CharSequence) {
        binding.appBarLayout.addOnOffsetChangedListener(appBarOffsetListener)
        binding.toolbarSubtitle.text = subtitle
        if (binding.toolbarSubtitle.visibility == View.VISIBLE) return
        binding.toolbarSubtitle.expand(duration = 200L)
        showSubtitleAnimator.start()
    }

    fun enableToolbarExpansion(enable: Boolean) {
        if (!enable) {
            toolbar.title = title
        }
        binding.collapsingToolbar.isTitleEnabled = enable

        val params = (binding.appBarLayout.layoutParams as CoordinatorLayout.LayoutParams)
        params.behavior = if (enable) {
            toolbarEnabledBehavior
        } else {
            toolbarDisabledBehavior
        }
    }

    /**
     * Returns a Boolean value in order to set the behaviour from a root navigation type in terms of:
     * .menu items visibility
     * .top nav bar titles
     *
     * @param isAtRoot The value that tells if root fragment is in the current destination
     * @param destination The object for the next navigation destination
     */
    private fun isAtTopLevelNavigation(isAtRoot: Boolean, destination: NavDestination): Boolean {
        val activeChild = getHostChildFragment()
        val activeChildIsRoot = activeChild != null && activeChild is TopLevelFragment
        return (isDialogDestination(destination) && activeChildIsRoot) || isAtRoot
    }

    private fun isDialogDestination(destination: NavDestination) = destination.navigatorName == DIALOG_NAVIGATOR_NAME

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

    override fun showUserEligibilityErrorScreen() {
        val action = NavGraphMainDirections.actionGlobalUserEligibilityErrorFragment()
        navController.navigateSafely(action)
    }

    override fun showSettingsScreen() {
        AnalyticsTracker.track(Stat.MAIN_MENU_SETTINGS_TAPPED)
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
        binding.bottomNav.init(navController, this)
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
        val host = uri?.host ?: ""
        return Intent.ACTION_VIEW == action && host.contains(MAGIC_LOGIN)
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
        binding.bottomNav.showReviewsBadge(false)
        viewModel.removeReviewNotifications()
    }

    override fun showReviewsBadge() {
        binding.bottomNav.showReviewsBadge(true)
    }

    override fun updateOrderBadge(hideCountUntilComplete: Boolean) {
        if (hideCountUntilComplete) {
            binding.bottomNav.clearOrderBadgeCount()
        }
        presenter.fetchUnfilledOrderCount()
    }

    override fun showOrderBadge(count: Int) {
        unfilledOrderCount = count
        binding.bottomNav.setOrderBadgeCount(count)
    }

    override fun hideOrderBadge() {
        unfilledOrderCount = 0
        binding.bottomNav.setOrderBadgeCount(0)
    }

    override fun onNavItemSelected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> Stat.MAIN_TAB_DASHBOARD_SELECTED
            ANALYTICS -> Stat.MAIN_TAB_ANALYTICS_SELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_SELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_SELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_SELECTED
        }
        AnalyticsTracker.track(stat)

        if (navPos == REVIEWS) {
            viewModel.removeReviewNotifications()
        } else if (navPos == ORDERS) {
            viewModel.removeOrderNotifications()
        }
    }

    override fun onNavItemReselected(navPos: BottomNavigationPosition) {
        val stat = when (navPos) {
            MY_STORE -> Stat.MAIN_TAB_DASHBOARD_RESELECTED
            ANALYTICS -> Stat.MAIN_TAB_ANALYTICS_RESELECTED
            ORDERS -> Stat.MAIN_TAB_ORDERS_RESELECTED
            PRODUCTS -> Stat.MAIN_TAB_PRODUCTS_RESELECTED
            REVIEWS -> Stat.MAIN_TAB_NOTIFICATIONS_RESELECTED
        }
        AnalyticsTracker.track(stat)

        // if we're at the root scroll the active fragment to the top, otherwise clear the nav backstack
        if (isAtNavigationRoot()) {
            // If the fragment's view is not yet created, do nothing
            if (getActiveTopLevelFragment()?.view != null) {
                getActiveTopLevelFragment()?.scrollToTop()
                expandToolbar(expand = true, animate = true)
            }
        } else {
            navController.navigate(binding.bottomNav.currentPosition.id)
        }
    }
    // endregion

    // region Fragment Processing
    private fun initFragment(savedInstanceState: Bundle?) {
        setupObservers()
        val openedFromPush = intent.getBooleanExtra(FIELD_OPENED_FROM_PUSH, false)
        // Reset this flag now that it's being processed
        intent.removeExtra(FIELD_OPENED_FROM_PUSH)

        if (savedInstanceState != null) {
            restoreSavedInstanceState(savedInstanceState)
        } else if (openedFromPush) {
            // Opened from a push notification
            menu?.close()

            val localPushId = intent.getIntExtra(FIELD_PUSH_ID, 0)
            val notification = intent.getParcelableExtra<Notification>(FIELD_REMOTE_NOTIFICATION)
            // Reset this flag now that it's being processed
            intent.removeExtra(FIELD_REMOTE_NOTIFICATION)
            intent.removeExtra(FIELD_PUSH_ID)

            viewModel.handleIncomingNotification(localPushId, notification)
        }
    }
    // endregion

    private fun setupObservers() {
        viewModel.event.observe(
            this,
            { event ->
                when (event) {
                    is ViewMyStoreStats -> binding.bottomNav.currentPosition = MY_STORE
                    is ViewOrderList -> binding.bottomNav.currentPosition = ORDERS
                    is ViewReviewList -> binding.bottomNav.currentPosition = REVIEWS
                    is ViewZendeskTickets -> {
                        binding.bottomNav.currentPosition = MY_STORE
                        startActivity(HelpActivity.createIntent(this, Origin.ZENDESK_NOTIFICATION, null))
                    }
                    is ViewOrderDetail -> {
                        showOrderDetail(
                            orderId = event.uniqueId,
                            remoteNoteId = event.remoteNoteId,
                            launchedFromNotification = true
                        )
                    }
                    is ViewReviewDetail -> {
                        showReviewDetail(event.uniqueId, launchedFromNotification = true, enableModeration = true)
                    }
                    is RestartActivityForNotification -> {
                        // Add flags for handling the push notification after restart
                        intent.putExtra(FIELD_OPENED_FROM_PUSH, true)
                        intent.putExtra(FIELD_REMOTE_NOTIFICATION, event.notification)
                        intent.putExtra(FIELD_PUSH_ID, event.pushId)
                        restart()
                    }
                    is ShowFeatureAnnouncement -> {
                        val action = NavGraphMainDirections.actionOpenWhatsnewFromMain(event.announcement)
                        navController.navigateSafely(action)
                    }
                }
            }
        )
    }

    override fun showProductDetail(remoteProductId: Long, enableTrash: Boolean) {
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            remoteProductId = remoteProductId,
            isTrashEnabled = enableTrash
        )
        navController.navigateSafely(action)
    }

    override fun showProductDetailWithSharedTransition(remoteProductId: Long, sharedView: View, enableTrash: Boolean) {
        val productCardDetailTransitionName = getString(R.string.product_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to productCardDetailTransitionName)

        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(
            remoteProductId = remoteProductId,
            isTrashEnabled = enableTrash
        )
        navController.navigateSafely(directions = action, extras = extras)
    }

    override fun showProductVariationDetail(remoteProductId: Long, remoteVariationId: Long) {
        // variation detail is part of the products navigation graph, and product detail is the starting destination
        // for that graph, so we have to use a deep link to navigate to variation detail
        val query = "?remoteProductId=$remoteProductId&remoteVariationId=$remoteVariationId"
        val deeplink = "wcandroid://variationDetail$query"
        navController.navigate(Uri.parse(deeplink))
    }

    override fun showAddProduct() {
        showBottomNav()
        val action = NavGraphMainDirections.actionGlobalProductDetailFragment(isAddProduct = true)
        navController.navigateSafely(action)
    }

    override fun showReviewDetail(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        tempStatus: String?
    ) {
        // make sure the review tab is active if the user came here from a notification
        if (launchedFromNotification) {
            showBottomNav()
            binding.bottomNav.currentPosition = REVIEWS
            binding.bottomNav.active(REVIEWS.position)
        }

        val action = ReviewListFragmentDirections.actionReviewListFragmentToReviewDetailFragment(
            remoteReviewId = remoteReviewId,
            tempStatus = tempStatus,
            launchedFromNotification = launchedFromNotification,
            enableModeration = enableModeration
        )
        navController.navigateSafely(action)
    }

    override fun showReviewDetailWithSharedTransition(
        remoteReviewId: Long,
        launchedFromNotification: Boolean,
        enableModeration: Boolean,
        sharedView: View,
        tempStatus: String?
    ) {
        val reviewCardDetailTransitionName = getString(R.string.review_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to reviewCardDetailTransitionName)
        val action = ReviewListFragmentDirections.actionReviewListFragmentToReviewDetailFragment(
            remoteReviewId = remoteReviewId,
            tempStatus = tempStatus,
            launchedFromNotification = launchedFromNotification,
            enableModeration = enableModeration
        )
        navController.navigateSafely(directions = action, extras = extras)
    }

    override fun showProductFilters(
        stockStatus: String?,
        productType: String?,
        productStatus: String?,
        productCategory: String?,
        productCategoryName: String?
    ) {
        val action = ProductListFragmentDirections.actionProductListFragmentToProductFilterListFragment(
            selectedStockStatus = stockStatus,
            selectedProductStatus = productStatus,
            selectedProductType = productType,
            selectedProductCategoryId = productCategory,
            selectedProductCategoryName = productCategoryName
        )
        navController.navigateSafely(action)
    }

    override fun showOrderDetail(
        orderId: Long,
        remoteNoteId: Long,
        launchedFromNotification: Boolean
    ) {
        if (launchedFromNotification) {
            showBottomNav()
            binding.bottomNav.currentPosition = ORDERS
            binding.bottomNav.active(ORDERS.position)
        }

        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(orderId, remoteNoteId)
        navController.navigateSafely(action)
    }

    override fun showOrderDetailWithSharedTransition(
        orderId: Long,
        remoteNoteId: Long,
        sharedView: View
    ) {
        val orderCardDetailTransitionName = getString(R.string.order_card_detail_transition_name)
        val extras = FragmentNavigatorExtras(sharedView to orderCardDetailTransitionName)

        val action = OrderListFragmentDirections.actionOrderListFragmentToOrderDetailFragment(orderId, remoteNoteId)
        navController.navigateSafely(directions = action, extras = extras)
    }

    override fun showFeedbackSurvey() {
        NavGraphMainDirections.actionGlobalFeedbackSurveyFragment(SurveyType.MAIN).apply {
            navController.navigateSafely(this)
        }
    }

    override fun updateOfflineStatusBar(isConnected: Boolean) {
        if (isConnected) binding.offlineBar.hide() else binding.offlineBar.show()
    }

    private fun checkConnection() {
        updateOfflineStatusBar(NetworkUtils.isNetworkAvailable(this))
    }

    override fun hideBottomNav() {
        if (isBottomNavShowing) {
            isBottomNavShowing = false
            WooAnimUtils.animateBottomBar(binding.bottomNav, false, Duration.MEDIUM)
        }
    }

    override fun showBottomNav() {
        if (!isBottomNavShowing) {
            isBottomNavShowing = true
            WooAnimUtils.animateBottomBar(binding.bottomNav, true, Duration.SHORT)
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
