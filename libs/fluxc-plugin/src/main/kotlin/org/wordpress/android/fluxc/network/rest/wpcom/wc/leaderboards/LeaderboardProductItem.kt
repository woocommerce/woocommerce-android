package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.LeaderboardItemRow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.PRODUCTS

/**
 * The API response from the V4 Leaderboards endpoint returns the Top Performers Products list as an array of arrays,
 * each inner array represents a Top Performer Product itself, so in the end it's an array of Top Performers items.
 *
 * Each Top Performer item is an array containing three objects with the following properties: display and value.
 *
 * Single Top Performer item response example:
[
    {
        "display": "<a href='https:\/\/mystagingwebsite.com\/wp-admin\/admin.php?page=wc-admin&path=\/analytics\/products&filter=single_product&products=14'>Beanie<\/a>",
        "value": "Beanie"
    },
    {
        "display": "2.000",
        "value": 2000
    },
    {
        "display": "<span class=\"woocommerce-Price-amount amount\"><span class=\"woocommerce-Price-currencySymbol\">&#82;&#36;<\/span>36.000,00<\/span>",
        "value": 36000
    }
]

 This class represents one Single Top Performer item response as a Product type one
 */
@Suppress("MaxLineLength")
class LeaderboardProductItem(
    private val itemRows: Array<LeaderboardItemRow>? = null
) {
    val quantity
        get() = itemRows
                ?.second()
                ?.value

    val total
        get() = itemRows
                ?.third()
                ?.value

    /**
     * This property will operate and transform a HTML tag in order to retrieve the currency symbol out of it.
     *
     * Example:
     *  HTML tag -> <span class=\"woocommerce-Price-amount amount\"><span class=\"woocommerce-Price-currencySymbol\">&#82;&#36;<\/span>36.000,00<\/span>
     *
     *  split from ">" delimiter
     *      Output: string list containing
     *              <span class=\"woocommerce-Price-amount amount\"
     *              <span class=\"woocommerce-Price-currencySymbol\"
     *              &#82;&#36;<\/span
     *              36.000,00<\/span
     *
     *  first who contains "&#"
     *      Output: &#82;&#36;<\/span>
     *
     *  split from ";" delimiter
     *      Output: string list containing
     *              &#82
     *              &#36
     *              <\/span>
     *
     *  filter what contains "&#"
     *      Output: string list containing
     *              &#82
     *              &#36
     *
     *  reduce string list
     *      Output: &#82&#36
     *
     *  fromHtmlWithSafeApiCall
     *      Output: R$
     */
    @Suppress("MaxLineLength") val currency by lazy {
        priceAmountHtmlTag
                ?.split(">")
                ?.firstOrNull { it.contains("&#") }
                ?.split(";")
                ?.filter { it.contains("&#") }
                ?.reduce { total, new -> "$total$new" }
                ?.run { Html.fromHtml(this, Html.FROM_HTML_MODE_LEGACY) }
                ?: plainTextCurrency
    }

    /**
     * This property will serve as a fallback for cases where the currency is represented in plain text instead of
     * HTML currency code.
     *
     * It will extract the raw content between tags and leave only the currency information
     *
     * Example:
     *
     *  HTML tag -> <span class=\"woocommerce-Price-amount amount\"><span class=\"woocommerce-Price-currencySymbol\">DKK<\/span>36.000,00<\/span>
     *
     *  parse HTML tag to string
     *      Output: DKK36.000,00
     *
     *  regex filter with replace
     *      Output: DKK
     */
    @Suppress("MaxLineLength") private val plainTextCurrency by lazy {
        Html.fromHtml(priceAmountHtmlTag, Html.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace(Regex("[0-9.,]"), "")
    }

    /**
     * This property will operate and transform a URL string in order to retrieve the product id parameter out of it.
     *
     * Example:
     *  URL string -> https:\/\/mystagingwebsite.com\/wp-admin\/admin.php?page=wc-admin&path=\/analytics\/products&filter=single_product&products=14
     *
     *  split from "&" delimiter
     *      Output: string list containing
     *              https:\/\/mystagingwebsite.com\/wp-admin\/admin.php?page=wc-admin
     *              filter=single_product
     *              products=14
     *
     *  first who contains "products="
     *      Output: products=14
     *
     *  split from "=" delimiter
     *      Output: string list containing
     *              products
     *              14
     *
     *  extract last string from the list
     *      Output: 14 as String
     *
     *  try to convert to long
     *      Output: 14 as Long
     */
    @Suppress("MaxLineLength") val productId by lazy {
        link
                ?.split("&")
                ?.firstOrNull { it.contains("${PRODUCTS.value}=", true) }
                ?.split("=")
                ?.last()
                ?.toLongOrNull()
    }

    /**
     * This property will operate and transform a HTML tag in order to retrieve the inner URL out of the <a href/> tag
     * using the [SpannableStringBuilder] implementation in order to parse it
     */
    private val link by lazy {
        Html.fromHtml(itemHtmlTag, Html.FROM_HTML_MODE_LEGACY)
                .run { this as? SpannableStringBuilder }
                ?.spansAsList()
                ?.firstOrNull()
                ?.url
    }

    private val itemHtmlTag by lazy {
        itemRows
                ?.first()
                ?.display
    }

    private val priceAmountHtmlTag by lazy {
        itemRows
                ?.third()
                ?.display
    }

    private fun SpannableStringBuilder.spansAsList() =
            getSpans(0, length, URLSpan::class.java)
                    .toList()

    /**
     * Returns the second object of the Top Performer Item Array if exists
     */
    private fun Array<LeaderboardItemRow>.second() =
            takeIf { isNotEmpty() && size > 1 }
                    ?.let { this[1] }

    /**
     * Returns the third object of the Top Performer Item Array if exists
     */
    private fun Array<LeaderboardItemRow>.third() =
            takeIf { isNotEmpty() && size > 2 }
                    ?.let { this[2] }
}
