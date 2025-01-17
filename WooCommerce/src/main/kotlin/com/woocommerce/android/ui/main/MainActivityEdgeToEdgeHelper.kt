package com.woocommerce.android.ui.main

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(viewToApplyPadding: View) {
        ViewCompat.setOnApplyWindowInsetsListener(viewToApplyPadding) { v, insets ->
            val innerPadding = insets.getInsets(
                WindowInsetsCompat.Type.systemBars()
                    or WindowInsetsCompat.Type.displayCutout()
            )
            v.setPadding(0, innerPadding.top, 0, 0)
            insets
        }
    }
}
