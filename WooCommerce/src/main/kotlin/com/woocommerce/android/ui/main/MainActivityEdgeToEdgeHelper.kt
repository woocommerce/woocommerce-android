package com.woocommerce.android.ui.main

import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.extensions.doOnApplyWindowInsets
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(binding: ActivityMainBinding) {
        binding.root.doOnApplyWindowInsets(consumeInsets = true) { insets ->
            binding.appBarLayout.setPadding(0, insets.top, 0, 0)

            binding.root.updatePadding(left = insets.left, right = insets.right)

            if (binding.bottomNav.isVisible) {
                // When the bottom navigation is visible, apply the bottom padding to it to make sure its
                // background is drawn behind the system navigation bar
                binding.bottomNav.updatePadding(bottom = insets.bottom)
                binding.root.updatePadding(bottom = 0)
            } else {
                binding.root.updatePadding(bottom = insets.bottom)
            }
        }
    }
}
