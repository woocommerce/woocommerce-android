package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingCustomsLineListItemBinding
import com.woocommerce.android.databinding.ShippingCustomsListItemBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.model.getTitle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsAdapter.PackageCustomsViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsLineAdapter.CustomsLineViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsViewModel.CustomsPackageUiState
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.WCMaterialOutlinedEditTextView
import com.woocommerce.android.widgets.WooClickableSpan

class ShippingCustomsAdapter(
    private val weightUnit: String,
    private val currencyUnit: String,
    private val countries: Array<Location>,
    private val listener: ShippingCustomsFormListener
) : RecyclerView.Adapter<PackageCustomsViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var customsPackages: List<CustomsPackageUiState> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(CustomsPackageDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    override fun getItemCount(): Int {
        return customsPackages.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageCustomsViewHolder {
        val binding = ShippingCustomsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PackageCustomsViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PackageCustomsViewHolder, position: Int) {
        holder.bind(customsPackages[position])
    }

    override fun getItemId(position: Int): Long {
        return customsPackages[position].data.id.hashCode().toLong()
    }

    @Suppress("MagicNumber")
    inner class PackageCustomsViewHolder(val binding: ShippingCustomsListItemBinding) : ViewHolder(binding.root) {
        private val linesAdapter: ShippingCustomsLineAdapter by lazy {
            ShippingCustomsLineAdapter(
                weightUnit = weightUnit,
                currencyUnit = currencyUnit,
                countries = countries,
                listener = listener
            )
        }
        private val context
            get() = binding.root.context

        private val isExpanded
            get() = binding.expandIcon.rotation == 180f

        init {
            binding.itemsList.apply {
                itemAnimator = null
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = linesAdapter
                itemAnimator = DefaultItemAnimator().apply {
                    supportsChangeAnimations = false
                }
            }

            val learnMoreText = context.getString(R.string.learn_more)
            binding.itnDescription.setClickableText(
                content = context.getString(R.string.shipping_label_customs_learn_more_itn, learnMoreText),
                clickableContent = learnMoreText,
                clickAction = WooClickableSpan {
                    ChromeCustomTabUtils.launchUrl(
                        context,
                        AppUrls.SHIPPING_LABEL_CUSTOMS_ITN
                    )
                }
            )

            // Setup listeners
            binding.returnCheckbox.setOnCheckedChangeListener { _, isChecked ->
                listener.onReturnToSenderChanged(adapterPosition, isChecked)
            }
            binding.contentsTypeSpinner.setup(
                values = ContentsType.values(),
                onSelected = { listener.onContentsTypeChanged(adapterPosition, it) },
                mapper = { context.getString(it.title) }
            )
            binding.contentsTypeDescription.setOnTextChangedListener {
                it?.let { listener.onContentsDescriptionChanged(adapterPosition, it.toString()) }
            }
            binding.restrictionTypeSpinner.setup(
                values = RestrictionType.values(),
                onSelected = { listener.onRestrictionTypeChanged(adapterPosition, it) },
                mapper = { context.getString(it.title) }
            )
            binding.restrictionTypeDescription.setOnTextChangedListener {
                it?.let { listener.onRestrictionDescriptionChanged(adapterPosition, it.toString()) }
            }
            binding.itnEditText.setOnTextChangedListener {
                it?.let { listener.onItnChanged(adapterPosition, it.toString()) }
            }
            binding.titleLayout.setOnClickListener {
                if (isExpanded) {
                    binding.expandIcon.animate().rotation(0f).start()
                    binding.detailsLayout.collapse()
                    listener.onPackageExpandedChanged(adapterPosition, false)
                } else {
                    binding.expandIcon.animate().rotation(180f).start()
                    binding.detailsLayout.expand()
                    listener.onPackageExpandedChanged(adapterPosition, true)
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(uiState: CustomsPackageUiState) {
            val (customsPackage, validationState) = uiState
            binding.packageId.text = customsPackage.labelPackage.getTitle(context)
            binding.packageName.text = "- ${customsPackage.labelPackage.selectedPackage!!.title}"
            binding.errorView.isVisible = !validationState.isValid
            binding.returnCheckbox.isChecked = customsPackage.returnToSender

            // Animate any potential change to visibility of the description fields
            val transition = AutoTransition().apply {
                binding.root.children.filter { it !is RecyclerView }.forEach { addTarget(it) }
            }
            TransitionManager.beginDelayedTransition(binding.root, transition)

            binding.contentsTypeSpinner.setText(customsPackage.contentsType.title)
            binding.contentsTypeDescription.setTextIfDifferent(customsPackage.contentsDescription.orEmpty())
            binding.contentsTypeDescription.isVisible = customsPackage.contentsType == ContentsType.Other
            binding.contentsTypeDescription.error = validationState.contentsDescriptionErrorMessage

            binding.restrictionTypeSpinner.setText(customsPackage.restrictionType.title)
            binding.restrictionTypeDescription.setTextIfDifferent(customsPackage.restrictionDescription.orEmpty())
            binding.restrictionTypeDescription.isVisible = customsPackage.restrictionType == RestrictionType.Other
            binding.restrictionTypeDescription.error = validationState.restrictionDescriptionErrorMessage

            binding.itnEditText.setTextIfDifferent(customsPackage.itn)
            binding.itnEditText.error = validationState.itnErrorMessage

            linesAdapter.parentItemPosition = adapterPosition
            linesAdapter.customsLines = uiState.customsLinesUiState

            if (uiState.isExpanded) {
                binding.expandIcon.rotation = 180f
                binding.detailsLayout.isVisible = true
            } else {
                binding.expandIcon.rotation = 0f
                binding.detailsLayout.isVisible = false
            }
        }
    }

    private class CustomsPackageDiffCallback(
        private val oldList: List<CustomsPackageUiState>,
        private val newList: List<CustomsPackageUiState>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].data.id == newList[newItemPosition].data.id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

class ShippingCustomsLineAdapter(
    private val weightUnit: String,
    private val currencyUnit: String,
    private val countries: Array<Location>,
    private val listener: ShippingCustomsFormListener
) : RecyclerView.Adapter<CustomsLineViewHolder>() {
    init {
        setHasStableIds(true)
    }

    var customsLines: List<CustomsLineUiState> = emptyList()
        set(value) {
            val diffResult = DiffUtil.calculateDiff(CustomsLineDiffCallback(field, value))
            field = value
            diffResult.dispatchUpdatesTo(this)
        }

    var parentItemPosition: Int = -1

    override fun getItemId(position: Int): Long {
        return customsLines[position].first.productId
    }

    override fun getItemCount(): Int = customsLines.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomsLineViewHolder {
        val binding = ShippingCustomsLineListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CustomsLineViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomsLineViewHolder, position: Int) {
        holder.bind(customsLines[position])
    }

    inner class CustomsLineViewHolder(val binding: ShippingCustomsLineListItemBinding) : ViewHolder(binding.root) {
        private val context
            get() = binding.root.context

        init {
            binding.titleLayout.setOnClickListener {
                if (binding.expandIcon.rotation == 0f) {
                    binding.expandIcon.animate().rotation(180f).start()
                    binding.detailsLayout.expand()
                    // TODO update the expand() function an on animation ended callback
                    binding.detailsLayout.postDelayed({ focusOnFirstInvalidField() }, 300)
                } else {
                    binding.expandIcon.animate().rotation(0f).start()
                    binding.detailsLayout.collapse()
                }
            }
            binding.weightEditText.hint = context.getString(R.string.shipping_label_customs_weight_hint, weightUnit)
            binding.valueEditText.hint = context.getString(R.string.shipping_label_customs_value_hint, currencyUnit)

            val learnMoreText = context.getString(R.string.learn_more)
            binding.hsTariffNumberInfos.setClickableText(
                content = context.getString(R.string.shipping_label_customs_learn_more_hs_tariff_number, learnMoreText),
                clickableContent = learnMoreText,
                clickAction = WooClickableSpan {
                    ChromeCustomTabUtils.launchUrl(
                        context,
                        AppUrls.SHIPPING_LABEL_CUSTOMS_HS_TARIFF_NUMBER
                    )
                }
            )

            binding.itemDescriptionEditText.setOnTextChangedListener {
                it?.let { listener.onItemDescriptionChanged(parentItemPosition, adapterPosition, it.toString()) }
            }
            binding.hsTariffNumberEditText.setOnTextChangedListener {
                it?.let { listener.onHsTariffNumberChanged(parentItemPosition, adapterPosition, it.toString()) }
            }
            binding.weightEditText.setOnTextChangedListener {
                it?.let { listener.onWeightChanged(parentItemPosition, adapterPosition, it.toString()) }
            }
            binding.valueEditText.setOnTextChangedListener {
                it?.let { listener.onItemValueChanged(parentItemPosition, adapterPosition, it.toString()) }
            }
            binding.countrySpinner.setup(
                values = countries,
                onSelected = { listener.onOriginCountryChanged(parentItemPosition, adapterPosition, it) },
                mapper = { it.name }
            )
        }

        private fun focusOnFirstInvalidField() {
            binding.detailsLayout.children.filterIsInstance(WCMaterialOutlinedEditTextView::class.java)
                .forEach {
                    if (!it.error.isNullOrEmpty()) {
                        it.requestFocus()
                        return
                    }
                }
        }

        fun bind(uiState: CustomsLineUiState) {
            val (customsLine, validationState) = uiState
            binding.lineTitle.text = context.getString(R.string.shipping_label_customs_line_item, adapterPosition + 1)

            binding.itemDescriptionEditText.setTextIfDifferent(customsLine.itemDescription)
            binding.itemDescriptionEditText.error = validationState.itemDescriptionErrorMessage

            binding.hsTariffNumberEditText.setTextIfDifferent(customsLine.hsTariffNumber)
            binding.hsTariffNumberEditText.error = validationState.hsTariffErrorMessage

            binding.weightEditText.setTextIfDifferent(customsLine.weight?.formatToString().orEmpty())
            binding.weightEditText.error = validationState.weightErrorMessage

            binding.valueEditText.setTextIfDifferent(customsLine.value?.toPlainString().orEmpty())
            binding.valueEditText.error = validationState.valueErrorMessage

            binding.countrySpinner.setText(customsLine.originCountry.name)

            binding.errorView.isVisible = !validationState.isValid
        }
    }

    private class CustomsLineDiffCallback(
        private val oldList: List<CustomsLineUiState>,
        private val newList: List<CustomsLineUiState>
    ) : DiffUtil.Callback() {
        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].first.productId == newList[newItemPosition].first.productId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }
    }
}

interface ShippingCustomsFormListener {
    fun onPackageExpandedChanged(position: Int, isExpanded: Boolean)
    fun onReturnToSenderChanged(position: Int, returnToSender: Boolean)
    fun onContentsTypeChanged(position: Int, contentsType: ContentsType)
    fun onContentsDescriptionChanged(position: Int, contentsDescription: String)
    fun onRestrictionTypeChanged(position: Int, restrictionType: RestrictionType)
    fun onRestrictionDescriptionChanged(position: Int, restrictionDescription: String)
    fun onItnChanged(position: Int, itn: String)
    fun onItemDescriptionChanged(packagePosition: Int, linePosition: Int, description: String)
    fun onHsTariffNumberChanged(packagePosition: Int, linePosition: Int, hsTariffNumber: String)
    fun onWeightChanged(packagePosition: Int, linePosition: Int, weight: String)
    fun onItemValueChanged(packagePosition: Int, linePosition: Int, itemValue: String)
    fun onOriginCountryChanged(packagePosition: Int, linePosition: Int, country: Location)
}
