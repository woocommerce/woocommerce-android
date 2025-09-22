package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val observeBookingsTabVisibility: ObserveBookingsTabVisibility,
    private val selectedSite: SelectedSite
) : DefaultLifecycleObserver {
    private lateinit var activity: MainActivity
    private lateinit var binding: ActivityMainBinding

    fun init(
        activity: MainActivity,
        binding: ActivityMainBinding
    ) {
        this.activity = activity
        this.binding = binding
        activity.lifecycle.addObserver(this)
    }

    override fun onResume(owner: LifecycleOwner) {
        checkBookingsTabVisibility()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        owner.lifecycle.removeObserver(this)
    }

    private fun checkBookingsTabVisibility() {
        activity.lifecycleScope.launch {
            selectedSite.observe()
                .filterNotNull()
                .collect { siteModel ->
                    observeBookingsTabVisibility(siteModel!!)
                        .collect {
                            binding.bottomNav.menu.findItem(R.id.bookings)?.isVisible = it
                        }
                }
        }
    }
}
