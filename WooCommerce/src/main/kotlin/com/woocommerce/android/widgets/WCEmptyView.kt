package com.woocommerce.android.widgets

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewTreeObserver
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.text.HtmlCompat
import androidx.core.view.isVisible
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WcEmptyViewBinding
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.DASHBOARD
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.FILTER_RESULTS
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.GROUPED_PRODUCT_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_ERROR
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.NETWORK_OFFLINE
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_DETAILS
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_CREATE_TEST_ORDER
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_FILTERED
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.ORDER_LIST_LOADING
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_CATEGORY_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.PRODUCT_TAG_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.REVIEW_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SEARCH_RESULTS
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SHIPPING_LABEL_CARRIER_RATES
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.SHIPPING_LABEL_SERVICE_PACKAGE_LIST
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.UNREAD_FILTERED_REVIEW_LIST

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    private val binding = WcEmptyViewBinding.inflate(LayoutInflater.from(context), this, true)

    enum class EmptyViewType {
        DASHBOARD,
        GROUPED_PRODUCT_LIST,
        ORDER_LIST,
        ORDER_LIST_CREATE_TEST_ORDER,
        ORDER_LIST_LOADING,
        ORDER_LIST_FILTERED,
        ORDER_DETAILS,
        PRODUCT_LIST,
        REVIEW_LIST,
        UNREAD_FILTERED_REVIEW_LIST,
        SEARCH_RESULTS,
        FILTER_RESULTS,
        NETWORK_ERROR,
        NETWORK_OFFLINE,
        PRODUCT_CATEGORY_LIST,
        PRODUCT_TAG_LIST,
        SHIPPING_LABEL_CARRIER_RATES,
        SHIPPING_LABEL_SERVICE_PACKAGE_LIST
    }

    private var lastEmptyViewType: EmptyViewType? = null

    private fun isParentViewHeightSufficient(): Boolean {
        var isSufficient = false
        val parentView = this.parent as? View
        parentView?.let {
            val parentHeightDp = it.height / context.resources.displayMetrics.density
            isSufficient = parentHeightDp >= MINIMUM_HEIGHT_DP
        }
        return isSufficient
    }

    init {
        // Add a global layout listener to check the height of the parent view
        viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                // Remove the listener to prevent it from being called multiple times
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                // Update the visibility based on the parent view's height
                binding.emptyViewImage.isVisible = isParentViewHeightSufficient()
            }
        })
    }

    @Suppress("LongMethod", "ComplexMethod")
    fun show(
        type: EmptyViewType,
        searchQueryOrFilter: String? = null,
        onButtonClick: (() -> Unit)? = null
    ) {
        binding.emptyViewImage.isVisible = isParentViewHeightSufficient()

        // if empty view is already showing and it's a different type, fade out the existing view before fading in
        if (visibility == View.VISIBLE && type != lastEmptyViewType) {
            WooAnimUtils.fadeOut(this, Duration.SHORT)
            val durationMs = Duration.SHORT.toMillis(context) + 50L
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    show(type, searchQueryOrFilter, onButtonClick)
                },
                durationMs
            )
            return
        }

        val title: String?
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

            GROUPED_PRODUCT_LIST -> {
                isTitleBold = false
                title = null
                message = context.getString(R.string.product_list_empty)
                buttonText = null
                drawableId = R.drawable.img_empty_products
            }

            ORDER_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.empty_order_list_title)
                message = context.getString(R.string.empty_order_list_message)
                buttonText = context.getString(R.string.learn_more)
                drawableId = R.drawable.img_empty_orders_no_orders
            }

            ORDER_LIST_CREATE_TEST_ORDER -> {
                isTitleBold = true
                title = context.getString(R.string.empty_order_list_title)
                message = context.getString(R.string.empty_order_test_order_message)
                buttonText = context.getString(R.string.empty_order_test_order_button)
                drawableId = R.drawable.img_empty_orders_no_orders
            }

            ORDER_LIST_LOADING -> {
                isTitleBold = true
                title = context.getString(R.string.orderlist_loading)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_orders_loading
            }

            ORDER_LIST_FILTERED -> {
                isTitleBold = false
                title = context.getString(R.string.orders_empty_message_for_filtered_orders)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_search
            }

            ORDER_DETAILS -> {
                isTitleBold = true
                title = context.getString(R.string.empty_order_detail_title)
                message = context.getString(R.string.empty_order_detail_message)
                buttonText = null
                drawableId = R.drawable.img_empty_orders_no_orders
            }

            PRODUCT_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.product_list_empty)
                message = context.getString(R.string.empty_product_message)
                buttonText = context.getString(R.string.empty_product_add_product_button)
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

            UNREAD_FILTERED_REVIEW_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.empty_review_filtered_list_title)
                message = context.getString(R.string.empty_review_filtered_list_message)
                buttonText = null
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

            SHIPPING_LABEL_CARRIER_RATES -> {
                isTitleBold = false
                title = context.getString(R.string.shipping_label_shipping_carrier_rates_unavailable_title)
                message = context.getString(R.string.shipping_label_shipping_carrier_rates_unavailable_message)
                buttonText = null
                drawableId = R.drawable.img_products_error
            }

            SHIPPING_LABEL_SERVICE_PACKAGE_LIST -> {
                isTitleBold = true
                title = context.getString(R.string.shipping_label_activate_service_package_empty_title)
                message = null
                buttonText = null
                drawableId = R.drawable.img_empty_orders_all_fulfilled
            }
        }

        if (title.isNullOrEmpty()) {
            binding.emptyViewTitle.isVisible = false
        } else {
            val titleHtml = if (isTitleBold) {
                "<strong>$title</strong>"
            } else {
                title
            }
            binding.emptyViewTitle.isVisible = true
            binding.emptyViewTitle.text = HtmlCompat.fromHtml(titleHtml, HtmlCompat.FROM_HTML_MODE_LEGACY)
        }

        binding.emptyViewImage.setImageDrawable(AppCompatResources.getDrawable(context, drawableId))
        if (message != null) {
            binding.emptyViewMessage.text = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY)
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

    companion object {
        private const val MINIMUM_HEIGHT_DP = 400
    }
}
