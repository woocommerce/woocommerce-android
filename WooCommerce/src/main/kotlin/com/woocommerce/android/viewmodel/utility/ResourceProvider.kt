package com.woocommerce.android.viewmodel.utility

import android.content.Context
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ResourceProvider @Inject constructor(private val context: Context) {
    fun getString(@StringRes resourceId: Int) = context.getString(resourceId)

    fun getString(@StringRes resourceId: Int, vararg formatArgs: Any) = context.getString(resourceId, *formatArgs)

    fun getColor(@ColorRes resourceId: Int) = ContextCompat.getColor(context, resourceId)

    fun getDimensionPixelSize(@DimenRes dimen: Int) = context.resources.getDimensionPixelSize(dimen)
}
