package com.woocommerce.android.ui.products.categories

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.OnLoadMoreListener
import com.woocommerce.android.ui.products.categories.ProductCategoriesAdapter.ProductCategoryViewHolder
import kotlinx.android.synthetic.main.product_category_list_item.view.*
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
        return ProductCategoryViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.product_category_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ProductCategoryViewHolder, position: Int) {
        val productCategory = productCategoryList[position]

        holder.apply {
            txtCategoryName.text = if (productCategory.category.name.isEmpty()) {
                context.getString(R.string.untitled)
            } else {
                HtmlUtils.fastStripHtml(productCategory.category.name)
            }

            val newLayoutParams = txtCategoryName.layoutParams as LayoutParams
            newLayoutParams.marginStart = productCategory.margin
            txtCategoryName.layoutParams = newLayoutParams

            checkBox.isChecked = productCategory.isSelected

            checkBox.setOnClickListener {
                handleCategoryClick(this, productCategory)
            }

            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
                handleCategoryClick(this, productCategory)
            }
        }

        if (position == itemCount - 1) {
            loadMoreListener.onRequestLoadMore()
        }
    }

    private fun handleCategoryClick(
        holder: ProductCategoryViewHolder,
        productCategory: ProductCategoryItemUiModel
    ) {
        productCategory.isSelected = holder.checkBox.isChecked
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

    class ProductCategoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtCategoryName: TextView = view.categoryName
        val checkBox: CheckBox = view.categorySelected
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
