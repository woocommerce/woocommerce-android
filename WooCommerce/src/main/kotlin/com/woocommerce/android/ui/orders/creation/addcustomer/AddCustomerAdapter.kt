package com.woocommerce.android.ui.orders.creation.addcustomer

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.databinding.CustomerListItemBinding
import com.woocommerce.android.databinding.SkeletonCustomerListItemBinding
import com.woocommerce.android.ui.orders.creation.addcustomer.CustomerListItemType.CustomerItem
import com.woocommerce.android.ui.orders.creation.addcustomer.CustomerListItemType.LoadingItem
import javax.inject.Inject

private const val ITEM_TYPE = 0
private const val LOADING_TYPE = 1

class AddCustomerAdapter @Inject constructor(
    private val layoutInflater: LayoutInflater
) : PagedListAdapter<CustomerListItemType, ViewHolder>(customerListDiffItemCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_TYPE -> CustomerViewHolder(
                CustomerListItemBinding.inflate(layoutInflater, parent, false)
            )
            LOADING_TYPE -> LoadingViewHolder(
                SkeletonCustomerListItemBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw IllegalStateException("$viewType is not supported by the adapter")
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is CustomerItem -> ITEM_TYPE
            else -> LOADING_TYPE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(position)
        if (holder is CustomerViewHolder) holder.onBind(item as CustomerItem)
    }
}

class CustomerViewHolder(
    private val binding: CustomerListItemBinding
) : RecyclerView.ViewHolder(binding.root) {
    @SuppressLint("SetTextI18n")
    fun onBind(item: CustomerItem) {
        with(item) {
            binding.tvCustomerName.text = "$firstName $lastName"
            binding.tvCustomerEmail.text = email
        }
    }
}

class LoadingViewHolder(binding: SkeletonCustomerListItemBinding) : RecyclerView.ViewHolder(binding.root)

private val customerListDiffItemCallback = object : DiffUtil.ItemCallback<CustomerListItemType>() {
    override fun areItemsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) {
            return oldItem.remoteCustomerId == newItem.remoteCustomerId
        }
        return if (oldItem is CustomerItem && newItem is CustomerItem) {
            oldItem.remoteCustomerId == newItem.remoteCustomerId
        } else {
            false
        }
    }

    override fun areContentsTheSame(oldItem: CustomerListItemType, newItem: CustomerListItemType): Boolean {
        if (oldItem is LoadingItem && newItem is LoadingItem) return true
        return if (oldItem is CustomerItem && newItem is CustomerItem) {
            oldItem.firstName == newItem.firstName &&
                oldItem.lastName == newItem.lastName &&
                oldItem.email == newItem.email
        } else {
            false
        }
    }
}

sealed class CustomerListItemType {
    data class LoadingItem(val remoteCustomerId: Long) : CustomerListItemType()
    data class CustomerItem(
        val remoteCustomerId: Long,
        val firstName: String?,
        val lastName: String?,
        val email: String
    ) : CustomerListItemType()
}
