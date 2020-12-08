package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductDetailBottomSheetListItemBinding
import com.woocommerce.android.ui.products.ProductTypesBottomSheetAdapter.ProductTypesBottomSheetViewHolder
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

class ProductTypesBottomSheetAdapter(
    private val onItemClicked: (productTypeUiItem: ProductTypesBottomSheetUiItem) -> Unit
) : RecyclerView.Adapter<ProductTypesBottomSheetViewHolder>() {
    private val options = ArrayList<ProductTypesBottomSheetUiItem>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductTypesBottomSheetViewHolder {
        return ProductTypesBottomSheetViewHolder(
            ProductDetailBottomSheetListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductTypesBottomSheetViewHolder, position: Int) {
        holder.bind(options[position], onItemClicked)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = options.size

    fun setProductTypeOptions(optionList: List<ProductTypesBottomSheetUiItem>) {
        if (options.isEmpty()) {
            options.addAll(optionList)
            notifyDataSetChanged()
        } else {
            val diffResult =
                DiffUtil.calculateDiff(ProductTypesBottomSheetItemDiffUtil(options, optionList))
            options.clear()
            options.addAll(optionList)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductTypesBottomSheetViewHolder(val viewBinder: ProductDetailBottomSheetListItemBinding) :
        RecyclerView.ViewHolder(viewBinder.root) {
        fun bind(
            item: ProductTypesBottomSheetUiItem,
            onItemClicked: (productTypeUiItem: ProductTypesBottomSheetUiItem) -> Unit
        ) {
            viewBinder.productDetailInfoItemName.text = itemView.context.getString(item.titleResource)
            viewBinder.productDetailInfoItemDesc.text = itemView.context.getString(item.descResource)
            viewBinder.productDetailInfoItemIcon.visibility = View.VISIBLE
            viewBinder.productDetailInfoItemIcon.setImageResource(item.iconResource)

            itemView.setOnClickListener {
                onItemClicked(item)
            }

            itemView.isEnabled = item.isEnabled
            viewBinder.productDetailInfoItemName.isEnabled = item.isEnabled
            viewBinder.productDetailInfoItemDesc.isEnabled = item.isEnabled
        }
    }

    private class ProductTypesBottomSheetItemDiffUtil(
        val items: List<ProductTypesBottomSheetUiItem>,
        val result: List<ProductTypesBottomSheetUiItem>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            items[oldItemPosition].type == result[newItemPosition].type

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem == newItem
        }
    }
}
