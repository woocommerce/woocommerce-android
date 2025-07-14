package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import androidx.navigation.NavController
import androidx.navigation.findNavController

@Composable
fun rememberNavController(): NavController {
    val view = LocalView.current

    return remember(view) {
        view.findNavController()
    }
}
