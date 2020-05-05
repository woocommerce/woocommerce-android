package com.woocommerce.android.ui.products

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.products.models.ProductDetailItem
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.RATING_BAR

class ProductDetailDiffCallback(
    private val oldList: List<ProductDetailItem>,
    private val newList: List<ProductDetailItem>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return if (oldItem.type == newItem.type) {
            when (oldItem.type) {
                DIVIDER,
                PROPERTY,
                COMPLEX_PROPERTY,
                PROPERTY_GROUP,
                RATING_BAR,
                EDITABLE -> oldItem == newItem
            }
        } else {
            false
        }
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
