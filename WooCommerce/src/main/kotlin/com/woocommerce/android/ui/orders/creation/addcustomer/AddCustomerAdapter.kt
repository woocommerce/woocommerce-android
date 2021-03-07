package com.woocommerce.android.ui.orders.creation.addcustomer

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.databinding.CustomerListItemBinding
import com.woocommerce.android.databinding.CustomerListLoadingItemBinding
import javax.inject.Inject

const val ITEM_TYPE = 0
const val LOADING_TYPE = 1

class AddCustomerAdapter @Inject constructor(
    private val viewModel: AddCustomerViewModel,
    private val layoutInflater: LayoutInflater
) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return when (viewType) {
            ITEM_TYPE -> CustomerViewHolder(
                CustomerListItemBinding.inflate(layoutInflater, parent, false)
            )
            LOADING_TYPE -> LoadingViewHolder(
                CustomerListLoadingItemBinding.inflate(layoutInflater, parent, false)
            )
            else -> throw IllegalStateException("$viewType is not supported by the adapter")
        }
    }

    override fun getItemViewType(position: Int): Int = viewModel.getItemViewType(position)

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        if (holder is CustomerItemView) viewModel.bindView(holder as CustomerItemView)
    }

    override fun getItemCount(): Int = viewModel.getItemCount()
}

interface CustomerItemView {
    fun setName(name: String?)
    fun setEmail(email: String?)
    fun setOnClickListener(onClick: () -> Unit)
}

class CustomerViewHolder(
    private val binding: CustomerListItemBinding
) : RecyclerView.ViewHolder(binding.root), CustomerItemView {
    override fun setName(name: String?) {
        binding.tvCustomerName.text = name
    }

    override fun setEmail(email: String?) {
        binding.tvCustomerEmail.text = email
    }

    override fun setOnClickListener(onClick: () -> Unit) {
        binding.root.setOnClickListener { onClick() }
    }
}

class LoadingViewHolder(binding: CustomerListLoadingItemBinding) : RecyclerView.ViewHolder(binding.root)

