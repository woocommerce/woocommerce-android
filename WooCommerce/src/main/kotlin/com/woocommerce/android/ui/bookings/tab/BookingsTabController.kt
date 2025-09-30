package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.ui.main.BottomNavigationPosition
import com.woocommerce.android.ui.main.MainActivity
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val observeBookingsTabVisibility: ObserveBookingsTabVisibility
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

    private fun checkBookingsTabVisibility() {
        activity.lifecycleScope.launch {
            observeBookingsTabVisibility()
                .collect { isVisible ->
                    binding.bottomNav.menu.findItem(BottomNavigationPosition.BOOKINGS.id)?.isVisible = isVisible
                }
        }
    }
}
