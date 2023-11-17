package com.woocommerce.android.ui.orders.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.databinding.OrderCreationCustomAmountItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.orders.creation.OrderCreateEditCustomAmountAdapter.CustomAmountViewHolder
import com.woocommerce.android.util.CurrencyFormatter

class OrderCreateEditCustomAmountAdapter(
    private val currencyFormatter: CurrencyFormatter,
    private val onCustomAmountClick: (CustomAmountUIModel) -> Unit,
    private val onCustomAmountDeleteClick: (CustomAmountUIModel) -> Unit
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

    inner class CustomAmountViewHolder(private val binding: OrderCreationCustomAmountItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val safePosition: Int?
            get() = bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }

        init {
            binding.root.setOnClickListener {
                safePosition?.let {
                    val customAmountUIModel = getItem(it)
                    if (!customAmountUIModel.isLocked) {
                        onCustomAmountClick(customAmountUIModel)
                    }
                }
            }
        }

        fun bind(customAmountUIModel: CustomAmountUIModel) {
            binding.customAmountLayout.customAmountName.text = customAmountUIModel.name
            binding.customAmountLayout.customAmountAmount.text = currencyFormatter.formatCurrency(
                customAmountUIModel.amount.toString()
            )
            if (safePosition == 0 || currentList.size <= 1) {
                binding.customAmountLayout.divider.hide()
            } else {
                binding.customAmountLayout.divider.show()
            }
            if (customAmountUIModel.isLocked) {
                binding.customAmountLayout.customAmountEdit.hide()
                binding.customAmountLayout.customAmountDeleteBtn.hide()
            }
            binding.customAmountLayout.customAmountDeleteBtn.setOnClickListener {
                safePosition?.let {
                    onCustomAmountDeleteClick(getItem(it))
                }
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
            (oldItem.isLocked == newItem.isLocked)

        override fun areContentsTheSame(
            oldItem: CustomAmountUIModel,
            newItem: CustomAmountUIModel
        ): Boolean = (oldItem.id == newItem.id) &&
            (oldItem.name == newItem.name) &&
            (oldItem.amount == newItem.amount) &&
            (oldItem.isLocked == newItem.isLocked)
    }
}
