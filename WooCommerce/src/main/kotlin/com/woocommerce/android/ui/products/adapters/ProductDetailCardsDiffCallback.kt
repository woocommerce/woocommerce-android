package com.woocommerce.android.ui.products.adapters

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.products.models.ProductDetailCard
import com.woocommerce.android.ui.products.models.ProductDetailItem
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.LINK
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.RATING_BAR
import com.woocommerce.android.ui.products.models.ProductDetailItem.Type.READ_MORE

class ProductDetailCardsDiffCallback(
    private val oldList: List<ProductDetailCard>,
    private val newList: List<ProductDetailCard>
) : Callback() {
    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val newItem = newList[newItemPosition]
        val oldItem = oldList[oldItemPosition]
        return oldItem.caption == newItem.caption
    }

    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}
