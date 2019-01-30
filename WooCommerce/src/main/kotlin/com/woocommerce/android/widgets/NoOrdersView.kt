package com.woocommerce.android.widgets

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.support.annotation.StringRes
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.WooAnimUtils
import com.woocommerce.android.util.WooAnimUtils.Duration
import kotlinx.android.synthetic.main.no_orders_view.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DisplayUtils

class NoOrdersView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.no_orders_view, this)
        orientation = LinearLayout.VERTICAL
        checkOrientation()
    }

    /**
     * the main activity has android:configChanges="orientation|screenSize" in the manifest, so we have to
     * handle screen rotation here
     */
    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)
        checkOrientation()
    }

    private fun checkOrientation() {
        val isLandscape = DisplayUtils.isLandscape(context)
        no_orders_image.visibility = if (isLandscape) View.GONE else View.VISIBLE
    }

    fun show(@StringRes messageId: Int, site: SiteModel) {
        no_orders_text.text = context.getText(messageId)
        no_orders_share_button.setOnClickListener {
            AnalyticsTracker.track(Stat.DASHBOARD_SHARE_YOUR_STORE_BUTTON_TAPPED)
            ActivityUtils.shareStoreUrl(context, site.url)
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

    fun test(site: SiteModel) {
        show(R.string.dashboard_no_orders, site)
        Handler().postDelayed({
            hide()
            Handler().postDelayed({
                test(site)
            }, 2000)
        }, 2000)
    }
}
