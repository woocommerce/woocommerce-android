package com.woocommerce.android.ui.sitepicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.SitePickerItemBinding
import com.woocommerce.android.extensions.getSiteName
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.SiteViewHolder
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SiteUiModel
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(
    private val onSiteSelected: (SiteModel) -> Unit
) : ListAdapter<SiteUiModel, SiteViewHolder>(SiteUIModelDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = getItem(position).site.siteId

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        return SiteViewHolder(SitePickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SiteViewHolder(val viewBinding: SitePickerItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
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

    object SiteUIModelDiffCallback : DiffUtil.ItemCallback<SiteUiModel>() {
        override fun areItemsTheSame(
            oldItem: SiteUiModel,
            newItem: SiteUiModel
        ): Boolean = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: SiteUiModel,
            newItem: SiteUiModel
        ): Boolean = oldItem == newItem
    }
}
