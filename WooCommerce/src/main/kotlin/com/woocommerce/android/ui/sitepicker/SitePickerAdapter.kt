package com.woocommerce.android.ui.sitepicker

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.view.ViewGroup.MarginLayoutParams
import androidx.annotation.StringRes
import androidx.core.view.isVisible
import androidx.core.view.setMargins
import androidx.core.widget.TextViewCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.SitePickerItemBinding
import com.woocommerce.android.extensions.getSiteName
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.Header
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.NonWooSiteUiModel
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.WooSiteUiModel
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(
    private val onSiteSelected: (SiteModel) -> Unit,
    private val onNonWooSiteSelected: (SiteModel) -> Unit
) : ListAdapter<SitesListItem, RecyclerView.ViewHolder>(SiteUIModelDiffCallback) {
    companion object {
        private const val HEADER_TYPE = 0
        private const val WOO_SITE_TYPE = 1
        private const val NON_WOO_SITE_TYPE = 2
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is Header -> item.label.toLong()
            is WooSiteUiModel -> item.site.siteId
            is NonWooSiteUiModel -> item.site.siteId
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Header -> HEADER_TYPE
            is WooSiteUiModel -> WOO_SITE_TYPE
            is NonWooSiteUiModel -> NON_WOO_SITE_TYPE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            HEADER_TYPE -> HeaderViewHolder(
                MaterialTextView(parent.context).apply {
                    TextViewCompat.setTextAppearance(this, R.style.TextAppearance_Woo_Subtitle2)
                    isAllCaps = true
                    layoutParams = MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                        setMargins(resources.getDimensionPixelSize(R.dimen.major_100))
                    }
                }
            )
            WOO_SITE_TYPE -> WooSiteViewHolder(
                SitePickerItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            NON_WOO_SITE_TYPE -> NonWooSiteViewHolder(
                SitePickerItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
            else -> throw IllegalStateException("Wrong view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Header -> (holder as HeaderViewHolder).bind(item.label)
            is WooSiteUiModel -> (holder as WooSiteViewHolder).bind(item)
            is NonWooSiteUiModel -> (holder as NonWooSiteViewHolder).bind(item)
        }
    }

    private class HeaderViewHolder(val view: MaterialTextView) : RecyclerView.ViewHolder(view) {
        fun bind(@StringRes label: Int) {
            view.setText(label)
        }
    }

    private inner class WooSiteViewHolder(val viewBinding: SitePickerItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.warningIcon.isVisible = false
        }

        fun bind(siteUiModel: WooSiteUiModel) {
            viewBinding.checkIcon.visibility = if (siteUiModel.isSelected) View.VISIBLE else View.INVISIBLE
            viewBinding.textSiteName.text = siteUiModel.site.getSiteName()
            viewBinding.textSiteDomain.text = StringUtils.getSiteDomainAndPath(siteUiModel.site)

            viewBinding.root.setOnClickListener {
                onSiteSelected.invoke(siteUiModel.site)
            }
        }
    }

    private inner class NonWooSiteViewHolder(val viewBinding: SitePickerItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.checkIcon.isVisible = false
            viewBinding.warningIcon.isVisible = true
        }

        fun bind(siteUiModel: NonWooSiteUiModel) {
            viewBinding.textSiteName.text = siteUiModel.site.getSiteName()
            viewBinding.textSiteDomain.text = StringUtils.getSiteDomainAndPath(siteUiModel.site)

            viewBinding.root.setOnClickListener {
                onNonWooSiteSelected.invoke(siteUiModel.site)
            }
        }
    }

    object SiteUIModelDiffCallback : DiffUtil.ItemCallback<SitesListItem>() {
        override fun areItemsTheSame(
            oldItem: SitesListItem,
            newItem: SitesListItem
        ): Boolean = when {
            oldItem is WooSiteUiModel && newItem is WooSiteUiModel -> oldItem.site.siteId == newItem.site.siteId
            oldItem is NonWooSiteUiModel && newItem is NonWooSiteUiModel -> oldItem.site.siteId == newItem.site.siteId
            else -> oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SitesListItem,
            newItem: SitesListItem
        ): Boolean = oldItem == newItem
    }
}
