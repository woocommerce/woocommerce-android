package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.extensions.areSameProductsAs
import com.woocommerce.android.model.Product

class GroupedProductListAdapter(
    private val onItemDeleted: (product: Product) -> Unit
) : RecyclerView.Adapter<ProductItemViewHolder>() {
    private val productList = ArrayList<Product>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productList[position].remoteId

    override fun getItemCount() = productList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductItemViewHolder {
        return ProductItemViewHolder(
            ProductListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductItemViewHolder, position: Int) {
        val product = productList[position]

        holder.bind(product)
        holder.setOnDeleteClickListener(product, onItemDeleted)
    }

    fun setProductList(products: List<Product>) {
        if (!productList.areSameProductsAs(products)) {
            val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
            productList.clear()
            productList.addAll(products)
            diffResult.dispatchUpdatesTo(this)
        }
    }
}
