package com.woocommerce.android.ui.products

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.ProductListItemBinding
import com.woocommerce.android.extensions.areSameProductsAs
import com.woocommerce.android.model.Product

class GroupedProductListAdapter(
    private var isEditMode: Boolean = false,
    private val productListReorderingStrategy: ProductListReorderingStrategy = DefaultProductListReorderingStrategy(),
    private val onItemDeleted: (product: Product) -> Unit,
    private val onItemReOrdered: (productsList: List<Product>) -> Unit
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
        if (isEditMode) {
            holder.setOnDeleteClickListener(product, onItemDeleted)
        } else {
            holder.viewBinding.productBtnDelete.isVisible = false
        }
        holder.setEditMode(isEditMode)
        if (productList.size > 1) {
            holder.bind(product)
        } else {
            holder.bind(product, showDragHandler = false)
        }
    }

    fun setProductList(products: List<Product>) {
        if (!productList.areSameProductsAs(products)) {
            if (products.size > 1) {
                val diffResult = DiffUtil.calculateDiff(ProductItemDiffUtil(productList, products))
                productList.clear()
                productList.addAll(products)
                diffResult.dispatchUpdatesTo(this)
            } else {
                productList.clear()
                productList.addAll(products)
                notifyDataSetChanged()
            }
        }
    }

    fun setEditMode(showEditMode: Boolean) {
        isEditMode = showEditMode
        notifyDataSetChanged()
    }

    /**
     * Helps in re-ordering the items in the list.
     **/
    fun swapItems(from: Int, to: Int) {
        productListReorderingStrategy.reOrderItems(from, to, productList)
        notifyItemMoved(from, to)
        onItemReOrdered(productList)
    }
}
