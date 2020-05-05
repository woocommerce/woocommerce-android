package com.woocommerce.android.ui.products

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.woocommerce.android.ui.products.models.ProductDetailItem
import com.woocommerce.android.ui.products.models.ProductDetailItem.ComplexProperty
import com.woocommerce.android.ui.products.models.ProductDetailItem.Editable
import com.woocommerce.android.ui.products.models.ProductDetailItem.Property
import com.woocommerce.android.ui.products.models.ProductDetailItem.PropertyGroup
import com.woocommerce.android.ui.products.models.ProductDetailItem.RatingBar
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.RATING_BAR
import com.woocommerce.android.ui.products.viewholders.ComplexPropertyViewHolder
import com.woocommerce.android.ui.products.viewholders.DividerViewHolder
import com.woocommerce.android.ui.products.viewholders.EditableViewHolder
import com.woocommerce.android.ui.products.viewholders.ProductDetailViewHolder
import com.woocommerce.android.ui.products.viewholders.PropertyGroupViewHolder
import com.woocommerce.android.ui.products.viewholders.PropertyViewHolder
import com.woocommerce.android.ui.products.viewholders.RatingBarViewHolder

class ProductDetailAdapter : Adapter<ProductDetailViewHolder>() {
    private var items = listOf<ProductDetailItem>()

    fun update(newItems: List<ProductDetailItem>) {
        val diffResult = DiffUtil.calculateDiff(
            ProductDetailDiffCallback(
                items,
                newItems
            )
        )
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductDetailViewHolder {
        return when (ProductDetailItem.Type.values()[viewType]) {
            DIVIDER -> DividerViewHolder(parent)
            PROPERTY -> PropertyViewHolder(parent)
            COMPLEX_PROPERTY -> ComplexPropertyViewHolder(parent)
            RATING_BAR -> RatingBarViewHolder(parent)
            PROPERTY_GROUP -> PropertyGroupViewHolder(parent)
            EDITABLE -> EditableViewHolder(parent)
        }
    }

    override fun getItemCount(): Int = items.size

    override fun getItemViewType(position: Int): Int {
        return items[position].type.ordinal
    }

    override fun onBindViewHolder(holder: ProductDetailViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is PropertyViewHolder -> holder.bind(item as Property)
            is ComplexPropertyViewHolder -> holder.bind(item as ComplexProperty)
            is EditableViewHolder -> holder.bind(item as Editable)
            is PropertyGroupViewHolder -> holder.bind(item as PropertyGroup)
            is RatingBarViewHolder -> holder.bind(item as RatingBar)
        }
    }
}
