package com.woocommerce.android.ui.sitepicker

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.SitePickerItemBinding
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.SiteViewHolder
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(private val context: Context, private val listener: OnSiteClickListener) :
        RecyclerView.Adapter<SiteViewHolder>() {
    var siteList: List<SiteModel> = ArrayList()
        set(value) {
            if (!isSameSiteList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }
    var selectedSiteId: Long = 0
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    interface OnSiteClickListener {
        fun onSiteClick(siteId: Long)
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return siteList[position].siteId
    }

    override fun getItemCount(): Int {
        return siteList.size
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
        val site = siteList[position]
        holder.bind(site)
    }

    /**
     * returns true if the passed list of sites is the same as the current list
     */
    private fun isSameSiteList(sites: List<SiteModel>): Boolean {
        if (sites.size != siteList.size) {
            return false
        }

        sites.forEach {
            if (!containsSite(it)) {
                return false
            }
        }

        return true
    }

    /**
     * Returns true if the passed order is in the current list of orders
     */
    private fun containsSite(site: SiteModel): Boolean {
        siteList.forEach {
            if (it.siteId == site.siteId) {
                return true
            }
        }
        return false
    }

    inner class SiteViewHolder(val viewBinding: SitePickerItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.radio.isClickable = false
        }

        fun bind(site: SiteModel) {
            viewBinding.radio.isVisible = siteList.size > 1
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
}
