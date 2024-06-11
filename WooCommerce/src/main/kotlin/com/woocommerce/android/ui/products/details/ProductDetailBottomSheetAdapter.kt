package com.woocommerce.android.ui.products.details

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductDetailBottomSheetListItemBinding
import com.woocommerce.android.ui.products.details.ProductDetailBottomSheetAdapter.ProductDetailBottomSheetViewHolder
import com.woocommerce.android.ui.products.details.ProductDetailBottomSheetBuilder.ProductDetailBottomSheetUiItem

class ProductDetailBottomSheetAdapter(
    private val onItemClicked: (bottomSheetUiItem: ProductDetailBottomSheetUiItem) -> Unit
) : RecyclerView.Adapter<ProductDetailBottomSheetViewHolder>() {
    var options: List<ProductDetailBottomSheetUiItem> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                ProductDetailBottomSheetItemDiffUtil(
                    field,
                    value
                )
            )
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailBottomSheetViewHolder {
        return ProductDetailBottomSheetViewHolder(
            ProductDetailBottomSheetListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductDetailBottomSheetViewHolder, position: Int) {
        holder.bind(options[position], onItemClicked)
    }

    override fun getItemCount() = options.size

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
            items[oldItemPosition].type.ordinal == result[newItemPosition].type.ordinal

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = result.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = result[newItemPosition]
            return oldItem == newItem
        }
    }
}
