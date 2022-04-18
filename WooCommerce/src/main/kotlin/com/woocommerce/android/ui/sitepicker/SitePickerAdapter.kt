package com.woocommerce.android.ui.sitepicker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.SitePickerItemBinding
import com.woocommerce.android.extensions.getSiteName
import com.woocommerce.android.ui.sitepicker.SitePickerAdapter.SiteViewHolder
import com.woocommerce.android.ui.sitepicker.SitePickerViewModel.SiteUiModel
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.fluxc.model.SiteModel

class SitePickerAdapter(
    private val onSiteSelected: (SiteModel) -> Unit
) : RecyclerView.Adapter<SiteViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var sites: List<SiteUiModel> = ArrayList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                SiteDiffUtil(
                    field,
                    value
                ),
                true
            )
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount(): Int = sites.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        return SiteViewHolder(SitePickerItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        holder.bind(sites[position])
    }

    inner class SiteViewHolder(val viewBinding: SitePickerItemBinding) : RecyclerView.ViewHolder(viewBinding.root) {
        init {
            viewBinding.radio.isClickable = false
        }
        fun bind(siteUiModel: SiteUiModel) {
            viewBinding.radio.isVisible = sites.size > 1
            viewBinding.radio.isChecked = siteUiModel.isSelected
            viewBinding.textSiteName.text = siteUiModel.site.getSiteName()
            viewBinding.textSiteDomain.text = StringUtils.getSiteDomainAndPath(siteUiModel.site)

            viewBinding.root.setOnClickListener {
                onSiteSelected.invoke(siteUiModel.site)
            }
        }
    }

    private class SiteDiffUtil(
        private val oldList: List<SiteUiModel>,
        private val newList: List<SiteUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            oldList[oldItemPosition] == newList[newItemPosition]

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            areItemsTheSame(oldItemPosition, newItemPosition)
    }
}
