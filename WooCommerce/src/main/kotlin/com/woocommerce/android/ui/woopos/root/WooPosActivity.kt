package com.woocommerce.android.ui.woopos.root

import android.os.Bundle
import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.activity.viewModels
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
    @Inject lateinit var productsDataSource: WooPosProductsDataSource
    private var saleSiteURL: String? = null

    private val lockViewModel: WooPosLockScreenViewModel by viewModels()
    private val visibleActivities = mutableSetOf<Activity>()
    private val appVisibility = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            if (visibleActivities.isEmpty()) lockViewModel.onForeground()
            visibleActivities.add(activity)
        }
        override fun onActivityStopped(activity: Activity) {
            visibleActivities.remove(activity)
            if (visibleActivities.isEmpty() && !activity.isChangingConfigurations) lockViewModel.onBackground()
        }
        override fun onActivityCreated(activity: Activity, state: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, state: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        saleSiteURL = selectedSite.getOrNull()?.url
        val savedSale = savedInstanceState?.takeIf { it.getString(SALE_SITE_URL) == saleSiteURL && saleSiteURL != null }
        val prepareSavedSale = savedSale != null && productsDataSource.getCurrentSyncStrategyOrNull() == null
        super.onCreate(savedSale)
        application.registerActivityLifecycleCallbacks(appVisibility)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        lockWooPosOrientation()

        lifecycle.addObserver(wooPosGetSupportFacade)
        lifecycle.addObserver(wooPosCouponCreationFacade)
        lifecycle.addObserver(wooPosPeriodicSyncFacade)

        lifecycleScope.launch {
            selectedSite.observe().distinctUntilChangedBy { it?.url }.collect { site ->
                if (site?.url != saleSiteURL) {
                    startActivity(Intent(this@WooPosActivity, WooPosActivity::class.java))
                    finish()
                }
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

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(SALE_SITE_URL, saleSiteURL)
        super.onSaveInstanceState(outState)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.action == Intent.ACTION_MAIN) lockViewModel.onLauncherEntry()
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
        application.unregisterActivityLifecycleCallbacks(appVisibility)
        super.onDestroy()
    }

    private fun endPosSessionIfFinishing() {
        if (isFinishing) analyticsEntryPointKeeper.onPosSessionEnded()
    }

    companion object {
        private const val SALE_SITE_URL = "pos_sale_site_url"
    }
}
