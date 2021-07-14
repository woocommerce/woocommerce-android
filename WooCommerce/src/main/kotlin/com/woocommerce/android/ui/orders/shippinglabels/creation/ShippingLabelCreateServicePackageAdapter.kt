package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.databinding.ShippingPackageListHeaderBinding
import com.woocommerce.android.databinding.ShippingPackageSelectableListItemBinding
import com.woocommerce.android.model.ShippingPackage

class ShippingLabelCreateServicePackageAdapter : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_PACKAGE = 1
    }

    private var items: List<ListItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
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

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val item = items[position]) {
            is ListItem.Header -> (holder as HeaderViewHolder).bind(item.title)
            is ListItem.Package -> (holder as PackageViewHolder).bind(item.data)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
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

    private class PackageViewHolder(private val binding: ShippingPackageSelectableListItemBinding) :
        ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(shippingPackage: ShippingPackage) {
            binding.title.text = shippingPackage.title
            val dimensions = shippingPackage.dimensions
            binding.dimensions.text = "${dimensions.length} x ${dimensions.width} x ${dimensions.height}"
        }
    }

    fun updatePackages(selectablePackages: List<ShippingPackage>) {
        items = selectablePackages.groupBy { it.category }.flatMap { entry ->
            val list = mutableListOf<ListItem>()
            list.add(ListItem.Header(entry.key))
            list.addAll(
                entry.value.map { shippingPackage ->
                    ListItem.Package(shippingPackage)
                }
            )
            list
        }
    }

    private sealed class ListItem {
        data class Header(val title: String) : ListItem()
        data class Package(val data: ShippingPackage) : ListItem()
    }
}
