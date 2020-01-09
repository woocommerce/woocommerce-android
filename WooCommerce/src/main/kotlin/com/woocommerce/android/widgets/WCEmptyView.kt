package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.core.text.HtmlCompat
import com.woocommerce.android.R
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import kotlinx.android.synthetic.main.wc_empty_view.view.*
import org.wordpress.android.util.DisplayUtils

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
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

    fun show(
        @StringRes messageId: Int,
        @DrawableRes imageId: Int = R.drawable.ic_woo_waiting_customers
    ) {
        show(context.getString(messageId), imageId)
    }

    fun show(
        message: String,
        @DrawableRes imageId: Int = R.drawable.ic_woo_waiting_customers
    ) {
        checkOrientation()

        empty_view_text.text = HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY)
        empty_view_image.setImageDrawable(context.getDrawable(imageId))

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
