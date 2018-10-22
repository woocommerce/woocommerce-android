package com.woocommerce.android.util

import android.content.Context
import android.net.Uri
import android.support.annotation.StringRes
import org.wordpress.android.fluxc.model.SiteModel
import java.util.regex.Pattern

object StringUtils {
    private val EMAIL_REGEX = "^\\s*?(.+)@(.+?)\\s*$"
    private val EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX)

    /**
     * Borrowed and modified from WordPress-Android :)
     *
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
        context: Context,
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

    /**
     * Similar to UrlUtils.getHost() except that it includes the path (subfolder)
     *
     * Ex:
     *      https://baseurl.com -> baseurl.com
     *      https://baseurl.com/mysite -> baseurl.com/mysite
     */
    fun getSiteDomainAndPath(site: SiteModel): String {
        site.url?.let {
            val uri = Uri.parse(it)
            return uri.host.orEmpty() + uri.path.orEmpty()
        } ?: return ""
    }

    /**
     * Returns true if the passed string is a valid email - note that this is partially adapted
     * from the Apache Commons EmailValidator but it isn't as comprehensive. If a more robust
     * validation is deemed necessary we should consider adding that library
     */
    fun isValidEmail(email: String?): Boolean {
        return email?.let {
            return EMAIL_PATTERN.matcher(it).matches()
        } ?: false
    }
}
