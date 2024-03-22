package com.woocommerce.android.ui.products.categories

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductCategoryListItemBinding
import com.woocommerce.android.extensions.setHtmlText
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.ProductCategoriesAdapter.ProductCategoryViewHolder

class ProductCategoriesAdapter(
    private val loadMoreListener: OnLoadMoreListener,
    private val clickListener: OnProductCategoryClickListener
) : RecyclerView.Adapter<ProductCategoryViewHolder>() {
    private val productCategoryList = ArrayList<ProductCategoryItemUiModel>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int) = productCategoryList[position].category.remoteCategoryId

    override fun getItemCount() = productCategoryList.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductCategoryViewHolder {
        return ProductCategoryViewHolder(
            ProductCategoryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ProductCategoryViewHolder, position: Int) {
        val productCategory = productCategoryList[position]
        holder.bind(productCategory)

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    fun setProductCategories(productsCategories: List<ProductCategoryItemUiModel>) {
        val diffResult =
            DiffUtil.calculateDiff(ProductCategoryItemDiffUtil(productCategoryList, productsCategories))
        productCategoryList.clear()
        productCategoryList.addAll(productsCategories)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ProductCategoryViewHolder(private val viewBinder: ProductCategoryListItemBinding) :
        RecyclerView.ViewHolder(viewBinder.root) {
        fun bind(productCategory: ProductCategoryItemUiModel) {
            viewBinder.categoryName.apply {
                setHtmlText(
                    if (productCategory.category.name.isEmpty()) {
                        context.getString(R.string.untitled)
                    } else {
                        productCategory.category.name
                    }
                )

                val newLayoutParams = layoutParams as LayoutParams
                newLayoutParams.marginStart = productCategory.margin
                layoutParams = newLayoutParams
            }

            viewBinder.categoryCheckbox.isChecked = productCategory.isSelected

            viewBinder.categoryCheckbox.setOnClickListener {
                productCategory.isSelected = !productCategory.isSelected
                clickListener.onProductCategoryChecked(productCategory)
            }

            itemView.setOnClickListener {
                clickListener.onProductCategorySelected(productCategory)
            }
        }
    }

    private class ProductCategoryItemDiffUtil(
        val oldList: List<ProductCategoryItemUiModel>,
        val newList: List<ProductCategoryItemUiModel>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldList[oldItemPosition].category.remoteCategoryId == newList[newItemPosition].category.remoteCategoryId

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.category == newItem.category && oldItem.margin == newItem.margin
        }
    }
}
