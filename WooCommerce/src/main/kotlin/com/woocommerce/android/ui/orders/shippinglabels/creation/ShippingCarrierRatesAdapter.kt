package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Parcelable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.databinding.ShippingRateListBinding
import com.woocommerce.android.databinding.ShippingRateListItemBinding
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.Option
import com.woocommerce.android.model.ShippingRate.Option.ADULT_SIGNATURE
import com.woocommerce.android.model.ShippingRate.Option.DEFAULT
import com.woocommerce.android.model.ShippingRate.Option.SIGNATURE
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateItemDiffUtil.ChangePayload
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateItemDiffUtil.ChangePayload.SELECTED_OPTION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListAdapter.RateViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.FEDEX
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.UPS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.USPS
import com.woocommerce.android.util.DateUtils
import kotlinx.android.parcel.Parcelize
import java.math.BigDecimal
import java.util.Date

class ShippingCarrierRatesAdapter(
    private val onRateSelected: (ShippingRate) -> Unit
) : RecyclerView.Adapter<RateListViewHolder>() {
    var items: List<PackageRateListItem> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(PackageListItemDiffUtil(field, value))
            field = value

            diffResult.dispatchUpdatesTo(this)
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
        init {
            binding.rateOptions.apply {
                adapter = RateListAdapter()
                layoutManager = LinearLayoutManager(context)
            }
        }
        @SuppressLint("SetTextI18n")
        fun bind(rateList: PackageRateListItem, position: Int) {
            binding.packageName.text = binding.root.resources.getString(
                R.string.shipping_label_package_details_title_template,
                position + 1
            )

            binding.packageItemsCount.text = "- ${binding.root.resources.getQuantityString(
                R.plurals.shipping_label_package_details_items_count,
                rateList.itemCount,
                rateList.itemCount
            )}"

            (binding.rateOptions.adapter as? RateListAdapter)?.updateRates(rateList)
        }
    }

    private inner class RateListAdapter : RecyclerView.Adapter<RateViewHolder>() {
        private lateinit var packageId: String
        private var items: List<ShippingRateItem> = emptyList()
            set(value) {
                val diffResult = DiffUtil.calculateDiff(RateItemDiffUtil(field, value))
                field = value

                diffResult.dispatchUpdatesTo(this)
            }

        fun updateRates(packageRateList: PackageRateListItem) {
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

        @Suppress("UNCHECKED_CAST")
        override fun onBindViewHolder(holder: RateViewHolder, position: Int, payloads: List<Any>) {
            holder.bind(items[position], payloads as List<ChangePayload>)
        }

        private inner class RateViewHolder(
            private val binding: ShippingRateListItemBinding
        ) : ViewHolder(binding.root) {
            @SuppressLint("SetTextI18n")
            fun bind(rateItem: ShippingRateItem, payloads: List<ChangePayload>) {
                if (!payloads.contains(SELECTED_OPTION)) {
                    binding.carrierServiceName.text = rateItem.title

                    if (rateItem.deliveryDate != null) {
                        val dateUtils = DateUtils()
                        binding.deliveryTime.text = dateUtils.getShortMonthDayString(
                            dateUtils.getYearMonthDayStringFromDate(rateItem.deliveryDate)
                        )
                    } else {
                        binding.deliveryTime.text = binding.root.resources.getQuantityString(
                            R.plurals.shipping_label_shipping_carrier_rates_delivery_estimate,
                            rateItem.deliveryEstimate,
                            rateItem.deliveryEstimate
                        )
                    }

                    binding.servicePrice.text = rateItem.options[DEFAULT]?.formattedPrice
                    binding.carrierImage.setImageResource(
                        when (rateItem.carrier) {
                            FEDEX -> R.drawable.fedex_logo
                            USPS -> R.drawable.usps_logo
                            UPS -> R.drawable.ups_logo
                        }
                    )

                    binding.root.setOnClickListener {
                        onRateSelected(getSelectedRate(rateItem))
                    }
                }

                val isExpanded = rateItem.selectedOption != null

                binding.carrierImage.isVisible = !isExpanded
                binding.carrierRadioButton.isVisible = isExpanded
                binding.carrierRadioButton.isChecked = isExpanded

                bindOptions(rateItem, isExpanded)
            }

            private fun getSelectedRate(rateItem: ShippingRateItem): ShippingRate {
                return when {
                    binding.signatureOption.isChecked -> rateItem[SIGNATURE]
                    binding.adultSignatureOption.isChecked -> rateItem[ADULT_SIGNATURE]
                    else -> rateItem[DEFAULT]
                }
            }

            private fun bindOptions(rateItem: ShippingRateItem, isExpanded: Boolean) {
                if (isExpanded) {
                    val options = mutableListOf<String>()
                    if (rateItem.isTrackingAvailable) {
                        options.add(
                            binding.root.resources.getString(
                                string.shipping_label_rate_included_options_tracking, rateItem.carrier.title
                            )
                        )
                    }
                    if (rateItem.isInsuranceAvailable) {
                        options.add(
                            binding.root.resources.getString(
                                string.shipping_label_rate_included_options_insurance, rateItem.insuranceCoverage
                            )
                        )
                    }
                    if (rateItem.isSignatureFree) {
                        options.add(
                            binding.root.resources.getString(
                                string.shipping_label_rate_included_options_signature_required
                            )
                        )
                    }
                    if (rateItem.isFreePickupAvailable) {
                        options.add(
                            binding.root.resources.getString(
                                string.shipping_label_rate_included_options_free_pickup
                            )
                        )
                    }

                    binding.includedOptions.text = binding.root.resources.getString(
                        string.shipping_label_rate_included_options,
                        options.joinToString()
                    )
                    binding.includedOptions.show()

                    if (rateItem.isSignatureAvailable) {
                        binding.signatureOption.text = binding.root.resources.getString(
                            string.shipping_label_rate_option_signature_required,
                            rateItem[SIGNATURE].formattedPrice
                        )
                        binding.signatureOption.isChecked = rateItem.selectedOption == SIGNATURE
                        binding.signatureOption.show()

                        binding.signatureOption.setOnClickListener {
                            if (binding.signatureOption.isChecked) {
                                binding.adultSignatureOption.isChecked = false
                            }
                            onRateSelected(getSelectedRate(rateItem))
                        }
                    } else {
                        binding.signatureOption.hide()
                    }

                    if (rateItem.isAdultSignatureAvailable) {
                        binding.adultSignatureOption.text = binding.root.resources.getString(
                            string.shipping_label_rate_option_adult_signature_required,
                            rateItem[ADULT_SIGNATURE].formattedPrice
                        )
                        binding.adultSignatureOption.isChecked = rateItem.selectedOption == ADULT_SIGNATURE
                        binding.adultSignatureOption.show()

                        binding.adultSignatureOption.setOnClickListener {
                            if (binding.adultSignatureOption.isChecked) {
                                binding.signatureOption.isChecked = false
                            }
                            onRateSelected(getSelectedRate(rateItem))
                        }
                    } else {
                        binding.adultSignatureOption.hide()
                    }
                } else {
                    binding.includedOptions.hide()
                    binding.signatureOption.hide()
                    binding.adultSignatureOption.hide()
                }
            }
        }
    }

    private class PackageListItemDiffUtil(
        val oldItems: List<PackageRateListItem>,
        val newItems: List<PackageRateListItem>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition].id == newItems[newItemPosition].id

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem == newItem
        }
    }

    private class RateItemDiffUtil(
        val oldItems: List<ShippingRateItem>,
        val newItems: List<ShippingRateItem>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
            oldItems[oldItemPosition].serviceId == newItems[newItemPosition].serviceId

        override fun getOldListSize(): Int = oldItems.size

        override fun getNewListSize(): Int = newItems.size

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldItems[oldItemPosition]
            val newItem = newItems[newItemPosition]
            return oldItem == newItem
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return if (oldItems[oldItemPosition].selectedOption != newItems[newItemPosition].selectedOption) {
                SELECTED_OPTION
            } else null
        }

        enum class ChangePayload {
            SELECTED_OPTION
        }
    }

    @Parcelize
    data class PackageRateListItem(
        val id: String,
        val itemCount: Int,
        val rateOptions: List<ShippingRateItem>
    ) : Parcelable {
        val hasSelectedOption: Boolean = rateOptions.any { it.selectedOption != null }

        fun updateSelectedRateAndCopy(selectedRate: ShippingRate): PackageRateListItem {
            return copy(rateOptions = rateOptions.map { item ->
                // update the selected rate for the specific carrier option and reset the rest, since only one
                // rate option can be selected per package
                if (item.serviceId == selectedRate.serviceId) {
                    item.copy(selectedOption = selectedRate.option)
                } else {
                    item.copy(selectedOption = null)
                }
            })
        }
    }

    @Parcelize
    data class ShippingRateItem(
        val serviceId: String,
        val title: String,
        val deliveryEstimate: Int,
        val deliveryDate: Date?,
        val carrier: ShippingCarrier,
        val isTrackingAvailable: Boolean,
        val isFreePickupAvailable: Boolean,
        val isInsuranceAvailable: Boolean,
        val insuranceCoverage: String?,
        val options: Map<Option, ShippingRate>,
        val selectedOption: Option? = null
    ) : Parcelable {
        operator fun get(option: Option): ShippingRate {
            return requireNotNull(options[option])
        }

        val isSignatureFree = options[SIGNATURE]?.price.isEqualTo(BigDecimal.ZERO)

        val isSignatureAvailable = options.keys.contains(SIGNATURE) && !isSignatureFree

        val isAdultSignatureAvailable = options.keys.contains(ADULT_SIGNATURE)

        enum class ShippingCarrier(val title: String) {
            FEDEX("Fedex"),
            USPS("USPS"),
            UPS("UPS")
        }
    }
}
