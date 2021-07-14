package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.model.ShippingPackage

class ShippingLabelCreateServicePackageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var items: List<ListItem> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        TODO("Not yet implemented")
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        TODO("Not yet implemented")
    }

    override fun getItemCount(): Int {
        TODO("Not yet implemented")
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
