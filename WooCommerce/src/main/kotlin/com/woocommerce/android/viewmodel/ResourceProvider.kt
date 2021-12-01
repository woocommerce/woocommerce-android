package com.woocommerce.android.viewmodel

import android.content.Context
import androidx.annotation.*
import androidx.core.content.ContextCompat
import java.io.InputStream
import javax.inject.Inject

class ResourceProvider @Inject constructor(private val context: Context) {
    fun getString(@StringRes resourceId: Int): String {
        return context.getString(resourceId)
    }

    fun getString(@StringRes resourceId: Int, vararg formatArgs: Any): String {
        return context.getString(resourceId, *formatArgs)
    }

    fun getStringArray(@ArrayRes resourceId: Int): Array<String> {
        return context.resources.getStringArray(resourceId)
    }

    fun getColor(@ColorRes resourceId: Int): Int {
        return ContextCompat.getColor(context, resourceId)
    }

    fun getDimensionPixelSize(@DimenRes dimen: Int): Int {
        val resources = context.resources
        return resources.getDimensionPixelSize(dimen)
    }

    fun openRawResource(@RawRes rawId: Int): InputStream {
        val resources = context.resources
        return resources.openRawResource(rawId)
    }
}
