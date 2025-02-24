package com.woocommerce.android.ui.main

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.woocommerce.android.databinding.ActivityMainBinding
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(binding: ActivityMainBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            binding.appBarLayout.setPadding(0, systemInsets.top, 0, 0)

            binding.root.updatePadding(left = systemInsets.left, right = systemInsets.right)

            if (binding.bottomNav.isVisible) {
                // When the bottom navigation is visible, apply the bottom padding to it to make sure its
                // background is drawn behind the system navigation bar
                binding.bottomNav.updatePadding(bottom = systemInsets.bottom)
                binding.root.updatePadding(bottom = 0)
            } else {
                binding.root.updatePadding(bottom = systemInsets.bottom)
            }

            // Prevent other views from consuming the insets, including the bottom navigation
            WindowInsetsCompat.CONSUMED
        }
    }
}
