package com.woocommerce.android.ui.orders

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.*
import com.woocommerce.android.databinding.OrderStatusListItemBinding
import com.woocommerce.android.databinding.OrderStatusListViewBinding
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
    val binding = OrderStatusListViewBinding.inflate(LayoutInflater.from(ctx), this)

    interface OrderStatusListListener {
        fun onOrderStatusSelected(orderStatus: String?)
    }

    fun init(listener: OrderStatusListListener) {
        binding.orderStatusList.apply {
            layoutManager = LinearLayoutManager(context)
            itemAnimator = DefaultItemAnimator()
            setHasFixedSize(true)
            adapter = OrderStatusListAdapter(listener)
        }
    }

    fun updateOrderStatusListView(orderStatusModelList: List<WCOrderStatusModel>) {
        (binding.orderStatusList.adapter as OrderStatusListAdapter).submitList(orderStatusModelList)
    }

    class OrderStatusListViewHolder(val viewBinding: OrderStatusListItemBinding) :
        RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(orderStatusModel: WCOrderStatusModel) {
            viewBinding.orderStatusItemName.text = orderStatusModel.label

            val count = orderStatusModel.statusCount
            val label = NumberFormat.getInstance().format(count)
            viewBinding.orderStatusItemCount.text = label
        }
    }

    class OrderStatusListAdapter(
        private val listener: OrderStatusListListener
    ) : ListAdapter<WCOrderStatusModel, OrderStatusListViewHolder>(StatusDiffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderStatusListViewHolder {
            return OrderStatusListViewHolder(
                OrderStatusListItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
            )
        }

        override fun onBindViewHolder(holder: OrderStatusListViewHolder, position: Int) {
            val orderStatusModel = getItem(position)
            holder.bind(orderStatusModel)
            holder.itemView.setOnClickListener {
                listener.onOrderStatusSelected(orderStatusModel.statusKey)
            }
        }
    }

    object StatusDiffCallback : DiffUtil.ItemCallback<WCOrderStatusModel>() {
        override fun areItemsTheSame(
            oldItem: WCOrderStatusModel,
            newItem: WCOrderStatusModel
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: WCOrderStatusModel,
            newItem: WCOrderStatusModel
        ): Boolean {
            return oldItem == newItem
        }
    }
}
