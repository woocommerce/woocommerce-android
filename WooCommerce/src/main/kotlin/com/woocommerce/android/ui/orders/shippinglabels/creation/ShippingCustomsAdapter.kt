package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.databinding.ShippingCustomsLineListItemBinding
import com.woocommerce.android.databinding.ShippingCustomsListItemBinding
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsAdapter.PackageCustomsViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsLineAdapter.CustomsLineViewHolder

class ShippingCustomsAdapter : RecyclerView.Adapter<PackageCustomsViewHolder>() {
    var customsPackages: List<CustomsPackage> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageCustomsViewHolder {
        val binding = ShippingCustomsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageCustomsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageCustomsViewHolder, position: Int) {
        holder.bind(customsPackages[position])
    }

    override fun getItemCount(): Int = customsPackages.size

    class PackageCustomsViewHolder(val binding: ShippingCustomsListItemBinding) : ViewHolder(binding.root) {
        private val linesAdapter: ShippingCustomsLineAdapter by lazy { ShippingCustomsLineAdapter() }

        init {
            binding.itemsList.apply {
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = linesAdapter
            }
        }

        fun bind(customsPackage: CustomsPackage) {
            binding.contentsTypeSpinner.setText(customsPackage.contentsType.title)
            binding.restrictionTypeSpinner.setText(customsPackage.restrictionType.title)
            binding.itnEditText.setText(customsPackage.itn)
            linesAdapter.customsLines = customsPackage.lines
        }
    }
}

class ShippingCustomsLineAdapter : RecyclerView.Adapter<CustomsLineViewHolder>() {
    var customsLines: List<CustomsLine> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomsLineViewHolder {
        val binding = ShippingCustomsLineListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomsLineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomsLineViewHolder, position: Int) {
        holder.bind(customsLines[position])
    }

    override fun getItemCount(): Int = customsLines.size

    class CustomsLineViewHolder(val binding: ShippingCustomsLineListItemBinding) : ViewHolder(binding.root) {
        fun bind(customsPackage: CustomsLine) {
            binding.itemDescriptionEditText.setText(customsPackage.itemDescription)
            binding.hsTariffNumberEditText.setText(customsPackage.hsTariffNumber)
            binding.weightEditText.setText(customsPackage.weight.toString())
            binding.valueEditText.setText(customsPackage.value.toPlainString())
            binding.countrySpinner.setText(customsPackage.originCountry)
        }
    }
}
