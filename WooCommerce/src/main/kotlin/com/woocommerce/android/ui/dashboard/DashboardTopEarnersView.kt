package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.tabs.TabLayout
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.dashboard.DashboardFragment.Companion.DEFAULT_STATS_GRANULARITY
import com.woocommerce.android.util.FormatCurrencyRounded
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.dashboard_top_earners.view.*
import kotlinx.android.synthetic.main.top_earner_list_item.view.*
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.WCTopEarnerModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils
import java.io.Serializable

class DashboardTopEarnersView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.dashboard_top_earners, this)
    }
    var tabStateStats: Serializable? = null // Save the current position of top earners tab view

    var activeGranularity: StatsGranularity = DEFAULT_STATS_GRANULARITY
        get() {
            return topEarners_tab_layout.getTabAt(topEarners_tab_layout.selectedTabPosition)?.let {
                it.tag as StatsGranularity
            } ?: tabStateStats?.let { it as StatsGranularity } ?: DEFAULT_STATS_GRANULARITY
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

        topEarners_recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        topEarners_recycler.adapter = TopEarnersAdapter(context, formatCurrencyForDisplay, listener)
        topEarners_recycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

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

                topEarners_recycler.adapter = TopEarnersAdapter(context, formatCurrencyForDisplay, listener)
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
        topEarners_emptyView.isVisible = show
    }

    fun updateView(topEarnerList: List<WCTopEarnerModel>) {
        (topEarners_recycler.adapter as TopEarnersAdapter).setTopEarnersList(topEarnerList)
        showEmptyView(topEarnerList.isEmpty())
    }

    fun showErrorView(show: Boolean) {
        showEmptyView(false)
        topEarners_error.isVisible = show
        topEarners_recycler.isVisible = !show
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
        val formatCurrencyForDisplay: FormatCurrencyRounded,
        val listener: DashboardStatsListener
    ) : RecyclerView.Adapter<TopEarnersViewHolder>() {
        private val orderString: String
        private val imageSize: Int
        private val topEarnerList: ArrayList<WCTopEarnerModel> = ArrayList()

        init {
            setHasStableIds(true)
            orderString = context.getString(R.string.dashboard_top_earners_total_orders)
            imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
        }

        fun setTopEarnersList(newList: List<WCTopEarnerModel>) {
            topEarnerList.clear()
            topEarnerList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemCount() = topEarnerList.size

        override fun getItemId(position: Int): Long {
            return topEarnerList[position].id
        }

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
            holder.divider.isVisible = position < itemCount - 1

            val imageUrl = PhotonUtils.getPhotonImageUrl(topEarner.image, imageSize, 0)
            GlideApp.with(holder.itemView.context)
                    .load(imageUrl)
                    .placeholder(ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_product))
                    .into(holder.productImage)

            holder.itemView.setOnClickListener {
                AnalyticsTracker.track(TOP_EARNER_PRODUCT_TAPPED)
                listener.onTopEarnerClicked(topEarner)
            }
        }
    }
}
