package com.woocommerce.android.ui.products.variations.attributes.edit

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeTermSelectionListItemBinding
import com.woocommerce.android.ui.products.variations.attributes.edit.VariationAttributesAdapter.VariationAttributeSelectionViewHolder

class VariationAttributesAdapter(
    private var sourceData: MutableList<VariationAttributeSelectionGroup>
) : RecyclerView.Adapter<VariationAttributeSelectionViewHolder>() {
    override fun getItemCount() = sourceData.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VariationAttributeSelectionViewHolder(
            AttributeTermSelectionListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: VariationAttributeSelectionViewHolder, position: Int) {
        sourceData.getOrNull(position)
            ?.let { holder.bind(it) }
    }

    fun refreshSourceData(sourceData: MutableList<VariationAttributeSelectionGroup>) {
        this.sourceData = sourceData
        notifyDataSetChanged()
    }

    inner class VariationAttributeSelectionViewHolder(
        val viewBinding: AttributeTermSelectionListItemBinding
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(item: VariationAttributeSelectionGroup) =
            viewBinding.productCategoryParent.apply {
                hint = item.attributeName
                setText(item.selectedOption)
                setOnClickListener { displaySelectionDialog(item) }
            }

        private fun displaySelectionDialog(item: VariationAttributeSelectionGroup) {
            AttributeOptionSelectorDialog.newInstance(
                attributeGroup = item,
                onAttributeOptionSelected = { modifiedGroup ->
                    sourceData.indexOf(item).let { sourceData[it] = modifiedGroup }
                    notifyDataSetChanged()
                }
            )
        }
    }
}
