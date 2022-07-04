package com.woocommerce.android.ui.mystore

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.MyStoreTopPerformersBinding
import com.woocommerce.android.databinding.TopPerformersListItemBinding
import com.woocommerce.android.di.GlideApp
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.widgets.SkeletonView
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity
import java.util.*

class MyStoreTopPerformersView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = MyStoreTopPerformersBinding.inflate(LayoutInflater.from(ctx), this, true)

    private lateinit var selectedSite: SelectedSite

    private var skeletonView = SkeletonView()

    fun initView(selectedSite: SelectedSite) {
        this.selectedSite = selectedSite

        binding.topPerformersRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.topPerformersRecycler.adapter = TopPerformersAdapter()
        binding.topPerformersRecycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        binding.topPerformersRecycler.isMotionEventSplittingEnabled = false
    }

    fun onDateGranularityChanged(granularity: StatsGranularity) {
        trackDateRangeChanged(granularity)
        binding.topPerformersRecycler.adapter = TopPerformersAdapter()
        showEmptyView(false)
    }

    private fun trackDateRangeChanged(granularity: StatsGranularity) {
        AnalyticsTracker.track(
            AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_DATE,
            mapOf(AnalyticsTracker.KEY_RANGE to granularity.toString().lowercase(Locale.getDefault()))
        )
    }

    fun showSkeleton(show: Boolean) {
        if (show) {
            skeletonView.show(
                binding.topPerformersLinearLayout,
                R.layout.skeleton_dashboard_top_performers,
                delayed = true
            )
        } else {
            skeletonView.hide()
        }
    }

    private fun showEmptyView(show: Boolean) {
        binding.topPerformersEmptyViewLinearLayout.isVisible = show
    }

    fun updateView(topPerformers: List<TopPerformerProductUiModel>) {
        (binding.topPerformersRecycler.adapter as TopPerformersAdapter).submitList(topPerformers)
        showEmptyView(topPerformers.isEmpty())
    }

    fun showErrorView(show: Boolean) {
        showEmptyView(false)
        binding.topPerformersEmptyViewLinearLayout.isVisible = show
        binding.topPerformersRecycler.isVisible = !show
    }

    class TopPerformersViewHolder(val viewBinding: TopPerformersListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root)

    class TopPerformersAdapter : ListAdapter<TopPerformerProductUiModel, TopPerformersViewHolder>(ItemDiffCallback) {
        init {
            setHasStableIds(true)
        }

        override fun getItemId(position: Int): Long = getItem(position).productId

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TopPerformersViewHolder {
            return TopPerformersViewHolder(
                TopPerformersListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent, false
                )
            )
        }

        override fun onBindViewHolder(holder: TopPerformersViewHolder, position: Int) {
            val topPerformer = getItem(position)
            holder.viewBinding.textProductName.text = topPerformer.name
            holder.viewBinding.itemsSoldTextView.text = topPerformer.timesOrdered
            holder.viewBinding.netSalesTextView.text = topPerformer.netSales
            holder.viewBinding.divider.isVisible = position < itemCount - 1
            GlideApp.with(holder.itemView.context)
                .load(topPerformer.imageUrl)
                .placeholder(ContextCompat.getDrawable(holder.itemView.context, R.drawable.ic_product))
                .into(holder.viewBinding.imageProduct)

            holder.itemView.setOnClickListener {
                topPerformer.onClick(topPerformer.productId)
            }
        }
    }

    object ItemDiffCallback : DiffUtil.ItemCallback<TopPerformerProductUiModel>() {
        override fun areItemsTheSame(
            oldItem: TopPerformerProductUiModel,
            newItem: TopPerformerProductUiModel
        ): Boolean {
            return oldItem.productId == newItem.productId
        }

        override fun areContentsTheSame(
            oldItem: TopPerformerProductUiModel,
            newItem: TopPerformerProductUiModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
