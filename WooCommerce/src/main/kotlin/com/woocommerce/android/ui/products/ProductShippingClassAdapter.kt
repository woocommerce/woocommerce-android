package com.woocommerce.android.ui.products

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.ui.products.ProductShippingClassAdapter.ViewHolder
import com.woocommerce.android.ui.products.ProductShippingClassDialog.ShippingClassDialogListener
import kotlinx.android.synthetic.main.product_shipping_class_item.view.*
import org.wordpress.android.fluxc.model.WCProductShippingClassModel

class ProductShippingClassAdapter(context: Context, private val listener: ShippingClassDialogListener) :
        RecyclerView.Adapter<ViewHolder>() {
    companion object {
        private const val VT_NO_SHIPPING_CLASS = 0
        private const val VT_SHIPPING_CLASS = 1
    }

    var shippingClassList: List<WCProductShippingClassModel> = ArrayList()
        set(value) {
            if (!isSameList(value)) {
                field = value
                notifyDataSetChanged()
            }
        }
    var selectedClassId: Long = 0
        set(value) {
            if (field != value) {
                field = value
                notifyDataSetChanged()
            }
        }

    private val inflater: LayoutInflater
    private val noShippingClassText: String

    init {
        inflater = LayoutInflater.from(context)
        noShippingClassText = context.getString(R.string.product_no_shipping_class)
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
            -1
        } else {
            return shippingClassList[position - 1].remoteShippingClassId
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
        return ViewHolder(inflater.inflate(R.layout.product_shipping_class_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
            holder.text.text = noShippingClassText
        } else {
            val shippingClass = shippingClassList[position - 1]
            holder.text.text = shippingClass.name
        }

        if (position == itemCount - 1) {
            listener.onRequestShippingClasses(loadMore = true)
        }
    }

    private fun isSameList(classes: List<WCProductShippingClassModel>): Boolean {
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

    private fun containsShippingClass(shippingClass: WCProductShippingClassModel): Boolean {
        shippingClassList.forEach {
            if (it.remoteShippingClassId == shippingClass.remoteShippingClassId) {
                return true
            }
        }
        return false
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.text

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position > -1) {
                    if (getItemViewType(position) == VT_NO_SHIPPING_CLASS) {
                        listener.onShippingClassClicked(null)
                    } else {
                        listener.onShippingClassClicked(shippingClassList[position - 1])
                    }
                }
            }
        }
    }
}
