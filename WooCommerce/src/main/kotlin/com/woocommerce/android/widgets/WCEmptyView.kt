package com.woocommerce.android.widgets

import android.content.Context
import android.content.res.Configuration
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import com.woocommerce.android.util.hide
import com.woocommerce.android.util.show
import kotlinx.android.synthetic.main.dashboard_main_stats_row.view.*
import kotlinx.android.synthetic.main.wc_empty_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils
import java.util.Date

class WCEmptyView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    private var showNoCustomersImage = true
    private var siteModel: SiteModel? = null
    private var shareTracksEvent: AnalyticsTracker.Stat? = null

    init {
        View.inflate(context, R.layout.wc_empty_view, this)
        checkOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        checkOrientation()
    }

    /**
     * Hide the "no customers" image in landscape since there isn't enough room for it on most devices
     */
    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        empty_view_image.visibility = if (showNoCustomersImage && !isLandscape) View.VISIBLE else View.GONE
    }

    /**
     * Pass the site to use when sharing the store's url along with the tracks event to record
     * when the share button is tapped
     */
    fun setSiteToShare(site: SiteModel, stat: AnalyticsTracker.Stat) {
        siteModel = site
        shareTracksEvent = stat
    }

    fun updateVisitorCount(visits: Int) {
        visitors_value.text = visits.toString()
        orders_value.text = "0"
        revenue_value.text = "0"
    }

    fun show(
        @StringRes messageId: Int,
        showImage: Boolean = true,
        showShareButton: Boolean = true,
        showStats: Boolean = false
    ) {
        showNoCustomersImage = showImage
        checkOrientation()

        empty_view_text.text = context.getText(messageId)

        if (showShareButton && siteModel != null) {
            empty_view_share_button.visibility = View.VISIBLE
            empty_view_share_button.setOnClickListener {
                shareTracksEvent?.let {
                    AnalyticsTracker.track(it)
                }
                ActivityUtils.shareStoreUrl(context, siteModel!!.url)
            }
        } else {
            empty_view_share_button.visibility = View.GONE
        }

        if (showStats) {
            empty_view_stats_row.show()
        } else
            empty_view_stats_row.hide()

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
