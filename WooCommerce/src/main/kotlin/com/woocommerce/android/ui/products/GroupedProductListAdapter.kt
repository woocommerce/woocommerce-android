package com.woocommerce.android.ui.products

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.isSameList

class GroupedProductListAdapter(
    private val onItemDeleted: (product: Product) -> Unit
) : RecyclerView.Adapter<ProductItemViewHolder>() {
    private val productList = ArrayList<Product>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ProductItemViewHolder(parent)

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = productList[position]

        holder.bind(product)
        holder.setOnDeleteClickListener(product, onItemDeleted)
    }

    fun setProductList(products: List<Product>) {
        if (!productList.isSameList(products)) {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
            productList.clear()
            productList.addAll(products)
            diffResult.dispatchUpdatesTo(this)
        }
    }
}
