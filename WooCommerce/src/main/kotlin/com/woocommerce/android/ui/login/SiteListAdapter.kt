package com.woocommerce.android.ui.login

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.SiteListAdapter.SiteViewHolder
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.site_list_item.view.*
import org.wordpress.android.fluxc.model.SiteModel

class SiteListAdapter(private val context: Context, private val listener: OnSiteClickListener) :
        RecyclerView.Adapter<SiteViewHolder>() {
    var siteList: List<SiteModel> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
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
        return SiteViewHolder(LayoutInflater.from(context).inflate(R.layout.site_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val site = siteList[position]
        holder.radio.visibility = if (siteList.size > 1) View.VISIBLE else View.GONE
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

    class SiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radio: RadioButton = view.radio
        val txtSiteName: TextView = view.text_site_name
        val txtSiteDomain: TextView = view.text_site_domain
    }
}
