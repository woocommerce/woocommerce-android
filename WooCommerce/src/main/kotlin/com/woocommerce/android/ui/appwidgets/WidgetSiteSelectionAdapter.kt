package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.WidgetSiteSelectorListItemBinding
import com.woocommerce.android.di.GlideRequests
import com.woocommerce.android.ui.appwidgets.WidgetSiteSelectionAdapter.WidgetSiteSelectionViewHolder
import com.woocommerce.android.ui.appwidgets.stats.today.TodayWidgetConfigureViewModel.SiteUiModel
import org.wordpress.android.util.PhotonUtils

class WidgetSiteSelectionAdapter(
    context: Context,
    private val glideRequest: GlideRequests,
    private val listener: OnWidgetSiteSelectedListener
) : RecyclerView.Adapter<WidgetSiteSelectionViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private var sites = mutableListOf<SiteUiModel>()

    interface OnWidgetSiteSelectedListener {
        fun onSiteSelected(site: SiteUiModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        WidgetSiteSelectionViewHolder(WidgetSiteSelectorListItemBinding.bind(parent))

    override fun getItemCount() = sites.size

    override fun onBindViewHolder(holder: WidgetSiteSelectionViewHolder, position: Int) {
        val site = sites[position]
        holder.bind(site)

        holder.itemView.setOnClickListener {
            listener.onSiteSelected(site)
        }
    }

    fun update(updatedSites: List<SiteUiModel>) {
        sites.clear()
        sites.addAll(updatedSites)
        notifyDataSetChanged()
    }

    inner class WidgetSiteSelectionViewHolder(val viewBinding: WidgetSiteSelectorListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(site: SiteUiModel) {
            viewBinding.widgetSiteName.text = site.title
            viewBinding.widgetSiteUrl.text = site.url

            site.iconUrl?.let {
                val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
                glideRequest.load(imageUrl)
                    .placeholder(R.drawable.ic_gridicons_globe)
                    .into(viewBinding.widgetSiteImage)
            } ?: viewBinding.widgetSiteImage.setImageResource(R.drawable.ic_gridicons_globe)
        }
    }
}
