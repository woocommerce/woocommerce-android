package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.widgets.WCEmptyView.EmptyViewType.DASHBOARD
import kotlinx.android.synthetic.main.dashboard_main_stats_row.view.*
import kotlinx.android.synthetic.main.wc_empty_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils
import java.util.Date

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    enum class EmptyViewType {
        DASHBOARD
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

    fun updateVisitorCount(visits: Int) {
        visitors_value.text = visits.toString()

        // The empty view is only shown when there are no orders, which means the revenue is also 0
        orders_value.text = "0"
        revenue_value.text = "0"
    }

    /**
     * Pass the site to use when sharing the store's url along with the tracks event to record
     * when the share button is tapped
     */
    fun show(
        type: EmptyViewType,
        site: SiteModel? = null,
        stat: AnalyticsTracker.Stat? = null
    ) {
        checkOrientation()

        val showStats: Boolean
        val showShareButton: Boolean
        @StringRes val titleId: Int
        @StringRes val messageId: Int
        @DrawableRes val drawableId: Int

        when (type) {
            DASHBOARD -> {
                showStats = true
                showShareButton = true
                titleId = R.string.get_the_word_out
                messageId = R.string.share_your_store_message
                drawableId = R.drawable.img_light_empty_my_store
            }
        }

        empty_view_title.text = context.getString(titleId)
        empty_view_message.text = context.getString(messageId)
        empty_view_image.setImageDrawable(context.getDrawable(drawableId))

        if (showShareButton && site != null && stat != null) {
            empty_view_button.visibility = View.VISIBLE
            empty_view_button.setOnClickListener {
                AnalyticsTracker.track(stat)
                ActivityUtils.shareStoreUrl(context, site.url)
            }
        } else {
            empty_view_button.visibility = View.GONE
        }

        if (showStats) {
            empty_view_stats_row.show()
            empty_view_date_title.text = DateUtils.getDayOfWeekWithMonthAndDayFromDate(Date())
        } else {
            empty_view_stats_row.hide()
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
