package com.woocommerce.android.ui.compose

import androidx.annotation.PluralsRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun pluralsResource(@PluralsRes id: Int, quantity: Int) =
    LocalContext.current.resources.getQuantityString(id, quantity, quantity)
