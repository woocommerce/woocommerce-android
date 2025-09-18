package com.woocommerce.android.ui.bookings.tab

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.ui.main.MainActivity
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.launch
import javax.inject.Inject

class BookingsTabController @Inject constructor(
    private val showBookingsTab: ShowBookingsTab
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
            showBookingsTab()
                .onSuccess {
                    binding.bottomNav.menu.findItem(R.id.bookings)?.isVisible = it
                }
                .onFailure {
                    WooLog.w(WooLog.T.BOOKINGS, "Failed to check if bookings tab should be visible: ${it.message}")
                }
        }
    }
}
