package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductDetailBottomSheetListItemBinding
import com.woocommerce.android.ui.products.ProductDetailBottomSheetAdapter.ProductDetailBottomSheetViewHolder
import com.woocommerce.android.ui.products.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem

class ProductDetailBottomSheetAdapter(
    private val onItemClicked: (bottomSheetUiItem: ProductDetailBottomSheetUiItem) -> Unit
) : RecyclerView.Adapter<ProductDetailBottomSheetViewHolder>() {
    private val options = ArrayList<ProductDetailBottomSheetUiItem>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailBottomSheetViewHolder {
        return ProductDetailBottomSheetViewHolder(
            ProductDetailBottomSheetListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false)
        )
    }

    override fun onBindViewHolder(holder: ProductDetailBottomSheetViewHolder, position: Int) {
        holder.bind(options[position], onItemClicked)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = options.size

    fun setProductDetailBottomSheetOptions(optionList: List<ProductDetailBottomSheetUiItem>) {
        if (options.isEmpty()) {
            options.addAll(optionList)
            notifyDataSetChanged()
        } else {
            val diffResult =
                DiffUtil.calculateDiff(
                    ProductDetailBottomSheetItemDiffUtil(options, optionList)
                )
            options.clear()
            options.addAll(optionList)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    class ProductDetailBottomSheetViewHolder(private val viewBinding: ProductDetailBottomSheetListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(
            item: ProductDetailBottomSheetUiItem,
            onItemClicked: (bottomSheetUiItem: ProductDetailBottomSheetUiItem) -> Unit
        ) {
            viewBinding.productDetailInfoItemName.text = viewBinding.root.context.getString(item.type.titleResource)
            viewBinding.productDetailInfoItemDesc.text = viewBinding.root.context.getString(item.type.descResource)
            viewBinding.root.setOnClickListener {
                onItemClicked(item)
            }
        }
    }

    private class ProductDetailBottomSheetItemDiffUtil(
        val items: List<ProductDetailBottomSheetUiItem>,
        val result: List<ProductDetailBottomSheetUiItem>
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
