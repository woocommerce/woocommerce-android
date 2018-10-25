package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.woocommerce.android.R

class DashboardStatsMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvContent: TextView

    init {
        tvContent = findViewById(R.id.tvContent)
    }

    // callbacks everytime the MarkerView is redrawn, can be used to update the
    // content (user-interface)
    override fun refreshContent(entry: Entry, highlight: Highlight) {
        // tvContent.text = e.data?.toString()
        tvContent.text = entry.toString()
    }

    override fun getOffset(): MPPointF {
        // center the marker horizontally and vertically
        return MPPointF((-(width / 2)).toFloat(), (-height).toFloat())
    }
}
