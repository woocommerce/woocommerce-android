package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.support.annotation.StringRes
import android.support.design.widget.TabLayout
import android.support.v7.widget.DefaultItemAnimator
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
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardUtils.DEFAULT_STATS_GRANULARITY
import com.woocommerce.android.util.FormatCurrencyRounded
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dashboard_top_earners.view.*
import kotlinx.android.synthetic.main.top_earner_list_item.view.*
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.UrlUtils

class DashboardTopEarnersView @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null)
    : LinearLayout(ctx, attrs) {
    init {
        View.inflate(context, R.layout.dashboard_top_earners, this)
    }

    var activeGranularity: StatsGranularity = DEFAULT_STATS_GRANULARITY
        get() {
            return topEarners_tab_layout.getTabAt(topEarners_tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: DEFAULT_STATS_GRANULARITY
        }

    private lateinit var selectedSite: SelectedSite
    private lateinit var formatCurrencyForDisplay: FormatCurrencyRounded

    private var skeletonView = SkeletonView()

    fun initView(
        period: StatsGranularity = DEFAULT_STATS_GRANULARITY,
        listener: DashboardStatsListener,
        selectedSite: SelectedSite,
        formatCurrencyForDisplay: FormatCurrencyRounded
    ) {
        this.selectedSite = selectedSite
        this.formatCurrencyForDisplay = formatCurrencyForDisplay

        topEarners_recycler.layoutManager = LinearLayoutManager(context)
        topEarners_recycler.adapter = TopEarnersAdapter(context, formatCurrencyForDisplay)
        topEarners_recycler.itemAnimator = DefaultItemAnimator()

        StatsGranularity.values().forEach { granularity ->
            val tab = topEarners_tab_layout.newTab().apply {
                setText(getTabTitleResForGranularity(granularity))
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
                // Track range change
                AnalyticsTracker.track(
                        Stat.DASHBOARD_TOP_PERFORMERS_DATE,
                        mapOf(AnalyticsTracker.KEY_RANGE to tab.tag.toString().toLowerCase()))

                topEarners_recycler.adapter = TopEarnersAdapter(context, formatCurrencyForDisplay)
                showEmptyView(false)
                showErrorView(false)
                listener.onRequestLoadTopEarnerStats(tab.tag as StatsGranularity)
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(dashboard_top_earners_container, R.layout.skeleton_dashboard_top_earners, delayed = true)
        } else {
            skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        topEarners_emptyView.visibility = if (show) View.VISIBLE else View.GONE
    }

    fun updateView(topEarnerList: List<WCTopEarnerModel>) {
        (topEarners_recycler.adapter as TopEarnersAdapter).setTopEarnersList(topEarnerList)
        showEmptyView(topEarnerList.isEmpty())
    }

    fun showErrorView(show: Boolean) {
        showEmptyView(false)
        topEarners_error.visibility = if (show) View.VISIBLE else View.GONE
        topEarners_recycler.visibility = if (show) View.GONE else View.VISIBLE
    }

    @StringRes
    private fun getTabTitleResForGranularity(granularity: StatsGranularity): Int {
        return when (granularity) {
            StatsGranularity.DAYS -> R.string.today
            StatsGranularity.WEEKS -> R.string.this_week
            StatsGranularity.MONTHS -> R.string.this_month
            StatsGranularity.YEARS -> R.string.this_year
        }
    }

    class TopEarnersViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var productNameText: TextView = view.text_ProductName
        var productOrdersText: TextView = view.text_ProductOrders
        var totalSpendText: TextView = view.text_TotalSpend
        var productImage: ImageView = view.image_product
        var divider: View = view.divider
    }

    class TopEarnersAdapter(
        context: Context,
        val formatCurrencyForDisplay: FormatCurrencyRounded
    ) : RecyclerView.Adapter<TopEarnersViewHolder>() {
        private val orderString: String
        private val imageSize: Int
        private val topEarnerList: ArrayList<WCTopEarnerModel> = ArrayList()

        init {
            setHasStableIds(true)
            orderString = context.getString(R.string.dashboard_top_earners_total_orders)
            imageSize = context.resources.getDimensionPixelSize(R.dimen.top_earner_product_image_sz)
        }

        fun setTopEarnersList(newList: List<WCTopEarnerModel>) {
            topEarnerList.clear()
            topEarnerList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemCount() = topEarnerList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopEarnersViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.top_earner_list_item, parent, false)
            return TopEarnersViewHolder(view)
        }

        override fun onBindViewHolder(holder: TopEarnersViewHolder, position: Int) {
            val topEarner = topEarnerList[position]
            val numOrders = String.format(orderString, FormatUtils.formatDecimal(topEarner.quantity))
            val total = formatCurrencyForDisplay(topEarner.total, topEarner.currency)

            holder.productNameText.text = StringEscapeUtils.unescapeHtml4(topEarner.name)
            holder.productOrdersText.text = numOrders
            holder.totalSpendText.text = total
            holder.divider.visibility = if (position < itemCount - 1) View.VISIBLE else View.GONE

            // strip the image query params and add a width param that matches our desired size
            val imageUrl = UrlUtils.removeQuery(topEarner.image) + "?w=$imageSize"
            GlideApp.with(holder.itemView.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_product)
                    .into(holder.productImage)
        }
    }
}
