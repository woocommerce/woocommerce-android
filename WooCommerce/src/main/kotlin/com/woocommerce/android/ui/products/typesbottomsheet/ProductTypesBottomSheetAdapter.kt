package com.woocommerce.android.ui.products.typesbottomsheet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductDetailBottomSheetListItemBinding
import com.woocommerce.android.ui.products.typesbottomsheet.ProductTypesBottomSheetAdapter.ProductTypesBottomSheetViewHolder
import com.woocommerce.android.ui.products.typesbottomsheet.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem

class ProductTypesBottomSheetAdapter(
    private val options: List<ProductTypesBottomSheetUiItem>,
    private val onItemClicked: (productTypeUiItem: ProductTypesBottomSheetUiItem) -> Unit
) : RecyclerView.Adapter<ProductTypesBottomSheetViewHolder>() {

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

    override fun getItemCount() = options.size

    class ProductTypesBottomSheetViewHolder(
        private val viewBinder: ProductDetailBottomSheetListItemBinding
    ) : RecyclerView.ViewHolder(viewBinder.root) {
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
        }
    }
}
