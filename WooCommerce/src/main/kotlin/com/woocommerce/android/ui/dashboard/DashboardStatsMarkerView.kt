package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.view.View
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.dashboard_stats_marker_view.view.*

/**
 * Custom MarkerView which appears on the stats chart when the user taps a bar
 */
class DashboardStatsMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    interface RequestMarkerCaptionListener {
        fun onRequestMarkerCaption(entry: Entry): String?
    }

    var captionListener: RequestMarkerCaptionListener? = null

    init {
        // set the "bubble" image's minHeight based on the size of the text with padding
        val paddingMed = resources.getDimensionPixelSize(R.dimen.margin_medium)
        val paddingSm = resources.getDimensionPixelSize(R.dimen.margin_small)
        val textSz = resources.getDimensionPixelSize(R.dimen.text_small)
        markerImage.minimumHeight =
                (paddingMed * 4) +  // padding around text
                (textSz * 2) +      // size of two lines of text
                paddingSm           // extra padding for bubble
    }

    override fun refreshContent(entry: Entry, highlight: Highlight) {
        if (entry.y <= 0) {
            markerContainer.visibility = View.GONE
        } else {
            tvContent.text = captionListener?.onRequestMarkerCaption(entry)
            markerContainer.visibility = View.VISIBLE
        }
        super.refreshContent(entry, highlight)
    }

    override fun getOffset(): MPPointF {
        // center the marker horizontally and vertically
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
