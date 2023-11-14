package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.os.Parcelable
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
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.extensions.show
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.model.ShippingRate.Option
import com.woocommerce.android.model.ShippingRate.Option.ADULT_SIGNATURE
import com.woocommerce.android.model.ShippingRate.Option.DEFAULT
import com.woocommerce.android.model.ShippingRate.Option.SIGNATURE
import com.woocommerce.android.model.getTitle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListAdapter.RateViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.RateListViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.DHL
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.FEDEX
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.UNKNOWN
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.UPS
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCarrierRatesAdapter.ShippingRateItem.ShippingCarrier.USPS
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.util.StringUtils
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date

class ShippingCarrierRatesAdapter(
    private val onRateSelected: (ShippingRate) -> Unit,
    private val dateUtils: DateUtils
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
        holder.bind(items[position])
    }

    @Suppress("MagicNumber")
    inner class RateListViewHolder(private val binding: ShippingRateListBinding) : ViewHolder(binding.root) {
        private val isExpanded
            get() = binding.expandIcon.rotation == 180f

        init {
            binding.rateOptions.apply {
                adapter = RateListAdapter()
                layoutManager = LinearLayoutManager(context)
                itemAnimator = DefaultItemAnimator().apply {
                    // Disable change animations to avoid flashing items when data changes
                    supportsChangeAnimations = false
                }
            }

            // expand items by default
            binding.expandIcon.rotation = 180f
            binding.rateOptions.isVisible = true
            binding.titleLayout.setOnClickListener {
                if (isExpanded) {
                    binding.expandIcon.animate().rotation(0f).start()
                    binding.rateOptions.collapse()
                } else {
                    binding.expandIcon.animate().rotation(180f).start()
                    binding.rateOptions.expand()
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(rateList: PackageRateListItem) {
            binding.packageName.text = rateList.shippingPackage.getTitle(binding.root.context)

            binding.packageItemsCount.text = "- ${StringUtils.getQuantityString(
                context = binding.packageItemsCount.context,
                quantity = rateList.shippingPackage.itemsCount,
                default = string.shipping_label_package_details_items_count_many,
                one = string.shipping_label_package_details_items_count_one,
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
            holder.bind(items[position])
        }

        private inner class RateViewHolder(
            private val binding: ShippingRateListItemBinding
        ) : ViewHolder(binding.root) {
            @SuppressLint("SetTextI18n")
            fun bind(rateItem: ShippingRateItem) {
                binding.carrierServiceName.text = rateItem.title

                when {
                    rateItem.deliveryDate != null -> {
                        binding.deliveryTime.isVisible = true
                        binding.deliveryTime.text = dateUtils.getShortMonthDayString(
                            dateUtils.getYearMonthDayStringFromDate(rateItem.deliveryDate)
                        )
                    }
                    rateItem.deliveryEstimate != 0 -> {
                        binding.deliveryTime.isVisible = true
                        binding.deliveryTime.text = StringUtils.getQuantityString(
                            context = binding.deliveryTime.context,
                            quantity = rateItem.deliveryEstimate,
                            default = string.shipping_label_shipping_carrier_rates_delivery_estimate_many,
                            one = string.shipping_label_shipping_carrier_rates_delivery_estimate_one
                        )
                    }
                    else -> {
                        binding.deliveryTime.isVisible = false
                    }
                }

                binding.servicePrice.text = rateItem.options[rateItem.selectedOption ?: DEFAULT]?.formattedPrice
                binding.carrierImage.setImageResource(
                    when (rateItem.carrier) {
                        FEDEX -> R.drawable.fedex_logo
                        USPS -> R.drawable.usps_logo
                        UPS -> R.drawable.ups_logo
                        DHL -> R.drawable.dhl_logo
                        UNKNOWN -> 0
                    }
                )

                binding.root.setOnClickListener {
                    onRateSelected(getSelectedRate(rateItem))
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
                                if (rateItem.carrier == USPS) string.shipping_label_rate_included_options_usps_tracking
                                else string.shipping_label_rate_included_options_tracking
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
                                string.shipping_label_rate_included_options_signature_required_free
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
                            rateItem[SIGNATURE].formattedFee
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
                            rateItem[ADULT_SIGNATURE].formattedFee
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
    }

    @Parcelize
    data class PackageRateListItem(
        val id: String,
        val shippingPackage: ShippingLabelPackage,
        val rateOptions: List<ShippingRateItem>
    ) : Parcelable {
        @IgnoredOnParcel
        val selectedRate: ShippingRate?
            get() {
                return rateOptions.mapNotNull { rate ->
                    rate.selectedOption?.let { option ->
                        rate[option]
                    }
                }.firstOrNull()
            }

        @IgnoredOnParcel
        val hasSelectedOption: Boolean = rateOptions.any { it.selectedOption != null }

        fun updateSelectedRateAndCopy(selectedRate: ShippingRate): PackageRateListItem {
            return copy(
                rateOptions = rateOptions.map { item ->
                    // update the selected rate for the specific carrier option and reset the rest, since only one
                    // rate option can be selected per package
                    if (item.serviceId == selectedRate.serviceId) {
                        item.copy(selectedOption = selectedRate.option)
                    } else {
                        item.copy(selectedOption = null)
                    }
                }
            )
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
        companion object {
            const val USPS_EXPRESS_SERVICE_ID = "Express"
        }
        operator fun get(option: Option): ShippingRate {
            return requireNotNull(options[option])
        }

        @IgnoredOnParcel
        val isSignatureFree = options[SIGNATURE]?.price.isEqualTo(options[DEFAULT]?.price)

        @IgnoredOnParcel
        val isSignatureAvailable = options.keys.contains(SIGNATURE) &&
            (!isSignatureFree || serviceId == USPS_EXPRESS_SERVICE_ID && carrier == USPS)

        @IgnoredOnParcel
        val isAdultSignatureAvailable = options.keys.contains(ADULT_SIGNATURE)

        enum class ShippingCarrier {
            FEDEX,
            USPS,
            UPS,
            DHL,
            UNKNOWN
        }
    }
}
