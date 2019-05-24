package com.woocommerce.android.util

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.support.annotation.StringRes
import android.util.Patterns
import org.wordpress.android.fluxc.model.SiteModel

object StringUtils {
    private const val ONE_MILLION = 1000000
    private const val ONE_THOUSAND = 1000

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
     * Returns true if the passed string is a valid email address
     */
    fun isValidEmail(email: String?): Boolean {
        return email?.let {
            return Patterns.EMAIL_ADDRESS.matcher(it).matches()
        } ?: false
    }

    /**
     * Returns the passed number formatted as a count
     * ex:
     *  formatCount(200) -> 200
     *  formatCount(2000) -> 2k
     *  formatCount(20000) -> 20k
     *  formatCount(2000000) - > 2m
     */
    fun formatCount(number: Int): String {
        val absNumber = Math.abs(number)
        return when {
            absNumber >= ONE_MILLION -> (number / ONE_MILLION).toString() + "m"
            absNumber >= ONE_THOUSAND -> (number / ONE_THOUSAND).toString() + "k"
            else -> number.toString()
        }
    }

    /**
     * Returns the name of the country associated with the current store.
     * @param [storeCountry], if available is in the format US:NY.
     * This method will transform `US:NY` into `United States`
     * by getting the corresponding country name from string.xml for this
     * value: "country_mapping_$countryCode"
     *
     * Will return nil if it can not figure out a valid country name
     * There might be some scenario where the store country is not
     * mapped to a valid country name. In order to avoid potential
     * crashes because of this, logging the exception and returning
     * null
     * */
    fun getCountryByCountryCode(
        context: Context,
        storeCountry: String?
    ): String? {
        try {
            storeCountry?.let {
                val countryCode = it.split(":")[0]
                val resourceId = context.resources.getIdentifier(
                        "country_mapping_$countryCode",
                        "string",
                        context.packageName
                )
                return context.getString(resourceId)
            }
        } catch (e: NotFoundException) {
            WooLog.d(WooLog.T.UTILS, "Unable to find a valid country name for country code: $storeCountry")
        }
        return null
    }
}
