package com.woocommerce.android.ui.searchfilter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.searchfilter.SearchFilterAdapter.SearchFilterViewHolder

class SearchFilterAdapter(
    private val items: List<SearchFilterItem>,
    private val onItemSelectedListener: (SearchFilterItem) -> Unit
) : RecyclerView.Adapter<SearchFilterViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFilterViewHolder =
        SearchFilterViewHolder(
            FilterListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: SearchFilterViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class SearchFilterViewHolder(val viewBinding: FilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {

        fun bind(item: SearchFilterItem) {
            viewBinding.filterItemName.text = item.name
            viewBinding.filterItemSelection.hide()
            viewBinding.root.setOnClickListener { onItemSelectedListener(item) }
        }
    }
}
