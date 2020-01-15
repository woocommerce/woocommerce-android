package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.DASHBOARD
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import kotlinx.android.synthetic.main.wc_empty_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    companion object {
        private const val URL_LEARN_MORE = "https://woocommerce.com/blog/"
    }

    enum class EmptyViewType {
        DASHBOARD,
        ORDER_LIST,
        PRODUCT_LIST,
        SEARCH_RESULTS,
    }

    init {
        View.inflate(context, R.layout.wc_empty_view, this)
        checkOrientation()
    }

    /**
     * Hide the image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        empty_view_image.visibility = if (isLandscape) View.GONE else View.VISIBLE
    }

    /**
     * Pass the site to use when sharing the store's url along with the tracks event to record
     * when the share button is tapped
     */
    fun show(
        type: EmptyViewType,
        site: SiteModel? = null,
        searchQuery: String? = null
    ) {
        checkOrientation()

        val tracksStat: Stat?
        val showButton: Boolean
        val title: String
        val message: String
        val buttonText: String?
        val isTitleBold: Boolean
        @DrawableRes val drawableId: Int

        when (type) {
            DASHBOARD -> {
                showButton = true
                tracksStat = Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED
                title = context.getString(R.string.get_the_word_out)
                message = context.getString(R.string.share_your_store_message)
                buttonText = context.getString(R.string.share_store_button)
                drawableId = R.drawable.img_light_empty_my_store
                isTitleBold = true
            }
            ORDER_LIST -> {
                showButton = true
                tracksStat = null // TODO
                title = context.getString(R.string.empty_order_list_title)
                message = context.getString(R.string.empty_order_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_light_empty_orders_no_orders
                isTitleBold = true
            }
            PRODUCT_LIST -> {
                // TODO: once adding products is supported, this needs to be updated to match designs
                showButton = false
                tracksStat = null
                title = context.getString(R.string.product_list_empty)
                message = ""
                buttonText = null
                drawableId = R.drawable.img_light_empty_products
                isTitleBold = false
            }
            SEARCH_RESULTS -> {
                showButton = false
                tracksStat = null
                val fmtArgs = "<strong>$searchQuery</strong>"
                title = String.format(context.getString(R.string.empty_message_with_search), fmtArgs)
                message = ""
                buttonText = null
                drawableId = R.drawable.img_light_empty_search
                isTitleBold = false
            }
        }

        val titleHtml = if (isTitleBold) {
            "<strong>$title</strong>"
        } else {
            title
        }

        empty_view_title.text = HtmlCompat.fromHtml(titleHtml, FROM_HTML_MODE_LEGACY)
        empty_view_message.text = HtmlCompat.fromHtml(message, FROM_HTML_MODE_LEGACY)
        empty_view_button.text = buttonText
        empty_view_image.setImageDrawable(context.getDrawable(drawableId))

        if (showButton) {
            empty_view_button.visibility = View.VISIBLE
            empty_view_button.setOnClickListener {
                tracksStat?.let {
                    AnalyticsTracker.track(it)
                }
                when (type) {
                    DASHBOARD -> {
                        site?.let {
                            ActivityUtils.shareStoreUrl(context, it.url)
                        }
                    }
                    ORDER_LIST -> {
                        ChromeCustomTabUtils.launchUrl(context, URL_LEARN_MORE)
                    }
                }
            }
        } else {
            empty_view_button.visibility = View.GONE
        }

        if (visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(this, Duration.LONG)
        }
    }

    fun hide() {
        if (visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(this, Duration.LONG)
        }
    }
}
