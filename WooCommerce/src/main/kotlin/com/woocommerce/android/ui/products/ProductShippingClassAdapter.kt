package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
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
    private val listener: ShippingClassAdapterListener,
    private var shippingClassSlug: String?
) : RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private const val VT_NO_SHIPPING_CLASS = 0
        private const val VT_SHIPPING_CLASS = 1
    }

    interface ShippingClassAdapterListener {
        fun onShippingClassClicked(shippingClass: ShippingClass?)
        fun onRequestLoadMore()
    }

    var shippingClassList: List<ShippingClass> = ArrayList()
        set(value) {
            if (!isSameList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val noShippingClassText: String = context.getString(R.string.product_no_shipping_class)

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
            -1
        } else {
            return getShippingClassAtPosition(position)!!.remoteShippingClassId
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) {
            VT_NO_SHIPPING_CLASS
        } else {
            VT_SHIPPING_CLASS
        }
    }

    override fun getItemCount(): Int {
        return shippingClassList.size + 1
    }

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
            listener.onRequestLoadMore()
        }
    }

    private fun isSameList(classes: List<ShippingClass>): Boolean {
        if (classes.size != shippingClassList.size) {
            return false
        }

        classes.forEach {
            if (!containsShippingClass(it)) {
                return false
            }
        }

        return true
    }

    private fun containsShippingClass(shippingClass: ShippingClass): Boolean {
        shippingClassList.forEach {
            if (it.remoteShippingClassId == shippingClass.remoteShippingClassId) {
                return true
            }
        }
        return false
    }

    private fun getShippingClassAtPosition(position: Int): ShippingClass? {
        return if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
            null
        } else {
            shippingClassList[position - 1]
        }
    }

    inner class ViewHolder(val viewBinding: ProductShippingClassItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position > -1) {
                    getShippingClassAtPosition(position)?.let {
                        shippingClassSlug = it.slug
                        listener.onShippingClassClicked(it)
                    } ?: listener.onShippingClassClicked(null)
                }
            }
        }

        fun bind(position: Int) {
            if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
                viewBinding.text.text = noShippingClassText
                viewBinding.text.isChecked = shippingClassSlug.isNullOrEmpty()
            } else {
                getShippingClassAtPosition(position)?.let {
                    viewBinding.text.text = it.name
                    viewBinding.text.isChecked = it.slug == shippingClassSlug
                }
            }
        }
    }
}
