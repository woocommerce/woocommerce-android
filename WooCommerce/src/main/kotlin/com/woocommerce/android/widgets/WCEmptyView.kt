package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.core.text.HtmlCompat
import androidx.core.text.HtmlCompat.FROM_HTML_MODE_LEGACY
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WcEmptyViewBinding
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.DASHBOARD
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.FILTER_RESULTS
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_ERROR
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_OFFLINE
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_ALL_PROCESSED
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_FILTERED
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_CATEGORY_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_TAG_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.REVIEW_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import org.wordpress.android.util.DisplayUtils

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    private val binding = WcEmptyViewBinding.inflate(LayoutInflater.from(context), this, true)

    enum class EmptyViewType {
        DASHBOARD,
        ORDER_LIST,
        ORDER_LIST_LOADING,
        ORDER_LIST_ALL_PROCESSED,
        ORDER_LIST_FILTERED,
        PRODUCT_LIST,
        REVIEW_LIST,
        SEARCH_RESULTS,
        FILTER_RESULTS,
        NETWORK_ERROR,
        NETWORK_OFFLINE,
        PRODUCT_CATEGORY_LIST,
        PRODUCT_TAG_LIST
    }

    init {
        checkOrientation()
    }

    private var lastEmptyViewType: EmptyViewType? = null

    /**
     * Hide the image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        binding.emptyViewImage.visibility = if (isLandscape) View.GONE else View.VISIBLE
    }

    fun show(
        type: EmptyViewType,
        searchQueryOrFilter: String? = null,
        onButtonClick: (() -> Unit)? = null
    ) {
        checkOrientation()

        // if empty view is already showing and it's a different type, fade out the existing view before fading in
        if (visibility == View.VISIBLE && type != lastEmptyViewType) {
            WooAnimUtils.fadeOut(this, Duration.SHORT)
            val durationMs = Duration.SHORT.toMillis(context) + 50L
            Handler().postDelayed({
                show(type, searchQueryOrFilter, onButtonClick)
            }, durationMs)
            return
        }

        val title: String
        val message: String?
        val buttonText: String?
        val isTitleBold: Boolean
        @DrawableRes val drawableId: Int

        when (type) {
            DASHBOARD -> {
                isTitleBold = true
                title = context.getString(R.string.get_the_word_out)
                message = context.getString(R.string.share_your_store_message)
                buttonText = context.getString(R.string.share_store_button)
                drawableId = R.drawable.img_empty_my_store
            }
            ORDER_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.empty_order_list_title)
                message = context.getString(R.string.empty_order_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_empty_orders_no_orders
            }
            ORDER_LIST_LOADING -> {
                isTitleBold = true
                title = context.getString(R.string.orderlist_loading)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_orders_loading
            }
            ORDER_LIST_ALL_PROCESSED -> {
                isTitleBold = true
                title = context.getString(R.string.empty_order_list_all_processed)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_orders_all_fulfilled
            }
            ORDER_LIST_FILTERED -> {
                isTitleBold = false
                val fmtArgs = "<strong>$searchQueryOrFilter</strong>"
                title = String.format(
                        context.getString(R.string.orders_empty_message_with_order_status_filter),
                        fmtArgs
                )
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_search
            }
            PRODUCT_LIST -> {
                // TODO: once adding products is supported, this needs to be updated to match designs
                isTitleBold = true
                title = context.getString(R.string.product_list_empty)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_products
            }
            FILTER_RESULTS -> {
                isTitleBold = true
                title = context.getString(R.string.product_list_empty_filters)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_products
            }
            REVIEW_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.empty_review_list_title)
                message = context.getString(R.string.empty_review_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_empty_reviews
            }
            SEARCH_RESULTS -> {
                isTitleBold = false
                val fmtArgs = "<strong>$searchQueryOrFilter</strong>"
                title = String.format(context.getString(R.string.empty_message_with_search), fmtArgs)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_search
            }
            NETWORK_ERROR -> {
                isTitleBold = false
                title = context.getString(R.string.error_generic_network)
                message = null
                buttonText = context.getString(R.string.retry)
                drawableId = R.drawable.ic_woo_error_state
            }
            NETWORK_OFFLINE -> {
                isTitleBold = false
                title = context.getString(R.string.offline_error)
                message = null
                buttonText = context.getString(R.string.retry)
                drawableId = R.drawable.ic_woo_error_state
            }
            PRODUCT_CATEGORY_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.product_category_list_empty_title)
                message = context.getString(R.string.product_category_list_empty_message)
                buttonText = null
                drawableId = R.drawable.img_empty_products
            }
            PRODUCT_TAG_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.product_tag_list_empty_title)
                message = context.getString(R.string.product_tag_list_empty_message)
                buttonText = null
                drawableId = R.drawable.img_empty_products
            }
        }

        val titleHtml = if (isTitleBold) {
            "<strong>$title</strong>"
        } else {
            title
        }

        binding.emptyViewTitle.text = HtmlCompat.fromHtml(titleHtml, FROM_HTML_MODE_LEGACY)
        binding.emptyViewImage.setImageDrawable(context.getDrawable(drawableId))

        if (message != null) {
            binding.emptyViewMessage.text = HtmlCompat.fromHtml(message, FROM_HTML_MODE_LEGACY)
            binding.emptyViewMessage.visibility = View.VISIBLE
        } else {
            binding.emptyViewMessage.visibility = View.GONE
        }

        if (onButtonClick != null) {
            binding.emptyViewButton.text = buttonText
            binding.emptyViewButton.visibility = View.VISIBLE
            binding.emptyViewButton.setOnClickListener {
                onButtonClick.invoke()
            }
        } else {
            binding.emptyViewButton.visibility = View.GONE
        }

        if (visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(this, Duration.LONG)
        }

        lastEmptyViewType = type
    }

    fun hide() {
        if (visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(this, Duration.SHORT)
        }
    }
}
