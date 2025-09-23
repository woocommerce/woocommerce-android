package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.main.MainActivity
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val observeBookingsTabVisibility: ObserveBookingsTabVisibility,
    private val selectedSite: SelectedSite
) {
    private lateinit var activity: MainActivity
    private lateinit var binding: ActivityMainBinding

    fun init(
        activity: MainActivity,
        binding: ActivityMainBinding
    ) {
        this.activity = activity
        this.binding = binding
        checkBookingsTabVisibility()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun checkBookingsTabVisibility() {
        activity.lifecycleScope.launch {
            selectedSite.observe()
            .filterNotNull()
            .flatMapLatest { siteModel ->
                observeBookingsTabVisibility(siteModel)
            }
            .collect { isVisible ->
                binding.bottomNav.menu.findItem(R.id.bookings)?.isVisible = isVisible
            }
        }
    }
}
