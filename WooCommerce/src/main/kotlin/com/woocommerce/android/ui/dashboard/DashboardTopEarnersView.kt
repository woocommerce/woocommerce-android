package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.design.widget.TabLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import kotlinx.android.synthetic.main.dashboard_top_earners.view.*
import kotlinx.android.synthetic.main.top_earner_list_item.view.*
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.UrlUtils

class DashboardTopEarnersView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_top_earners, this)
    }

    companion object {
        private val DEFAULT_STATS_GRANULARITY = StatsGranularity.DAYS
    }

    var activeGranularity: StatsGranularity = DEFAULT_STATS_GRANULARITY
        get() {
            return topEarners_tab_layout.getTabAt(topEarners_tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private lateinit var selectedSite: SelectedSite

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: DashboardStatsListener,
        selectedSite: SelectedSite
    ) {
        this.selectedSite = selectedSite

        topEarners_recycler.layoutManager = LinearLayoutManager(context)

        StatsGranularity.values().forEach { granularity ->
            val tab = topEarners_tab_layout.newTab().apply {
                setText(DashboardUtils.getStringForGranularity(granularity))
                tag = granularity
            }
            topEarners_tab_layout.addTab(tab)

            // Start with the given time period selected
            if (granularity == period) {
                tab.select()
            }
        }

        topEarners_tab_layout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                listener.onRequestLoadTopEarnerStats(tab.tag as StatsGranularity)
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}

            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun updateView(topEarnerList: List<WCTopEarnerModel>) {
        topEarners_recycler.adapter = TopEarnersAdapter(context, topEarnerList)
    }

    class TopEarnersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var productNameText: TextView = view.text_ProductName
        var productOrdersText: TextView = view.text_ProductOrders
        var totalSpendText: TextView = view.text_TotalSpend
        var productImage: ImageView = view.image_product
    }

    class TopEarnersAdapter(context: Context, private val topEarnerList: List<WCTopEarnerModel>)
        : RecyclerView.Adapter<TopEarnersViewHolder>() {
        private val orderString: String
        private val imageSize: Int

        init {
            setHasStableIds(true)
            orderString = context.getString(R.string.dashboard_top_earners_total_orders)
            imageSize = context.resources.getDimensionPixelSize(R.dimen.top_earner_product_image_sz)
        }

        override fun getItemCount() = topEarnerList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopEarnersViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.top_earner_list_item, parent, false)
            return TopEarnersViewHolder(view)
        }

        override fun onBindViewHolder(holder: TopEarnersViewHolder, position: Int) {
            val topEarner = topEarnerList[position]
            holder.productNameText.text = topEarner.name
            holder.productOrdersText.text = String.format(orderString, topEarner.quantity)
            holder.totalSpendText.text = topEarner.price.toString() // TODO: format using currency

            // strip the image query params and add a width param that matches our desired size
            val imageUrl = UrlUtils.removeQuery(topEarner.image) + "?w=${imageSize}"
            GlideApp.with(holder.itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.productImage)
        }
    }
}
