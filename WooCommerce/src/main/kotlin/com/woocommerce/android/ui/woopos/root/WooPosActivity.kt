package com.woocommerce.android.ui.woopos.root

import android.os.Bundle
import android.os.Build
import android.app.ActivityManager
import android.view.Window
import android.view.MotionEvent
import android.view.KeyEvent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import com.woocommerce.android.WooCommerce
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.viewModels
import androidx.activity.addCallback
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.home.items.products.WooPosProductsDataSource
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.launch
import com.woocommerce.android.ui.woopos.roles.lock.WooPosLockScreenViewModel
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosTheme
import com.woocommerce.android.ui.woopos.common.composeui.modifier.gesturesOrButtonsNavigationPadding
import com.woocommerce.android.ui.woopos.home.items.coupons.creation.WooPosCouponCreationFacade
import com.woocommerce.android.ui.woopos.support.WooPosGetSupportFacade
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEntryPointKeeper
import com.woocommerce.android.ui.woopos.util.ext.lockWooPosOrientation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WooPosActivity : AppCompatActivity() {
    @Inject
    lateinit var wooPosGetSupportFacade: WooPosGetSupportFacade

    @Inject
    lateinit var wooPosCouponCreationFacade: WooPosCouponCreationFacade

    @Inject
    lateinit var wooPosPeriodicSyncFacade: WooPosPeriodicSyncFacade

    @Inject
    lateinit var analyticsEntryPointKeeper: WooPosAnalyticsEntryPointKeeper

    @Inject lateinit var selectedSite: SelectedSite
    @Inject lateinit var featureFlags: FeatureFlagRepository
    @Inject lateinit var productsDataSource: WooPosProductsDataSource
    private var saleSiteId: Int? = null
    private var paymentActivityVisible = false
    private var observingVisibility = false
    private val paymentVisibility = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            paymentActivityVisible = intent.getBooleanExtra(WooCommerce.POS_PAYMENT_VISIBLE, false)
            if (paymentActivityVisible) lockViewModel.onForeground()
            else if (visibleActivities.isEmpty()) lockViewModel.onBackground()
        }
    }

    private val lockViewModel: WooPosLockScreenViewModel by viewModels()
    private val visibleActivities = mutableSetOf<Activity>()
    private val appVisibility = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (visibleActivities.isEmpty()) lockViewModel.onForeground()
            visibleActivities.add(activity)
            if (activity !== this@WooPosActivity) {
                activity.window.callback?.let { original ->
                    activity.window.callback = InteractionCallback(original, lockViewModel::onUserInteraction)
                }
            }
        }
        override fun onActivityStopped(activity: Activity) {
            restoreWindowCallback(activity)
            visibleActivities.remove(activity)
            if (visibleActivities.isEmpty() && !paymentActivityVisible && !hasVisiblePaymentTask() &&
                !activity.isChangingConfigurations) {
                lockViewModel.onBackground()
            }
        }
        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    private fun hasVisiblePaymentTask(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S_V2 &&
            getSystemService(ActivityManager::class.java).appTasks.any {
                it.taskInfo?.let { info -> info.taskId == taskId && info.isVisible } == true
            }

    override fun onCreate(savedInstanceState: Bundle?) {
        val discardSavedSale = intent.getBooleanExtra(DISCARD_SAVED_SALE, false)
        intent.removeExtra(DISCARD_SAVED_SALE)
        val savedSale = savedInstanceState?.takeIf {
            !discardSavedSale && it.getInt(SALE_SITE_ID) == SelectedSite.getSelectedSiteId(this) &&
                SelectedSite.hasSelectedSiteId(this)
        }
        super.onCreate(savedSale)
        saleSiteId = selectedSite.getOrNull()?.id
        onBackPressedDispatcher.addCallback(this) { }
        lifecycleScope.launch {
            featureFlags.awaitRemoteFlagsLoaded()
            val rolesEnabled = featureFlags.isEnabled(FeatureFlag.WOO_POS_ROLES)
            if (savedSale != null && (savedSale.getInt(SALE_SITE_ID) != saleSiteId ||
                (!rolesEnabled && !initializedProcess))) {
                intent.putExtra(DISCARD_SAVED_SALE, true)
                recreate()
                return@launch
            }
            initializedProcess = true
            val prepareSavedSale = savedSale != null && productsDataSource.getCurrentSyncStrategyOrNull() == null
            application.registerActivityLifecycleCallbacks(appVisibility)
            observingVisibility = true
            if (lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) {
                visibleActivities.add(this@WooPosActivity)
                lockViewModel.onForeground()
            }
            ContextCompat.registerReceiver(this@WooPosActivity, paymentVisibility, IntentFilter(WooCommerce.POS_PAYMENT_VISIBILITY_ACTION),
                ContextCompat.RECEIVER_NOT_EXPORTED)
            WindowCompat.setDecorFitsSystemWindows(window, false)
            lockWooPosOrientation()

            lifecycle.addObserver(wooPosGetSupportFacade)
            lifecycle.addObserver(wooPosCouponCreationFacade)
            lifecycle.addObserver(wooPosPeriodicSyncFacade)

            lifecycleScope.launch {
                selectedSite.observe().distinctUntilChangedBy { it?.id }.collect { site ->
                    if (site?.id != saleSiteId) finish()
                }
            }

            setContent {
                WooPosTheme {
                    WooPosRootScreen(
                        modifier = Modifier.gesturesOrButtonsNavigationPadding(),
                        lockViewModel = lockViewModel,
                        prepareSavedSale = prepareSavedSale,
                    )
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        saleSiteId?.let { outState.putInt(SALE_SITE_ID, it) }
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) lifecycleScope.launch {
            featureFlags.awaitRemoteFlagsLoaded()
            lockViewModel.onLauncherEntry()
        }
    }

    override fun onUserInteraction() {
        super.onUserInteraction()
        lockViewModel.onUserInteraction()
    }

    override fun onPause() {
        super.onPause()
        endPosSessionIfFinishing()
    }

    override fun onDestroy() {
        endPosSessionIfFinishing()
        if (observingVisibility) {
            visibleActivities.forEach(::restoreWindowCallback)
            application.unregisterActivityLifecycleCallbacks(appVisibility)
            unregisterReceiver(paymentVisibility)
        }
        super.onDestroy()
    }

    private fun endPosSessionIfFinishing() {
        if (isFinishing) analyticsEntryPointKeeper.onPosSessionEnded()
    }

    private fun restoreWindowCallback(activity: Activity) {
        (activity.window.callback as? InteractionCallback)?.let { activity.window.callback = it.original }
    }

    private class InteractionCallback(
        val original: Window.Callback,
        private val onInteraction: () -> Unit,
    ) : Window.Callback by original {
        override fun dispatchTouchEvent(event: MotionEvent): Boolean {
            onInteraction()
            return original.dispatchTouchEvent(event)
        }
        override fun dispatchKeyEvent(event: KeyEvent): Boolean {
            onInteraction()
            return original.dispatchKeyEvent(event)
        }
    }

    companion object {
        private var initializedProcess = false
        private const val SALE_SITE_ID = "pos_sale_site_id"
        private const val DISCARD_SAVED_SALE = "discard_pos_saved_sale"
    }
}
