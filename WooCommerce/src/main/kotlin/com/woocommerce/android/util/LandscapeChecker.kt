package com.woocommerce.android.util

import android.content.Context
import dagger.Reusable
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@Reusable
class LandscapeChecker @Inject constructor(@ApplicationContext private val context: Context) {
    fun isLandscape() = DisplayUtils.isLandscape(context)
}
