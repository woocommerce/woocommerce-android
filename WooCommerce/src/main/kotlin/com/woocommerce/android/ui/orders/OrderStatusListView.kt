package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import kotlinx.android.synthetic.main.order_status_list_item.view.*
import kotlinx.android.synthetic.main.order_status_list_view.view.*
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import java.text.NumberFormat

/**
 * Custom class that  displays a list of order statuses and allows for
 * selecting a single order status for filtering
 */
class OrderStatusListView @JvmOverloads constructor(
    ctx: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(ctx, attrs, defStyleAttr) {
    init {
        View.inflate(context, R.layout.order_status_list_view, this)
    }

    interface OrderStatusListListener {
        fun onOrderStatusSelected(orderStatus: String?)
    }

    fun init(listener: OrderStatusListListener) {
        orderStatusList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = OrderStatusListAdapter(listener)
        }
    }

    fun updateOrderStatusListView(orderStatusModelList: List<WCOrderStatusModel>) {
        (orderStatusList.adapter as OrderStatusListAdapter).setOrderStatusList(orderStatusModelList)
    }

    class OrderStatusListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        var orderStatusNameText: TextView = view.orderStatusItem_name
        var orderStatusCountText: TextView = view.orderStatusItem_count
    }

    class OrderStatusListAdapter(private val listener: OrderStatusListListener)
        : RecyclerView.Adapter<OrderStatusListViewHolder>() {
        private val orderStatusList: ArrayList<WCOrderStatusModel> = ArrayList()

        fun setOrderStatusList(newList: List<WCOrderStatusModel>) {
            orderStatusList.clear()
            orderStatusList.addAll(newList)
            notifyDataSetChanged()
        }

        override fun getItemCount() = orderStatusList.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderStatusListViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.order_status_list_item, parent, false)
            return OrderStatusListViewHolder(view)
        }

        override fun onBindViewHolder(holder: OrderStatusListViewHolder, position: Int) {
            val orderStatusModel = orderStatusList[position]

            holder.orderStatusNameText.text = orderStatusModel.label

            val count = orderStatusModel.statusCount
            val label = NumberFormat.getInstance().format(count)
            holder.orderStatusCountText.text = label

            holder.itemView.setOnClickListener {
                listener.onOrderStatusSelected(orderStatusModel.statusKey)
            }
        }
    }
}
