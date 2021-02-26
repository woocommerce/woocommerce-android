package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.databinding.ShippingRateListBinding
import com.woocommerce.android.databinding.ShippingRateListItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.ExtraOption
import com.woocommerce.android.model.ShippingRate.ExtraOption.ADULT_SIGNATURE
import com.woocommerce.android.model.ShippingRate.ExtraOption.NONE
import com.woocommerce.android.model.ShippingRate.ExtraOption.SIGNATURE
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.FEDEX
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.UPS
import com.woocommerce.android.model.ShippingRate.ShippingCarrier.USPS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListAdapter.RateViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesViewModel.PackageRateList
import com.woocommerce.android.util.DateUtils

class ShippingCarrierRatesAdapter(
    private val onRateSelected: (String, String, ExtraOption) -> Unit
) : RecyclerView.Adapter<RateListViewHolder>() {
    var items: List<PackageRateList> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(
                PackageListItemDiffUtil(
                    field,
                    value
                ), false)
            field = value

            diffResult.dispatchUpdatesTo(this)
        }

    init {
        setHasStableIds(true)
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
            ratesAdapter.updateRates(rateList)

            binding.rateOptions.apply {
                adapter = ratesAdapter
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator()
            }
        }
    }

    private inner class RateListAdapter : RecyclerView.Adapter<RateViewHolder>() {
        private lateinit var packageId: String
        private var items: List<ShippingRate> = emptyList()
            set(value) {
                val diffResult = DiffUtil.calculateDiff(
                    RateItemDiffUtil(
                        field,
                        value
                    ), false)
                field = value

                diffResult.dispatchUpdatesTo(this)
            }

        init {
            setHasStableIds(true)
        }

        fun updateRates(packageRateList: PackageRateList) {
            this.packageId = packageRateList.id
            this.items = packageRateList.rateOptions
        }

        override fun getItemCount(): Int = items.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RateViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            return RateViewHolder(ShippingRateListItemBinding.inflate(layoutInflater, parent, false))
        }

        override fun onBindViewHolder(holder: RateViewHolder, position: Int) {
            onBindViewHolder(holder, position, listOf())
        }

        override fun onBindViewHolder(holder: RateViewHolder, position: Int, payloads: List<Any>) {
            holder.bind(items[position])
        }

        private inner class RateViewHolder(
            private val binding: ShippingRateListItemBinding
        ) : ViewHolder(binding.root) {
            @SuppressLint("SetTextI18n")
            fun bind(rate: ShippingRate) {
                binding.carrierServiceName.text = rate.title

                if (rate.deliveryDate != null) {
                    val dateUtils = DateUtils()
                    binding.deliveryTime.text = dateUtils.getShortMonthDayString(
                        dateUtils.getYearMonthDayStringFromDate(rate.deliveryDate)
                    )
                } else {
                    binding.deliveryTime.text = binding.root.resources.getQuantityString(
                        R.plurals.shipping_label_shipping_carrier_rates_delivery_estimate,
                        rate.deliveryEstimate,
                        rate.deliveryEstimate
                    )
                }

                binding.servicePrice.text = rate.price

                binding.carrierImage.isVisible = !rate.isSelected
                binding.carrierImage.setImageResource(
                    when (rate.carrier) {
                        FEDEX -> R.drawable.fedex_logo
                        USPS -> R.drawable.usps_logo
                        UPS -> R.drawable.ups_logo
                    }
                )

                binding.carrierRadioButton.isVisible = rate.isSelected
                binding.carrierRadioButton.isChecked = rate.isSelected

                bindOptions(rate)

                binding.root.setOnClickListener {
                    onRateSelected(packageId, rate.id, getSelectedOption())
                }
            }

            private fun getSelectedOption(): ExtraOption {
                return when {
                    binding.signatureOption.isChecked -> SIGNATURE
                    binding.adultSignatureOption.isChecked -> ADULT_SIGNATURE
                    else -> NONE
                }
            }

            private fun bindOptions(rate: ShippingRate) {
                val options = mutableListOf<String>()
                if (rate.isTrackingAvailable) {
                    options.add(
                        binding.root.resources.getString(
                            string.shipping_label_rate_included_options_tracking, rate.carrier.title
                        )
                    )
                }
                if (rate.isInsuranceAvailable) {
                    options.add(
                        binding.root.resources.getString(
                            string.shipping_label_rate_included_options_insurance, rate.insuranceCoverage
                        )
                    )
                }
                if (rate.isSignatureRequired) {
                    options.add(
                        binding.root.resources.getString(
                            string.shipping_label_rate_included_options_signature_required
                        )
                    )
                }
                if (rate.isFreePickupAvailable) {
                    options.add(
                        binding.root.resources.getString(
                            string.shipping_label_rate_included_options_free_pickup
                        )
                    )
                }

                if (options.isNotEmpty() && rate.isSelected) {
                    binding.includedOptions.text = binding.root.resources.getString(
                        string.shipping_label_rate_included_options,
                        options.joinToString()
                    )
                    binding.includedOptions.show()
                } else {
                    binding.includedOptions.hide()
                }

                if (rate.isSignatureAvailable && rate.isSelected) {
                    binding.signatureOption.text = binding.root.resources.getString(
                        string.shipping_label_rate_option_signature_required,
                        rate.signaturePrice
                    )
                    binding.signatureOption.isChecked = rate.extraOptionSelected == SIGNATURE
                    binding.signatureOption.show()

                    binding.signatureOption.setOnClickListener {
                        if (binding.signatureOption.isChecked) {
                            binding.adultSignatureOption.isChecked = false
                        }
                        onRateSelected(packageId, rate.id, getSelectedOption())
                    }
                } else {
                    binding.signatureOption.hide()
                }

                if (rate.isAdultSignatureAvailable && rate.isSelected) {
                    binding.adultSignatureOption.text = binding.root.resources.getString(
                        string.shipping_label_rate_option_adult_signature_required,
                        rate.adultSignaturePrice
                    )
                    binding.adultSignatureOption.isChecked = rate.extraOptionSelected == ADULT_SIGNATURE
                    binding.adultSignatureOption.show()

                    binding.adultSignatureOption.setOnClickListener {
                        if (binding.adultSignatureOption.isChecked) {
                            binding.signatureOption.isChecked = false
                        }
                        onRateSelected(packageId, rate.id, getSelectedOption())
                    }
                } else {
                    binding.adultSignatureOption.hide()
                }
            }
        }
    }

    private class PackageListItemDiffUtil(
        val items: List<PackageRateList>,
        val newItems: List<PackageRateList>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            items[oldItemPosition].id == newItems[newItemPosition].id

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem == newItem
        }
    }

    private class RateItemDiffUtil(
        val items: List<ShippingRate>,
        val newItems: List<ShippingRate>
    ) : DiffUtil.Callback() {
        enum class RateItemPayload {
            SELECTION_CHANGED, EXTRA_OPTION_CHANGED
        }
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            items[oldItemPosition].id == newItems[newItemPosition].id

        override fun getOldListSize(): Int = items.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = items[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem == newItem
        }

//        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
//            val oldItem = items[oldItemPosition]
//            val newItem = newItems[newItemPosition]
//            return when {
//                oldItem.isSelected != newItem.isSelected -> SELECTION_CHANGED
//                oldItem.extraOptionSelected != newItem.extraOptionSelected -> EXTRA_OPTION_CHANGED
//                else -> null
//            }
//        }
    }
}
