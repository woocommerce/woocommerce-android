package com.woocommerce.android.ui.woopos.tab

import android.content.Intent
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import com.woocommerce.android.AppPrefs
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.WooPosIsEnabled
import com.woocommerce.android.ui.woopos.root.WooPosActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosTabController @Inject constructor(
    private val appPrefs: AppPrefs,
    private val selectedSite: SelectedSite,
    private val isWooPosEnabled: WooPosIsEnabled,
    private val isPosAsTabEnabled: WooPosIsPosAsTabEnabled
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
    }

    override fun onCreate(owner: LifecycleOwner) {
        super.onCreate(owner)
        setupPOSTab()
    }

    override fun onResume(owner: LifecycleOwner) {
        super.onResume(owner)
        refreshPOSTabVisibility()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        super.onDestroy(owner)
        owner.lifecycle.removeObserver(this)
    }

    private fun setupPOSTab() {
        setPOSTabVisibility(false)
        if (isPosAsTabEnabled()) {
            setupPOSTabNavigation()
        }
    }

    fun refreshPOSTabVisibility() {
        setPOSTabVisibility(false)
        if (isPosAsTabEnabled()) {
            // Load visibility from prefs for fast UI feedback
            updatePOSTabVisibilityFromPrefs()

            // Then update with the remote value
            updateTabVisibilityFromRemoteAndPersist()
        }
    }

    private fun updatePOSTabVisibilityFromPrefs() = setPOSTabVisibility(
        appPrefs.isPOSTabVisibleForSite(selectedSite.getSelectedSiteId())
    )

    private fun updateTabVisibilityFromRemoteAndPersist() {
        activity.lifecycleScope.launch {
            val isWooPosEnabledValue = isWooPosEnabled()
            setPOSTabVisibility(isWooPosEnabledValue)
            appPrefs.setPOSTabVisibilityForSite(selectedSite.getSelectedSiteId(), isWooPosEnabledValue)
        }
    }

    private fun setPOSTabVisibility(isVisible: Boolean) {
        binding.bottomNav.menu.findItem(R.id.point_of_sale)?.isVisible = isVisible
    }

    private fun setupPOSTabNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.point_of_sale -> {
                    activity.startActivity(Intent(activity, WooPosActivity::class.java))
                    false // return false to *not* keep the tab selected
                }
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }
}
