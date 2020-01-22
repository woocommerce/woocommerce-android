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

    init {
        inflater = LayoutInflater.from(context)
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return shippingClassList[position].remoteShippingClassId
    }

    override fun getItemCount(): Int {
        return shippingClassList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(inflater.inflate(R.layout.product_shipping_class_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val shippingClass = shippingClassList[position]
        holder.text.text = shippingClass.name

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
            itemView.setOnClickListener{
                val position = adapterPosition
                if (position > -1) {
                    listener.onShippingClassClicked(shippingClassList[position])
                }
            }
        }
    }
}
