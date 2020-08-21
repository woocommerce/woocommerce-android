package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.TOP_EARNER_PRODUCT_TAPPED
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FormatCurrencyRounded
import com.woocommerce.android.widgets.SkeletonView
import kotlinx.android.synthetic.main.my_store_top_performers.view.*
import kotlinx.android.synthetic.main.top_earner_list_item.view.*
import org.apache.commons.text.StringEscapeUtils
import org.wordpress.android.fluxc.model.leaderboards.WCTopPerformerProductModel
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import org.wordpress.android.util.FormatUtils
import org.wordpress.android.util.PhotonUtils

class MyStoreTopPerformersView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.my_store_top_performers, this)
    }

    private lateinit var selectedSite: SelectedSite
    private lateinit var formatCurrencyForDisplay: FormatCurrencyRounded
    private lateinit var statsCurrencyCode: String

    private var listener: MyStoreStatsListener? = null
    private var skeletonView = SkeletonView()

    fun initView(
        listener: MyStoreStatsListener,
        selectedSite: SelectedSite,
        formatCurrencyForDisplay: FormatCurrencyRounded,
        statsCurrencyCode: String
    ) {
        this.listener = listener
        this.selectedSite = selectedSite
        this.formatCurrencyForDisplay = formatCurrencyForDisplay
        this.statsCurrencyCode = statsCurrencyCode

        topEarners_recycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        topEarners_recycler.adapter = TopEarnersAdapter(
            context,
            formatCurrencyForDisplay,
            statsCurrencyCode,
            listener
        )
        topEarners_recycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        topEarners_recycler.isMotionEventSplittingEnabled = false
    }

    fun removeListener() {
        listener = null
    }

    /**
     * Load top earners stats when tab is selected in [MyStoreStatsView]
     */
    fun loadTopEarnerStats(granularity: StatsGranularity) {
        // Track range change
        AnalyticsTracker.track(
            Stat.DASHBOARD_TOP_PERFORMERS_DATE,
            mapOf(AnalyticsTracker.KEY_RANGE to granularity.toString().toLowerCase())
        )

        topEarners_recycler.adapter = TopEarnersAdapter(
            context,
            formatCurrencyForDisplay,
            statsCurrencyCode,
            listener
        )
        showEmptyView(false)
        showErrorView(false)
        listener?.onRequestLoadTopEarnerStats(granularity)
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

    fun updateView(topPerformers: List<WCTopPerformerProductModel>) {
        (topEarners_recycler.adapter as TopEarnersAdapter).setTopPerformers(topPerformers)
        showEmptyView(topPerformers.isEmpty())
    }

    fun showErrorView(show: Boolean) {
        showEmptyView(false)
        topEarners_error.isVisible = show
        topEarners_recycler.isVisible = !show
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
        val statsCurrencyCode: String,
        val listener: MyStoreStatsListener?
    ) : RecyclerView.Adapter<TopEarnersViewHolder>() {
        private val orderString: String
        private val imageSize: Int
        private val topEarnerList: ArrayList<WCTopPerformerProductModel> = ArrayList()

        init {
            setHasStableIds(true)
            orderString = context.getString(R.string.dashboard_top_earners_total_orders)
            imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
        }

        fun setTopPerformers(newList: List<WCTopPerformerProductModel>) {
            topEarnerList.clear()
            topEarnerList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemCount() = topEarnerList.size

        override fun getItemId(position: Int): Long {
            return topEarnerList[position].id.toLong()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopEarnersViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.top_earner_list_item, parent, false)
            return TopEarnersViewHolder(view)
        }

        override fun onBindViewHolder(holder: TopEarnersViewHolder, position: Int) {
            val topPerformer = topEarnerList[position]
            val numOrders = String.format(orderString, FormatUtils.formatDecimal(topPerformer.quantity))
            val total = formatCurrencyForDisplay(topPerformer.total, statsCurrencyCode)

            holder.productNameText.text = StringEscapeUtils.unescapeHtml4(topPerformer.product.name)
            holder.productOrdersText.text = numOrders
            holder.totalSpendText.text = total
            holder.divider.isVisible = position < itemCount - 1

            val imageUrl = PhotonUtils.getPhotonImageUrl(topPerformer.product.getFirstImageUrl(), imageSize, 0)
            GlideApp.with(holder.itemView.context)
                .load(imageUrl)
                .placeholder(ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_product))
                .into(holder.productImage)

            holder.itemView.setOnClickListener {
                AnalyticsTracker.track(TOP_EARNER_PRODUCT_TAPPED)
                listener?.onTopPerformerClicked(topPerformer)
            }
        }
    }
}
