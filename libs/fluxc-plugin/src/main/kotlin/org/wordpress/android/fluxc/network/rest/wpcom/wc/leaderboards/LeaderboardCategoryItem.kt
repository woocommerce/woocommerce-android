package org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards

import android.text.Html
import android.text.SpannableStringBuilder
import android.text.style.URLSpan
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.LeaderboardItemRow
import org.wordpress.android.fluxc.network.rest.wpcom.wc.leaderboards.LeaderboardsApiResponse.Type.CATEGORIES

/**
 * The API response from the V4 Leaderboards endpoint returns the Top Performer Categories list as an array of arrays,
 * each inner array represents a Top Performer Category, so in the end it's an array of Top Performer Category items.
 *
 * Each Top Performer Category item is an array containing three objects with the following properties: display and value.
 *
 * Single Top Performer Category item response example:
[
    {
        "display": "<a href='https:\/\/mystagingwebsite.com\/wp-admin\/admin.php?page=wc-admin&path=\/analytics\/categories&filter=single_category&categories=16'>Clothing<\/a>",
        "value": "Clothing"
    },
    {
        "display": "6.650",
        "value": 6650
    },
    {
        "display": "<span class=\"woocommerce-Price-amount amount\"><span class=\"woocommerce-Price-currencySymbol\">&#82;&#36;<\/span>239.300,00<\/span>",
        "value": 239300
    }
]

 * This class represents one Single Top Performer item response as a Category type one
 */
@Suppress("MaxLineLength")
class LeaderboardCategoryItem(
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

    @Suppress("MaxLineLength") private val plainTextCurrency by lazy {
        Html.fromHtml(priceAmountHtmlTag, Html.FROM_HTML_MODE_LEGACY)
            .toString()
            .replace(Regex("[0-9.,]"), "")
    }

    val categoryId by lazy {
        link
            ?.split("&")
            ?.firstOrNull { it.contains("${CATEGORIES.value}=", true) }
            ?.split("=")
            ?.last()
            ?.toLongOrNull()
    }

    val name by lazy {
        itemRows
            ?.first()
            ?.value
    }

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

    private fun Array<LeaderboardItemRow>.second() =
        takeIf { isNotEmpty() && size > 1 }
            ?.let { this[1] }

    private fun Array<LeaderboardItemRow>.third() =
        takeIf { isNotEmpty() && size > 2 }
            ?.let { this[2] }
}
