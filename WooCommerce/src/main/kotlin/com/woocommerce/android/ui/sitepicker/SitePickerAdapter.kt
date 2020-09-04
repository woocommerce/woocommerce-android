package com.woocommerce.android.ui.sitepicker

import android.content.Context
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.SiteViewHolder
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.site_picker_item.view.*
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
        return SiteViewHolder(LayoutInflater.from(context).inflate(R.layout.site_picker_item, parent, false))
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = siteList[position]
        holder.radio.isVisible = siteList.size > 1
        holder.radio.isChecked = site.siteId == selectedSiteId
        holder.txtSiteName.text = if (!TextUtils.isEmpty(site.name)) site.name else context.getString(R.string.untitled)
        holder.txtSiteDomain.text = StringUtils.getSiteDomainAndPath(site)
        if (itemCount > 1) {
            holder.itemView.setOnClickListener {
                if (selectedSiteId != site.siteId) {
                    listener.onSiteClick(site.siteId)
                    selectedSiteId = site.siteId
                }
            }
        } else {
            holder.itemView.setOnClickListener(null)
        }
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

    class SiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radio: RadioButton = view.radio
        val txtSiteName: TextView = view.text_site_name
        val txtSiteDomain: TextView = view.text_site_domain

        init {
            radio.isClickable = false
        }
    }
}
