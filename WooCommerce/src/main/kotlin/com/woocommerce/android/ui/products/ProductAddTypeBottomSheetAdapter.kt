package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductAddTypeBottomSheetAdapter.ProductAddTypesBottomSheetViewHolder
import kotlinx.android.synthetic.main.product_add_type_bottom_sheet_list_item.view.*

class ProductAddTypeBottomSheetAdapter(private val onItemClicked: (type: ProductType) -> Unit) : RecyclerView.Adapter<ProductAddTypesBottomSheetViewHolder>(){
    private var list: ArrayList<ProductTypesBottomSheetUiItem> = arrayListOf()
        set(value) {
            val postsDiff = ProductAddTypesBottomSheetItemDiffUtil(field, value)
            val result = DiffUtil.calculateDiff(postsDiff)
            result.dispatchUpdatesTo(this)
            field = value
        }

    fun setProductTypeOptions(items: List<ProductTypesBottomSheetUiItem>) = list.addAll(items)

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductAddTypesBottomSheetViewHolder {
        return ProductAddTypesBottomSheetViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.product_add_type_bottom_sheet_list_item, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ProductAddTypesBottomSheetViewHolder, position: Int) {
        holder.bind(list[position], onItemClicked)
    }

    override fun getItemId(position: Int) = position.toLong()

    override fun getItemCount() = list.size

    class ProductAddTypesBottomSheetViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        private val productTypeName: TextView = view.productAddTypeName
        private val productTypeDesc: TextView = view.productAddTypeDesc
        private val productTypeIcon: ImageView = view.productAddTypeIcon

        fun bind(
            item: ProductTypesBottomSheetUiItem,
            onItemClicked: (productTypeUiItem: ProductType) -> Unit
        ) {
            productTypeName.text = view.context.getString(item.titleResource)
            productTypeDesc.text = view.context.getString(item.descResource)
            productTypeIcon.visibility = View.VISIBLE
            productTypeIcon.setImageResource(item.iconResource)

            view.setOnClickListener {
                onItemClicked(item.type)
            }
        }
    }

    private class ProductAddTypesBottomSheetItemDiffUtil(
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
