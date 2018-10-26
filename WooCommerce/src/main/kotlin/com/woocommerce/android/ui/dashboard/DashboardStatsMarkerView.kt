package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.view.View
import android.widget.TextView
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

    private val tvContent: TextView = findViewById(R.id.tvContent)
    var captionListener: RequestMarkerCaptionListener? = null

    override fun refreshContent(entry: Entry, highlight: Highlight) {
        if (entry.y <= 0) {
            marker_container.visibility = View.GONE
        } else {
            tvContent.text = captionListener?.onRequestMarkerCaption(entry)
            marker_container.visibility = View.VISIBLE
        }
        super.refreshContent(entry, highlight)
    }

    override fun getOffset(): MPPointF {
        // center the marker horizontally and vertically
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
