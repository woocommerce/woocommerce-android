package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ShippingPackageListHeaderBinding
import com.woocommerce.android.databinding.ShippingPackageSelectableListItemBinding
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCreateServicePackageViewModel.ServicePackageUiModel

class ShippingLabelServicePackageAdapter(
    private val onChecked: (String) -> Unit,
    private val dimensionUnit: String
) :
    ListAdapter<ShippingLabelServicePackageAdapter.ListItem, RecyclerView.ViewHolder>(DiffCallback) {
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PACKAGE = 1
    }

    fun updateData(uiModels: List<ServicePackageUiModel>) {
        val items = uiModels
            .groupBy { it.data.category }
            .flatMap { entry ->
                val list = mutableListOf<ListItem>()
                list.add(ListItem.Header(entry.key))
                list.addAll(entry.value.map { ListItem.Package(it) })
                list
            }

        submitList(items)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_HEADER -> {
                HeaderViewHolder(
                    ShippingPackageListHeaderBinding.inflate(
                        layoutInflater,
                        parent,
                        false
                    )
                )
            }
            VIEW_TYPE_PACKAGE -> {
                PackageViewHolder(ShippingPackageSelectableListItemBinding.inflate(layoutInflater, parent, false))
            }
            else -> throw IllegalArgumentException()
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is ListItem.Package -> (holder as PackageViewHolder).bind(item.uiModel)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is ListItem.Header -> VIEW_TYPE_HEADER
            is ListItem.Package -> VIEW_TYPE_PACKAGE
        }
    }

    private class HeaderViewHolder(private val binding: ShippingPackageListHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(title: String) {
            binding.root.text = title
        }
    }

    private inner class PackageViewHolder(private val binding: ShippingPackageSelectableListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(uiModel: ServicePackageUiModel) {
            binding.title.text = uiModel.data.title
            val dimensions = uiModel.data.dimensions
            binding.dimensions.text = "${dimensions.length} x ${dimensions.width} x ${dimensions.height} $dimensionUnit"
            binding.packageRadioButton.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) onChecked(uiModel.data.id)
            }
            binding.packageRadioButton.isChecked = uiModel.isChecked
        }
    }

    object DiffCallback : DiffUtil.ItemCallback<ListItem>() {
        override fun areItemsTheSame(oldItem: ListItem, newItem: ListItem) =
            when (oldItem) {
                is ListItem.Header -> oldItem.title == (newItem as ListItem.Header).title
                is ListItem.Package -> oldItem.uiModel.data.id == (newItem as ListItem.Package).uiModel.data.id
            }

        override fun areContentsTheSame(oldItem: ListItem, newItem: ListItem) =
            oldItem == newItem
    }

    sealed class ListItem {
        data class Header(val title: String) : ListItem()
        data class Package(val uiModel: ServicePackageUiModel) : ListItem()
    }
}
