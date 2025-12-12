package com.woocommerce.android.viewmodel

import android.content.Context
import android.content.res.Configuration
import androidx.annotation.ArrayRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.RawRes
import androidx.annotation.StringRes
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

    /**
     * Formats the string for the given [quantity], using the given params.
     * We need this because our translation platform doesn't support Android plurals.
     *
     * If a string resource is not provided for [zero] or [one] the [default] resource will be used.
     *
     * @param [quantity] The number used to pick the correct string
     * @param [default] The desired string identifier to get when [quantity] is not (0 or 1)
     * @param [zero] Optional. The desired string identifier to use when [quantity] is exactly 0.
     * @param [one] Optional. The desired string identifier to use when the [quantity] is exactly 1
     */
    fun getQuantityString(
        quantity: Int,
        @StringRes default: Int,
        @StringRes zero: Int? = null,
        @StringRes one: Int? = null
    ): String {
        return when (quantity) {
            0 -> context.getString(zero ?: default, quantity)
            1 -> context.getString(one ?: default, quantity)
            else -> context.getString(default, quantity)
        }
    }

    fun isDarkMode() = context.resources.configuration.uiMode and
        Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
}
