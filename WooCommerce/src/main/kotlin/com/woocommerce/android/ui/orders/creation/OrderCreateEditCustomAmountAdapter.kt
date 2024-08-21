package com.woocommerce.android.ui.orders.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderCreationCustomAmountItemBinding
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditCustomAmountAdapter.CustomAmountViewHolder
import com.woocommerce.android.util.CurrencyFormatter

class OrderCreateEditCustomAmountAdapter(
    private val currencyFormatter: CurrencyFormatter,
    private val onCustomAmountClick: (CustomAmountUIModel) -> Unit,
) : ListAdapter<CustomAmountUIModel, CustomAmountViewHolder>(CustomAmountUIModelDiffCallback) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CustomAmountViewHolder {
        return CustomAmountViewHolder(
            OrderCreationCustomAmountItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: CustomAmountViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    var isLocked: Boolean = false
        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    inner class CustomAmountViewHolder(private val binding: OrderCreationCustomAmountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(customAmountUIModel: CustomAmountUIModel) {
            binding.customAmountLayout.customAmountName.text = customAmountUIModel.name
            binding.customAmountLayout.customAmountAmount.text = currencyFormatter.formatCurrency(
                customAmountUIModel.amount.toString()
            )
            binding.customAmountLayout.customAmountEdit.isVisible = isLocked.not()
            if (isLocked.not()) {
                binding.root.setOnClickListener {
                    onCustomAmountClick(customAmountUIModel)
                }
                binding.root.isClickable = true
            } else if (binding.root.hasOnClickListeners()) {
                binding.root.setOnClickListener(null)
                binding.root.isClickable = false
            }
        }
    }

    object CustomAmountUIModelDiffCallback : DiffUtil.ItemCallback<CustomAmountUIModel>() {
        override fun areItemsTheSame(
            oldItem: CustomAmountUIModel,
            newItem: CustomAmountUIModel
        ): Boolean = (oldItem.id == newItem.id) &&
            (oldItem.name == newItem.name) &&
            (oldItem.amount == newItem.amount) &&
            (oldItem.taxStatus.isTaxable == newItem.taxStatus.isTaxable)

        override fun areContentsTheSame(
            oldItem: CustomAmountUIModel,
            newItem: CustomAmountUIModel
        ): Boolean = (oldItem.id == newItem.id) &&
            (oldItem.name == newItem.name) &&
            (oldItem.amount == newItem.amount) &&
            (oldItem.taxStatus.isTaxable == newItem.taxStatus.isTaxable)
    }
}
