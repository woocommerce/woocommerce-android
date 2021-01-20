package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ProductShippingClassItemBinding
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.ViewHolder

/**
 * RecyclerView adapter which shows a list of product shipping classes, the first of which will
 * be "No shipping class" so the user can choose to clear this value.
 */
class ProductShippingClassAdapter(
    context: Context,
    private val onItemClicked: (ShippingClass) -> Unit = { },
    private val onLoadMoreRequested: () -> Unit = { }
) : RecyclerView.Adapter<ViewHolder>() {
    private var items = mutableListOf<ShippingClass>()
    private val noShippingClass = ShippingClass(
        name = context.getString(R.string.product_no_shipping_class),
        slug = "",
        remoteShippingClassId = 0
    )
    private var selectedShippingClassId: Long = -1

    override fun getItemCount() = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ProductShippingClassItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)

        if (position > 0 && position == itemCount - 1) {
            onLoadMoreRequested()
        }
    }

    fun update(newItems: List<ShippingClass>, selectedItemId: Long = -1) {
        val diffResult = DiffUtil.calculateDiff(ShippingClassDiffCallback(items, newItems))
        items = mutableListOf(noShippingClass, *newItems.toTypedArray())
        selectedShippingClassId = selectedItemId
        diffResult.dispatchUpdatesTo(this)
    }

    inner class ViewHolder(val viewBinding: ProductShippingClassItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position > -1) {
                    onItemClicked(items[position])
                }
            }
        }

        fun bind(position: Int) {
            viewBinding.text.text = items[position].name
            viewBinding.text.isChecked = items[position].remoteShippingClassId == selectedShippingClassId
        }
    }

    inner class ShippingClassDiffCallback(
        private val oldList: List<ShippingClass>,
        private val newList: List<ShippingClass>
    ) : Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].remoteShippingClassId == newList[newItemPosition].remoteShippingClassId
        }

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val old = oldList[oldItemPosition]
            val new = newList[newItemPosition]
            return old == new
        }
    }
}
