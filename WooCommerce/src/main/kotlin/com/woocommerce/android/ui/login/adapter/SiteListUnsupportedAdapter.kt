package com.woocommerce.android.ui.login.adapter

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.woocommerce.android.R
import com.woocommerce.android.ui.login.adapter.SiteListUnsupportedAdapter.SiteViewUnsupportedHolder
import com.woocommerce.android.util.StringUtils
import kotlinx.android.synthetic.main.site_list_unsupported_item.view.*
import org.wordpress.android.fluxc.model.SiteModel

class SiteListUnsupportedAdapter(private val context: Context) :
        RecyclerView.Adapter<SiteViewUnsupportedHolder>() {
    var siteList: List<SiteModel> = ArrayList()
        set(value) {
            field = value
            notifyDataSetChanged()
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewUnsupportedHolder {
        return SiteViewUnsupportedHolder(
                LayoutInflater.from(context).inflate(R.layout.site_list_unsupported_item, parent, false))
    }

    override fun onBindViewHolder(holder: SiteViewUnsupportedHolder, position: Int) {
        val site = siteList[position]
        holder.txtSiteName.text = if (!TextUtils.isEmpty(site.name)) site.name else context.getString(R.string.untitled)
        holder.txtSiteDomain.text = StringUtils.getSiteDomainAndPath(site)
        holder.itemView.setOnClickListener(null)
    }

    class SiteViewUnsupportedHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtSiteName: TextView = view.text_site_name
        val txtSiteDomain: TextView = view.text_site_domain
    }
}
