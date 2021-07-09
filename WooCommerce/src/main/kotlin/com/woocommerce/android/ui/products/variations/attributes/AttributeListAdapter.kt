package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.AttributeItemBinding
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.variations.attributes.AttributeListAdapter.AttributeViewHolder

class AttributeListAdapter(
    onItemClick: (attributeId: Long, attributeName: String) -> Unit
) : AttributeBaseAdapter<AttributeViewHolder>(onItemClick) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributeViewHolder {
        return AttributeViewHolder(
            AttributeItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    inner class AttributeViewHolder(viewBinding: AttributeItemBinding) :
        AttributeBaseViewHolder(viewBinding) {
        override fun bind(attribute: ProductAttribute) {
            viewBinding.attributeName.text = attribute.name
            if (attribute.terms.isNotEmpty()) {
                viewBinding.attributeTerms.isVisible = true
                viewBinding.attributeTerms.text = attribute.terms.joinToString()
            } else {
                viewBinding.attributeTerms.isVisible = false
            }
        }
    }
}
