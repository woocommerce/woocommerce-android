package com.woocommerce.android.ui.main

import androidx.annotation.DrawableRes
import com.woocommerce.android.ui.compose.designsystem.R as DesignSystemR

sealed class AppBarStatus {
    object Hidden : AppBarStatus()
    data class Visible(
        @DrawableRes
        val navigationIcon: Int? = DesignSystemR.drawable.woo_ds_ic_regular_arrow_left_24dp,
    ) : AppBarStatus()
}
