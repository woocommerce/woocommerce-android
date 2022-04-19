package com.woocommerce.android.ui.sitepicker

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.SitePickerItemBinding
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.SiteViewHolder
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(private val context: Context, private val listener: OnSiteClickListener) :
    ListAdapter<SiteModel, SiteViewHolder>(SiteModelDiffCallBack) {
    var selectedSiteId: Long = 0
        set(value) {
            if (field != value) {
                val oldPos = indexOfSite(field)
                val newPos = indexOfSite(value)
                field = value
                if (oldPos > -1) {
                    notifyItemChanged(oldPos)
                }
                if (newPos > -1) {
                    notifyItemChanged(newPos)
                }
            }
        }

    interface OnSiteClickListener {
        fun onSiteClick(siteId: Long)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long = getItem(position).siteId

    private fun indexOfSite(siteId: Long): Int {
        for (index in 0 until itemCount) {
            if (getItem(index).siteId == siteId) {
                return index
            }
        }
        return -1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        return SiteViewHolder(
            SitePickerItemBinding.inflate(
                LayoutInflater.from(context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = getItem(position)
        holder.bind(site)
    }

    inner class SiteViewHolder(val viewBinding: SitePickerItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.radio.isClickable = false
        }

        fun bind(site: SiteModel) {
            viewBinding.radio.isVisible = itemCount > 1
            viewBinding.radio.isChecked = site.siteId == selectedSiteId
            viewBinding.textSiteName.text =
                if (!TextUtils.isEmpty(site.name)) site.name else context.getString(R.string.untitled)
            viewBinding.textSiteDomain.text = StringUtils.getSiteDomainAndPath(site)
            if (itemCount > 1) {
                viewBinding.root.setOnClickListener {
                    if (selectedSiteId != site.siteId) {
                        listener.onSiteClick(site.siteId)
                        selectedSiteId = site.siteId
                    }
                }
            } else {
                viewBinding.root.setOnClickListener(null)
            }
        }
    }

    object SiteModelDiffCallBack : DiffUtil.ItemCallback<SiteModel>() {
        override fun areItemsTheSame(
            oldItem: SiteModel,
            newItem: SiteModel
        ): Boolean = oldItem.siteId == newItem.siteId

        override fun areContentsTheSame(
            oldItem: SiteModel,
            newItem: SiteModel
        ): Boolean = areItemsTheSame(oldItem, newItem)
    }
}
