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
import com.woocommerce.android.ui.login.SitePickerAdapter.SiteViewHolder
import kotlinx.android.synthetic.main.site_list_item.view.*
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.UrlUtils

class SitePickerAdapter(private val context: Context) :
        RecyclerView.Adapter<SiteViewHolder>() {
    private val siteList : ArrayList<SiteModel> = ArrayList()
    private var selectedSiteId : Long = 0

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
        holder.txtSiteDomain.text = UrlUtils.getHost(site.url)
    }

    class SiteViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val radio: RadioButton = view.radio
        val txtSiteName: TextView = view.text_site_name
        val txtSiteDomain: TextView = view.text_site_domain
    }

    fun setSites(selectedSiteId: Long, sites : List<SiteModel>) {
        siteList.clear()
        siteList.addAll(sites)
        this.selectedSiteId = selectedSiteId
        notifyDataSetChanged()
    }
}
