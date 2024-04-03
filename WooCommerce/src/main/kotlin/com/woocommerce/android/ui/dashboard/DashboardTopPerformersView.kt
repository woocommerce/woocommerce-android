package com.woocommerce.android.ui.dashboard

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.google.android.material.card.MaterialCardView
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.MyStoreTopPerformersBinding
import com.woocommerce.android.databinding.TopPerformersListItemBinding
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.analytics.ranges.StatsTimeRangeSelection.SelectionType
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.widgets.SkeletonView
import java.util.Locale

class DashboardTopPerformersView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : MaterialCardView(ctx, attrs, defStyleAttr) {
    private val binding = MyStoreTopPerformersBinding.inflate(LayoutInflater.from(ctx), this, true)

    private lateinit var selectedSite: SelectedSite
    private lateinit var dateUtils: DateUtils

    private var skeletonView = SkeletonView()

    private val lastUpdated
        get() = binding.lastUpdatedTextView

    fun initView(
        selectedSite: SelectedSite,
        dateUtils: DateUtils,
    ) {
        this.selectedSite = selectedSite
        this.dateUtils = dateUtils
        binding.topPerformersRecycler.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
        binding.topPerformersRecycler.adapter = TopPerformersAdapter()
        binding.topPerformersRecycler.itemAnimator = androidx.recyclerview.widget.DefaultItemAnimator()

        // Setting this field to false ensures that the RecyclerView children do NOT receive the multiple clicks,
        // and only processes the first click event. More details on this issue can be found here:
        // https://github.com/woocommerce/woocommerce-android/issues/2074
        binding.topPerformersRecycler.isMotionEventSplittingEnabled = false
    }

    fun onDateGranularityChanged(selectionType: SelectionType) {
        AnalyticsTracker.track(
            AnalyticsEvent.DASHBOARD_TOP_PERFORMERS_DATE,
            mapOf(AnalyticsTracker.KEY_RANGE to selectionType.identifier)
        )
        binding.topPerformersRecycler.adapter = TopPerformersAdapter()
        showEmptyView(false)
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

    fun showLastUpdate(lastUpdateMillis: Long?) {
        if (lastUpdateMillis != null) {
            val lastUpdateFormatted = dateUtils.getDateOrTimeFromMillis(lastUpdateMillis)
            lastUpdated.isVisible = true
            lastUpdated.text = String.format(
                Locale.getDefault(),
                resources.getString(R.string.last_update),
                lastUpdateFormatted
            )
        } else {
            lastUpdated.isVisible = false
        }
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
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: TopPerformersViewHolder, position: Int) {
            val topPerformer = getItem(position)
            val imageCornerRadius = holder.itemView.context.resources.getDimensionPixelSize(R.dimen.corner_radius_image)
            holder.viewBinding.textProductName.text = topPerformer.name
            holder.viewBinding.itemsSoldTextView.text = topPerformer.timesOrdered
            holder.viewBinding.netSalesTextView.text = topPerformer.netSales
            holder.viewBinding.divider.isVisible = position < itemCount - 1
            Glide.with(holder.itemView.context)
                .load(topPerformer.imageUrl)
                .transform(CenterCrop(), RoundedCorners(imageCornerRadius))
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
