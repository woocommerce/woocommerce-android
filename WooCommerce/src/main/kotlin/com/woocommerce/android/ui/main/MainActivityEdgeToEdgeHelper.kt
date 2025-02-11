package com.woocommerce.android.ui.main

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.woocommerce.android.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(binding: ActivityMainBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.appBarLayout) { v, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            v.setPadding(0, systemInsets.top, 0, 0)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.snackRoot) { v, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            val isBottomNavVisible = binding.bottomNav.visibility == View.VISIBLE

            // Apply bottom padding only if bottom nav is hidden
            // because bottom nav is already handling the padding
            val bottomPadding = if (isBottomNavVisible) 0 else systemInsets.bottom

            v.setPadding(systemInsets.left, 0, systemInsets.right, bottomPadding)
            insets
        }
    }
}
