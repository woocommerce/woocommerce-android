package com.woocommerce.android.widgets

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import kotlinx.android.synthetic.main.dashboard_main_stats_row.view.*
import kotlinx.android.synthetic.main.wc_empty_view.view.date_title
import kotlinx.android.synthetic.main.wc_share_your_store_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils
import java.util.Date

class WCShareYourStoreView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.wc_share_your_store_view, this)
        checkOrientation()
    }

    /**
     * Hide the image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        share_your_store_image.visibility = if (isLandscape) View.GONE else View.VISIBLE
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
        site: SiteModel,
        stat: AnalyticsTracker.Stat,
        showStats: Boolean = false
    ) {
        checkOrientation()

        share_your_store_button.setOnClickListener {
            AnalyticsTracker.track(stat)
            ActivityUtils.shareStoreUrl(context, site.url)
        }

        if (showStats) {
            share_your_store_stats_row.show()
        } else {
            share_your_store_stats_row.hide()
        }

        if (visibility != View.VISIBLE) {
            WooAnimUtils.fadeIn(this, Duration.LONG)
        }

        date_title.text = DateUtils.getDayOfWeekWithMonthAndDayFromDate(Date())
    }

    fun hide() {
        if (visibility == View.VISIBLE) {
            WooAnimUtils.fadeOut(this, Duration.LONG)
        }
    }
}
