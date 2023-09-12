package com.woocommerce.android.viewmodel

import android.content.Context
import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.IntegerRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import com.woocommerce.android.util.StringUtils
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

    fun getInteger(@IntegerRes resourceId: Int): Int {
        return context.resources.getInteger(resourceId)
    }

    fun getQuantityString(
        quantity: Int,
        @StringRes default: Int,
        @StringRes zero: Int? = null,
        @StringRes one: Int? = null
    ) = StringUtils.getQuantityString(
        context,
        quantity,
        default,
        zero,
        one,
    )

    @StringRes
    fun getStringResFromStringName(stringName: String): Int? {
        val stringRes = context.resources.getIdentifier(stringName, "string", context.packageName)
        return when (stringRes) {
            0 -> null // String not found for given key, return null so it can be handled from calling function
            else -> stringRes
        }
    }
}
