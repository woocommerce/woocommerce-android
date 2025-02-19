package com.woocommerce.android.ui.main

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.ActivityMainBinding
import com.woocommerce.android.util.SystemVersionUtils
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(binding: ActivityMainBinding) {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemInsets = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )

            val isBottomNavVisible = binding.bottomNav.isVisible
            // Apply bottom padding only if bottom nav is hidden
            // because bottom nav is already handling the padding
            // Note: this doesn't work well on Android Q and below, so we apply it only on Android R and above
            // TODO: Remove this when Androidx Core v1.16 is released by using
            //  ViewGroupCompat#installCompatInsetsDispatch
            //  see: https://developer.android.com/develop/ui/views/layout/edge-to-edge#backward-compatible-dispatching
            val bottomPadding = if (SystemVersionUtils.isAtLeastR() && isBottomNavVisible) 0 else systemInsets.bottom

            v.setPadding(systemInsets.left, systemInsets.top, systemInsets.right, bottomPadding)

            insets
        }
    }
}
