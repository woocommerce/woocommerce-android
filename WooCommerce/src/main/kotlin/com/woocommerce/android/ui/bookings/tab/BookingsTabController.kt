package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.ui.main.BottomNavigationPosition
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.ui.woopos.tab.WooPosTabShouldBeVisible
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val observeBookingsVisibility: ObserveBookingsVisibility,
    private val shouldPosTabBeVisible: WooPosTabShouldBeVisible
) {
    private lateinit var activity: MainActivity
    private lateinit var binding: ActivityMainBinding

    fun init(
        activity: MainActivity,
        binding: ActivityMainBinding
    ) {
        this.activity = activity
        this.binding = binding

        binding.bottomNav.menu.findItem(BottomNavigationPosition.BOOKINGS.id)?.isVisible = false

        checkBookingsTabVisibility()
    }

    private fun checkBookingsTabVisibility() {
        activity.lifecycleScope.launch {
            val isPosVisible = shouldPosTabBeVisible().getOrDefault(false)
            observeBookingsVisibility()
                .collect { isVisible ->
                    binding.bottomNav.menu.findItem(BottomNavigationPosition.BOOKINGS.id)?.isVisible =
                        isVisible && !isPosVisible
                }
        }
    }
}
