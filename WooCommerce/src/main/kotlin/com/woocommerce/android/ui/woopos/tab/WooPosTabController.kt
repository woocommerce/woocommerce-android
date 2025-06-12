package com.woocommerce.android.ui.woopos.tab

import android.content.Intent
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.ui.NavigationUI
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.WooPosIsEnabled
import com.woocommerce.android.ui.woopos.root.WooPosActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

class WooPosTabController @Inject constructor(
    private val isWooPosEnabled: WooPosIsEnabled
) {
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
    }

    /**
     * Initializes the POS tab with default settings and navigation handling, updating its visibility.
     * Should be called once during activity setup (e.g., in onCreate).
     */
    fun setupPOSTab() {
        setPOSTabVisibility(false) // Hide by default
        updatePOSTabVisibility()
        setupPOSTabNavigation()
    }

    /**
     * Refreshes the visibility of the POS tab based on current conditions.
     * Call this when conditions that affect POS availability change, such as:
     * - User login/logout
     * - Store selection changes
     * - Feature flags updates
     */
    fun refreshPOSTabVisibility() {
        updatePOSTabVisibility()
    }

    private fun updatePOSTabVisibility() {
        activity.lifecycleScope.launch {
            val shouldShow = isWooPosEnabled()
            setPOSTabVisibility(shouldShow)
        }
    }

    private fun setPOSTabVisibility(isVisible: Boolean) {
        binding.bottomNav.menu.findItem(R.id.point_of_sale)?.isVisible = isVisible
    }

    private fun setupPOSTabNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.point_of_sale -> handlePOSTabSelection()
                else -> NavigationUI.onNavDestinationSelected(item, navController)
            }
        }
    }

    private fun handlePOSTabSelection(): Boolean {
        activity.startActivity(Intent(activity, WooPosActivity::class.java))
        return false // return false to *not* keep the tab selected
    }
}
