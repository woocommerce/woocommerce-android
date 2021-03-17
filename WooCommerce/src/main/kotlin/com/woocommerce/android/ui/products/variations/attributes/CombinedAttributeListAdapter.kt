package com.woocommerce.android.ui.products.variations.attributes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.AttributeListItemBinding
import com.woocommerce.android.model.CombinedAttributeModel
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductGlobalAttribute
import com.woocommerce.android.ui.products.variations.attributes.CombinedAttributeListAdapter.AttributeViewHolder

class CombinedAttributeListAdapter(
    private val onItemClick: (attributeId: Long, attributeName: String) -> Unit
) : RecyclerView.Adapter<AttributeViewHolder>() {
    private var attributeList = listOf<CombinedAttributeModel>()

    init {
        setHasStableIds(true)
    }

    // note that we can't rely on the attribute id for uniqueness since all local attributes have id = 0
    override fun getItemId(position: Int) = attributeList[position].name.hashCode().toLong()

    override fun getItemCount() = attributeList.size

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

    private class AttributeItemDiffUtil(
        val oldList: List<CombinedAttributeModel>,
        val newList: List<CombinedAttributeModel>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldList[oldItemPosition].id == newList[newItemPosition].id

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem == newItem
        }
    }

    fun setAttributeList(
        localAttributes: List<ProductAttribute>? = null,
        globalAttributes: List<ProductGlobalAttribute>? = null
    ) {
        val combinedList = ArrayList<CombinedAttributeModel>()

        localAttributes?.map { combinedList.add(CombinedAttributeModel.fromLocalAttribute(it)) }
        globalAttributes?.map { combinedList.add(CombinedAttributeModel.fromGlobalAttribute(it)) }
        combinedList.sortBy { it.name }

        val diffResult = DiffUtil.calculateDiff(
            AttributeItemDiffUtil(
                attributeList,
                combinedList
            )
        )

        attributeList = combinedList
        diffResult.dispatchUpdatesTo(this)
    }

    inner class AttributeViewHolder(val viewBinding: AttributeListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(attribute: CombinedAttributeModel) {
            viewBinding.attributeName.text = attribute.name
            viewBinding.attributeTerms.isVisible = false
        }
    }
}
