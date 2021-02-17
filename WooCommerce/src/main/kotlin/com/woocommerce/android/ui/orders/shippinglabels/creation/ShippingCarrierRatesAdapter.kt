package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingRateListBinding
import com.woocommerce.android.databinding.ShippingRateListItemBinding
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.FEDEX
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.UPS
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.USPS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListAdapter.RateViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesViewModel.PackageRateList

class ShippingCarrierRatesAdapter(
    internal val onRateSelected: (ShippingRate) -> Unit
) : RecyclerView.Adapter<RateListViewHolder>() {
    var items: List<PackageRateList> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateListViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return RateListViewHolder(ShippingRateListBinding.inflate(layoutInflater, parent, false))
    }

    override fun onBindViewHolder(holder: RateListViewHolder, position: Int) {
        holder.bind(items[position], position)
    }

    inner class RateListViewHolder(private val binding: ShippingRateListBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(rateList: PackageRateList, position: Int) {
            binding.packageName.text = binding.root.resources.getString(
                R.string.shipping_label_package_details_title_template,
                position + 1
            )

            binding.packageItemsCount.text = "- ${binding.root.resources.getQuantityString(
                R.plurals.shipping_label_package_details_items_count,
                rateList.itemCount,
                rateList.itemCount
            )}"

            val ratesAdapter = binding.rateOptions.adapter as? RateListAdapter ?: RateListAdapter()
            ratesAdapter.updateRates(rateList.rateOptions, rateList.selectedRate)

            binding.rateOptions.apply {
                adapter = ratesAdapter
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
            }
        }
    }

    private inner class RateListAdapter : RecyclerView.Adapter<RateViewHolder>() {
        private var selectedRate: ShippingRate? = null
        private var items: List<ShippingRate> = emptyList()
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        fun updateRates(rates: List<ShippingRate>, selectedRate: ShippingRate?) {
            this.items = rates
            this.selectedRate = selectedRate
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return RateViewHolder(ShippingRateListItemBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RateViewHolder, position: Int) {
            holder.bind(items[position], items[position] == selectedRate)
        }

        private inner class RateViewHolder(
            private val binding: ShippingRateListItemBinding
        ) : ViewHolder(binding.root) {
            @SuppressLint("SetTextI18n")
            fun bind(rate: ShippingRate, isSelected: Boolean) {
                binding.carrierServiceName.text = rate.title
                binding.deliveryTime.text = binding.root.resources.getQuantityString(
                    R.plurals.shipping_label_shipping_carrier_rates_delivery_estimate,
                    rate.deliveryEstimate,
                    rate.deliveryEstimate
                )
                binding.servicePrice.text = "\$${rate.price}"

                binding.carrierImage.isVisible = !isSelected
                binding.carrierImage.setImageResource(
                    when (rate.carrier) {
                        FEDEX -> R.drawable.fedex_logo
                        USPS -> R.drawable.usps_logo
                        UPS -> R.drawable.ups_logo
                    }
                )

                binding.carrierRadioButton.isVisible = isSelected
                binding.carrierRadioButton.isChecked = isSelected

                binding.root.setOnClickListener {
                    onRateSelected(rate)
                }
            }
        }
    }
}
