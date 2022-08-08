package com.woocommerce.android.ui.sitepicker

import android.view.LayoutInflater
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
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SitesListItem.SiteUiModel
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(
    private val onSiteSelected: (SiteModel) -> Unit
) : ListAdapter<SitesListItem, RecyclerView.ViewHolder>(SiteUIModelDiffCallback) {
    companion object {
        private const val HEADER_TYPE = 0
        private const val SITE_TYPE = 1
    }

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return when (val item = getItem(position)) {
            is Header -> item.label.toLong()
            is SiteUiModel -> item.site.siteId
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Header -> HEADER_TYPE
            is SiteUiModel -> SITE_TYPE
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
            SITE_TYPE -> SiteViewHolder(
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
            is SiteUiModel -> (holder as SiteViewHolder).bind(item)
        }
    }

    private class HeaderViewHolder(val view: MaterialTextView) : RecyclerView.ViewHolder(view) {
        fun bind(@StringRes label: Int) {
            view.setText(label)
        }
    }

    private inner class SiteViewHolder(val viewBinding: SitePickerItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.radio.isClickable = false
        }

        fun bind(siteUiModel: SiteUiModel) {
            viewBinding.radio.isVisible = itemCount > 1
            viewBinding.radio.isChecked = siteUiModel.isSelected
            viewBinding.textSiteName.text = siteUiModel.site.getSiteName()
            viewBinding.textSiteDomain.text = StringUtils.getSiteDomainAndPath(siteUiModel.site)

            viewBinding.root.setOnClickListener {
                onSiteSelected.invoke(siteUiModel.site)
            }
        }
    }

    object SiteUIModelDiffCallback : DiffUtil.ItemCallback<SitesListItem>() {
        override fun areItemsTheSame(
            oldItem: SitesListItem,
            newItem: SitesListItem
        ): Boolean {
            return if (oldItem is SiteUiModel && newItem is SiteUiModel) oldItem.site.siteId == newItem.site.siteId
            else oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: SitesListItem,
            newItem: SitesListItem
        ): Boolean = oldItem == newItem
    }
}
