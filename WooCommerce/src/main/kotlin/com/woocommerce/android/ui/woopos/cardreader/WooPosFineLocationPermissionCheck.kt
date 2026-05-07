package com.woocommerce.android.ui.woopos.cardreader

import android.content.Context
import com.woocommerce.android.util.WooPermissionUtils
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

@Reusable
class WooPosFineLocationPermissionCheck @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isGranted(): Boolean = WooPermissionUtils.hasFineLocationPermission(context)
}
