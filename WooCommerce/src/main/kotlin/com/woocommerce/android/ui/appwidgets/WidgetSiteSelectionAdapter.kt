package com.woocommerce.android.ui.appwidgets

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.di.GlideRequests
import com.woocommerce.android.ui.widgets.WidgetSiteSelectionAdapter.WidgetSiteSelectionViewHolder
import com.woocommerce.android.ui.widgets.stats.today.TodayWidgetConfigureViewModel.SiteUiModel
import kotlinx.android.synthetic.main.widget_site_selector_list_item.view.*
import org.wordpress.android.util.PhotonUtils

class WidgetSiteSelectionAdapter(
    private val context: Context,
    private val glideRequest: GlideRequests,
    private val listener: OnWidgetSiteSelectedListener
) : RecyclerView.Adapter<WidgetSiteSelectionViewHolder>() {
    private val imageSize = context.resources.getDimensionPixelSize(R.dimen.image_minor_100)
    private var sites = mutableListOf<SiteUiModel>()

    interface OnWidgetSiteSelectedListener {
        fun onSiteSelected(site: SiteUiModel)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetSiteSelectionViewHolder {
        return WidgetSiteSelectionViewHolder(
            LayoutInflater.from(context).inflate(R.layout.widget_site_selector_list_item, parent, false)
        )
    }

    override fun getItemCount(): Int {
        return sites.size
    }

    override fun onBindViewHolder(holder: WidgetSiteSelectionViewHolder, position: Int) {
        val site = sites[position]
        holder.txtSiteName.text = site.title
        holder.txtSiteDomain.text = site.url

        site.iconUrl?.let {
            val imageUrl = PhotonUtils.getPhotonImageUrl(it, imageSize, imageSize)
            glideRequest.load(imageUrl)
                .placeholder(R.drawable.ic_product)
                .into(holder.imageView)
        } ?: holder.imageView.setImageResource(R.drawable.ic_gridicons_globe)

        holder.itemView.setOnClickListener {
            listener.onSiteSelected(site)
        }
    }

    fun update(updatedSites: List<SiteUiModel>) {
        sites.clear()
        sites.addAll(updatedSites)
        notifyDataSetChanged()
    }

    class WidgetSiteSelectionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView: ImageView = view.widget_site_image
        val txtSiteName: TextView = view.widget_site_name
        val txtSiteDomain: TextView = view.widget_site_url
    }
}
