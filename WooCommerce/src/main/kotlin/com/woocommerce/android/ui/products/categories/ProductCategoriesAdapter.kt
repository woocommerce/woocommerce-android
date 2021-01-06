package com.woocommerce.android.ui.products.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductCategoryListItemBinding
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.ProductCategoriesAdapter.ProductCategoryViewHolder
import org.wordpress.android.util.HtmlUtils

class ProductCategoriesAdapter(
    private val context: Context,
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

    private fun handleCategoryClick(
        productCategory: ProductCategoryItemUiModel,
        isChecked: Boolean
    ) {
        productCategory.isSelected = isChecked
        clickListener.onProductCategoryClick(productCategory)
    }

    fun setProductCategories(productsCategories: List<ProductCategoryItemUiModel>) {
        if (productCategoryList.isEmpty()) {
            productCategoryList.addAll(productsCategories)
            notifyDataSetChanged()
        } else {
            val diffResult =
                DiffUtil.calculateDiff(ProductCategoryItemDiffUtil(productCategoryList, productsCategories))
            productCategoryList.clear()
            productCategoryList.addAll(productsCategories)
            diffResult.dispatchUpdatesTo(this)
        }
    }

    inner class ProductCategoryViewHolder(private val viewBinder: ProductCategoryListItemBinding) :
        RecyclerView.ViewHolder(viewBinder.root) {
        fun bind(productCategory: ProductCategoryItemUiModel) {
            viewBinder.categoryName.apply {
                text = if (productCategory.category.name.isEmpty()) {
                    context.getString(R.string.untitled)
                } else {
                    HtmlUtils.fastStripHtml(productCategory.category.name)
                }

                val newLayoutParams = layoutParams as LayoutParams
                newLayoutParams.marginStart = productCategory.margin
                layoutParams = newLayoutParams
            }

            viewBinder.categorySelected.isChecked = productCategory.isSelected

            viewBinder.categorySelected.setOnClickListener {
                handleCategoryClick(productCategory, viewBinder.categorySelected.isChecked)
            }

            itemView.setOnClickListener {
                viewBinder.categorySelected.isChecked = !viewBinder.categorySelected.isChecked
                handleCategoryClick(productCategory, viewBinder.categorySelected.isChecked)
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
            return oldItem.category == newItem.category
        }
    }
}
