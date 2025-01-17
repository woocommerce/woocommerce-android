package com.woocommerce.android.ui.main

import android.graphics.Color
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import javax.inject.Inject

class MainActivityEdgeToEdgeHelper @Inject constructor() {
    fun applyEdgeToEdgeSettings(viewToApplyPadding: View) {
        (viewToApplyPadding.context as ComponentActivity).enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT)
        )

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
