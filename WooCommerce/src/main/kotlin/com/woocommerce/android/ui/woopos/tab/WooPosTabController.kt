package com.woocommerce.android.ui.woopos.tab

import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import com.woocommerce.android.ui.woopos.root.WooPosActivity
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEntryPointKeeper
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEventConstant.EntryPoint
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import kotlinx.coroutines.launch
import javax.inject.Inject

class WooPosTabController @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val shouldPosTabBeVisible: WooPosTabShouldBeVisible,
    private val analyticsTracker: WooPosAnalyticsTracker,
    private val analyticsEntryPointKeeper: WooPosAnalyticsEntryPointKeeper,
    private val wooPosLog: WooPosLogWrapper
) : DefaultLifecycleObserver {

    private lateinit var activity: MainActivity
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    fun initialize(
        activity: MainActivity,
        binding: ActivityMainBinding,
        navController: NavController
    ) {
        this.activity = activity
        this.binding = binding
        this.navController = navController

        activity.lifecycle.addObserver(this)
        setPOSTabVisibility(false)
    }

    override fun onResume(owner: LifecycleOwner) {
        refreshPOSTabVisibility()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    fun refreshPOSTabVisibility() {
        setPOSTabVisibility(false)
        updatePOSTabVisibilityFromPrefs()
        updateTabVisibilityFromRemoteAndPersist()
    }

    fun navigateToPOS() {
        analyticsEntryPointKeeper.onPosEntered(EntryPoint.POS_TAB)
        activity.startActivity(Intent(activity, WooPosActivity::class.java))
    }

    private fun updatePOSTabVisibilityFromPrefs() = setPOSTabVisibility(
        appPrefs.isPOSTabVisibleForSite(selectedSite.getSelectedSiteId())
    )

    private fun updateTabVisibilityFromRemoteAndPersist() {
        activity.lifecycleScope.launch {
            val result = shouldPosTabBeVisible(forceRefresh = true)

            result.onSuccess { tabShouldBeVisible ->
                setPOSTabVisibility(tabShouldBeVisible)
            }

            result.onFailure { error ->
                wooPosLog.i("POS Tab Visibility Value cannot be determined")
            }

            analyticsTracker.track(WooPosAnalyticsEvent.Event.TabVisibilityChecked(result))
        }
    }

    private fun setPOSTabVisibility(isVisible: Boolean) {
        binding.bottomNav.menu.findItem(R.id.point_of_sale)?.isVisible = isVisible
    }
}
