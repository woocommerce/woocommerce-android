package com.woocommerce.android.ui.products.adapters

import androidx.recyclerview.widget.DiffUtil.Callback
import com.woocommerce.android.ui.products.models.ProductProperty
import com.woocommerce.android.ui.products.models.ProductProperty.Type.COMPLEX_PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.DIVIDER
import com.woocommerce.android.ui.products.models.ProductProperty.Type.EDITABLE
import com.woocommerce.android.ui.products.models.ProductProperty.Type.LINK
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY
import com.woocommerce.android.ui.products.models.ProductProperty.Type.PROPERTY_GROUP
import com.woocommerce.android.ui.products.models.ProductProperty.Type.RATING_BAR
import com.woocommerce.android.ui.products.models.ProductProperty.Type.READ_MORE

class ProductPropertiesDiffCallback(
    private val oldList: List<ProductProperty>,
    private val newList: List<ProductProperty>
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
                EDITABLE,
                LINK,
                READ_MORE
                    -> oldItem == newItem
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
