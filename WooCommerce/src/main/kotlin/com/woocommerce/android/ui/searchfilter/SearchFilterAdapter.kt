package com.woocommerce.android.ui.searchfilter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.FilterListItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.ui.searchfilter.SearchFilterAdapter.SearchFilterViewHolder

class SearchFilterAdapter(
    private val onItemSelectedListener: (SearchFilterItem) -> Unit
) : ListAdapter<SearchFilterItem, SearchFilterViewHolder>(SearchFilterDiffCallback) {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchFilterViewHolder =
        SearchFilterViewHolder(
            FilterListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: SearchFilterViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).hashCode().toLong()

    inner class SearchFilterViewHolder(val viewBinding: FilterListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: SearchFilterItem) {
            viewBinding.filterItemName.text = item.name
            viewBinding.filterItemSelection.hide()
            viewBinding.root.setOnClickListener { onItemSelectedListener(item) }
        }
    }

    object SearchFilterDiffCallback : DiffUtil.ItemCallback<SearchFilterItem>() {
        override fun areItemsTheSame(
            oldSearchFilterItem: SearchFilterItem,
            newSearchFilterItem: SearchFilterItem
        ): Boolean = oldSearchFilterItem.value == newSearchFilterItem.value

        override fun areContentsTheSame(
            oldSearchFilterItem: SearchFilterItem,
            newSearchFilterItem: SearchFilterItem
        ): Boolean = oldSearchFilterItem == newSearchFilterItem
    }
}
