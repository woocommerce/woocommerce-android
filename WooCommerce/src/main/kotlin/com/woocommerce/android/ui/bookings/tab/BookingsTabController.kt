package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.extensions.isTwoPanesShouldBeUsed
import com.woocommerce.android.ui.main.BottomNavigationPosition
import com.woocommerce.android.ui.main.MainActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val observeBookingsVisibility: ObserveBookingsVisibility
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
        // On large screens, Bookings is accessible from the More Menu instead of a bottom tab.
        if (activity.isTwoPanesShouldBeUsed) {
            return
        }

        activity.lifecycleScope.launch {
            observeBookingsVisibility()
                .collect { isVisible ->
                    binding.bottomNav.menu.findItem(BottomNavigationPosition.BOOKINGS.id)?.isVisible = isVisible
                }
        }
    }
}
