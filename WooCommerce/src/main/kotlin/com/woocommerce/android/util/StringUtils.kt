package com.woocommerce.android.util

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources.NotFoundException
import android.net.Uri
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Patterns
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import com.woocommerce.android.extensions.isInteger
import com.woocommerce.android.util.WooLog.T.UTILS
import com.woocommerce.android.viewmodel.ResourceProvider
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.FormatUtils
import java.io.IOException
import java.util.Locale
import kotlin.math.abs

@Suppress("unused")
object StringUtils {
    const val EMPTY = ""
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
     * Borrowed and modified from WordPress-Android :)
     *
     * Formats the string for the given [quantity], using the given params.
     * We need this because our translation platform doesn't support Android plurals.
     *
     * This variant uses a [ResourceProvider]
     *
     * If a string resource is not provided for [zero] or [one] the [default] resource will be used.
     *
     * @param [resourceProvider] The string resources provider
     * @param [quantity] The number used to pick the correct string
     * @param [default] The desired string identifier to get when [quantity] is not (0 or 1)
     * @param [zero] Optional. The desired string identifier to use when [quantity] is exactly 0.
     * @param [one] Optional. The desired string identifier to use when the [quantity] is exactly 1
     */
    fun getQuantityString(
        resourceProvider: ResourceProvider,
        quantity: Int,
        @StringRes default: Int,
        @StringRes zero: Int? = null,
        @StringRes one: Int? = null
    ): String {
        return when (quantity) {
            0 -> resourceProvider.getString(zero ?: default, quantity)
            1 -> resourceProvider.getString(one ?: default, quantity)
            else -> resourceProvider.getString(default, quantity)
        }
    }

    fun getPluralString(
        resourceProvider: ResourceProvider,
        quantity: Int,
        @PluralsRes pluralId: Int
    ): String {
        return resourceProvider.getPluralString(pluralId, quantity)
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
        val absNumber = abs(number)
        return when {
            absNumber >= ONE_MILLION -> (number / ONE_MILLION).toString() + "m"
            absNumber >= ONE_THOUSAND -> (number / ONE_THOUSAND).toString() + "k"
            else -> number.toString()
        }
    }

    /**
     * If a number's fractional part is zero, remove it and return as string. Otherwise return as string while
     * including the fractional part.
     * If the number is to be displayed as regular text, `formatInt` is used to display commas/dot for thousands
     * separator (depending on locale).
     * If the number is to be displayed in an editable text input, number.toInt() is used so that the input behavior
     * does not show the thousands separator.
     *
     *  @param [number] The number to be formatted
     *  @param [forInput] Whether the formatting is used in a text input or not.
     *
     *  For eg: for a number = 234560 and forInput = false, returns 234560
     * for a number = 234560 forInput = true, returns 2,34,560
     * for a number = 2.3456 return 2.3456
     *
     */
    fun formatCountDecimal(number: Double, forInput: Boolean = false): String {
        return if (number.isInteger()) {
            if (forInput)
                number.toInt().toString()
            else
                FormatUtils.formatInt(number.toInt())
        } else {
            number.toString()
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
            WooLog.d(UTILS, "Unable to find a valid country name for country code: $storeCountry")
        }
        return null
    }

    /**
     * Given a raw HTML file, returns the url for the file
     */
    fun getRawFileUrl(context: Context, rawId: Int): String {
        return try {
            val inputStream = context.resources.openRawResource(rawId)
            val buffer = ByteArray(inputStream.available())
            inputStream.read(buffer)
            inputStream.close()
            String(buffer)
        } catch (e: IOException) {
            ""
        }
    }

    /**
     * Strips HTML tags and newline characters from the provided text and returns the raw text.
     * Newline characters are replaced with a space, and then we replace any instances of
     * double spaces with a single space (just in case)
     */
    fun getRawTextFromHtml(htmlStr: String) =
            Html.fromHtml(htmlStr).toString().replace("\n", " ").replace("  ", " ")

    /**
     * Helper method for using the appropriate `Html.fromHtml()` for the build version.
     */
    fun fromHtml(htmlStr: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(htmlStr, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(htmlStr)
        }
    }

    /**
     * Returns a string for the specified locale.
     *
     * @param context The active context
     * @param id The id of the string (ex. R.string.my_string)
     * @param locale The two-character locale to fetch the string for (ex. "en")
     * @return A string matching the [id] for the [locale] requested, or null if none found
     */
    fun getStringByLocale(context: Context, id: Int, locale: String): String? {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale(locale))
        return try {
            context.createConfigurationContext(configuration).resources.getString(id)
        } catch (e: NotFoundException) {
            WooLog.w(UTILS, "No resource found for id $id in locale $locale")
            null
        }
    }

    /**
     * Returns a string array for the specified locale.
     *
     * @param context The active context
     * @param id The id of the string (ex. R.string.my_string)
     * @param locale The two-character locale to fetch the string for (ex. "en")
     * @return A list of strings matching the [id] for the [locale] requested, or null if none found
     */
    fun getStringArrayByLocale(context: Context, id: Int, locale: String): List<String>? {
        val configuration = Configuration(context.resources.configuration)
        configuration.setLocale(Locale(locale))
        return try {
            context.createConfigurationContext(configuration).resources.getStringArray(id).asList()
        } catch (e: NotFoundException) {
            WooLog.w(UTILS, "No string array resource found for id $id in locale $locale")
            null
        }
    }
}
