package com.woocommerce.android.ui.analytics.hub.listcard

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.analytics.hub.listcard.AnalyticsHubListAdapter.ViewHolder

class AnalyticsHubListAdapter(
    private val items: List<AnalyticsHubListCardItemViewState>,
) : RecyclerView.Adapter<ViewHolder>() {
    class ViewHolder(val view: AnalyticsHubListCardItemView) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.analytics_list_card_item, parent, false)
            as AnalyticsHubListCardItemView
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.view.cardElevation = holder.itemView.resources.getDimension(R.dimen.minor_00)
        holder.view.setInformation(items[position])
    }

    override fun getItemCount() = items.size
}
