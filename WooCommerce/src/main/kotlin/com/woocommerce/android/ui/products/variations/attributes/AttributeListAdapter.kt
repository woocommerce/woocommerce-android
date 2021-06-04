package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.ui.products.variations.attributes.AttributeListAdapter.AttributeViewHolder

class AttributeListAdapter(
    private inline val onItemClick: (attributeId: Long, attributeName: String) -> Unit
) : AttributeBaseAdapter<AttributeViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttributeViewHolder {
        return AttributeViewHolder(
            AttributeListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: AttributeViewHolder, position: Int) {
        holder.bind(attributeList[position])

        holder.itemView.setOnClickListener {
            val item = attributeList[position]
            onItemClick(item.id, item.name)
        }
    }

    inner class AttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: ProductAttribute) {
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
