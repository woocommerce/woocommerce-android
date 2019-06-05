package com.woocommerce.android.ui.products

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.util.ChromeCustomTabUtils

class WCProductPropertyLinkView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : ConstraintLayout(context, attrs, defStyle) {
    private var view: View = View.inflate(context, R.layout.product_property_link_view, this)

    fun show(caption: String, url: String, tracksEvent: Stat) {
        with(view.findViewById<TextView>(R.id.textLink)) {
            text = caption
        }
        view.setOnClickListener {
            AnalyticsTracker.track(tracksEvent)
            ChromeCustomTabUtils.launchUrl(context, url)
        }
    }
}
