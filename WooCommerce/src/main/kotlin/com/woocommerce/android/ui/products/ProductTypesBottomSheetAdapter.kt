package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductTypesBottomSheetAdapter.ProductTypesBottomSheetViewHolder
import com.woocommerce.android.ui.products.ProductTypesBottomSheetViewModel.ProductTypesBottomSheetUiItem
import kotlinx.android.synthetic.main.product_detail_bottom_sheet_list_item.view.*

class ProductTypesBottomSheetAdapter(
    private val onItemClicked: (productTypeUiItem: ProductTypesBottomSheetUiItem) -> Unit
) : RecyclerView.Adapter<ProductTypesBottomSheetViewHolder>() {
    private val options = ArrayList<ProductTypesBottomSheetUiItem>()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductTypesBottomSheetViewHolder {
        return ProductTypesBottomSheetViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.product_detail_bottom_sheet_list_item, parent, false))
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

    class ProductTypesBottomSheetViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val productTypeName: TextView = view.productDetailInfoItem_name
        private val productTypeDesc: TextView = view.productDetailInfoItem_desc
        private val productTypeIcon: ImageView = view.productDetailInfoItem_icon

        fun bind(
            item: ProductTypesBottomSheetUiItem,
            onItemClicked: (productTypeUiItem: ProductTypesBottomSheetUiItem) -> Unit
        ) {
            productTypeName.text = view.context.getString(item.titleResource)
            productTypeDesc.text = view.context.getString(item.descResource)
            productTypeIcon.visibility = View.VISIBLE
            productTypeIcon.setImageResource(item.iconResource)

            view.setOnClickListener {
                onItemClicked(item)
            }

            view.isEnabled = item.isEnabled
            productTypeName.isEnabled = item.isEnabled
            productTypeDesc.isEnabled = item.isEnabled
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
