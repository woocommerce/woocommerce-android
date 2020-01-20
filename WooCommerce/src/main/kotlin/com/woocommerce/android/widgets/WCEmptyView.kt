package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.DASHBOARD
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.REVIEW_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import kotlinx.android.synthetic.main.wc_empty_view.view.*
import org.wordpress.android.util.DisplayUtils

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    enum class EmptyViewType {
        DASHBOARD,
        ORDER_LIST,
        ORDER_LIST_LOADING,
        PRODUCT_LIST,
        REVIEW_LIST,
        SEARCH_RESULTS,
    }

    init {
        View.inflate(context, R.layout.wc_empty_view, this)
        checkOrientation()
    }

    private var lastEmptyViewType: EmptyViewType? = null

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
        searchQuery: String? = null,
        onButtonClick: (() -> Unit)? = null
    ) {
        checkOrientation()

        // if empty view is already showing and it's a different type, fade out the existing view before fading in
        if (visibility == View.VISIBLE && type != lastEmptyViewType) {
            WooAnimUtils.fadeOut(this, Duration.SHORT)
            val durationMs = Duration.SHORT.toMillis(context) + 50L
            Handler().postDelayed({
                show(type, searchQuery, onButtonClick)
            }, durationMs)
            return
        }

        val showButton: Boolean
        val title: String
        val message: String
        val buttonText: String?
        val isTitleBold: Boolean
        @DrawableRes val drawableId: Int

        when (type) {
            DASHBOARD -> {
                showButton = true
                isTitleBold = true
                title = context.getString(R.string.get_the_word_out)
                message = context.getString(R.string.share_your_store_message)
                buttonText = context.getString(R.string.share_store_button)
                drawableId = R.drawable.img_light_empty_my_store
            }
            ORDER_LIST -> {
                showButton = true
                isTitleBold = true
                title = context.getString(R.string.empty_order_list_title)
                message = context.getString(R.string.empty_order_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_light_empty_orders_no_orders
            }
            ORDER_LIST_LOADING -> {
                showButton = false
                isTitleBold = true
                title = context.getString(R.string.orderlist_loading)
                message = ""
                buttonText = null
                drawableId = R.drawable.img_light_empty_orders_looking_up
            }
            PRODUCT_LIST -> {
                // TODO: once adding products is supported, this needs to be updated to match designs
                showButton = false
                isTitleBold = true
                title = context.getString(R.string.product_list_empty)
                message = ""
                buttonText = null
                drawableId = R.drawable.img_light_empty_products
            }
            REVIEW_LIST -> {
                showButton = true
                isTitleBold = true
                title = context.getString(R.string.empty_review_list_title)
                message = context.getString(R.string.empty_review_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_light_empty_reviews
            }
            SEARCH_RESULTS -> {
                showButton = false
                isTitleBold = false
                val fmtArgs = "<strong>$searchQuery</strong>"
                title = String.format(context.getString(R.string.empty_message_with_search), fmtArgs)
                message = ""
                buttonText = null
                drawableId = R.drawable.img_light_empty_search
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
                onButtonClick?.invoke()
            }
        } else {
            empty_view_button.visibility = View.GONE
        }

        if (visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(this, Duration.LONG)
        }

        lastEmptyViewType = type
    }

    fun hide() {
        if (visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(this, Duration.LONG)
        }
    }
}
