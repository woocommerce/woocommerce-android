package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingLabelPackageDetailsListItemBinding
import com.woocommerce.android.databinding.ShippingLabelPackageProductListItemBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.formatToString
import com.woocommerce.android.extensions.getColorCompat
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.getTitle
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPackagesViewModel.ShippingLabelPackageUiModel
import com.woocommerce.android.ui.orders.shippinglabels.creation.PackageProductsAdapter.PackageProductViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelPackagesAdapter.ShippingLabelPackageViewHolder
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.StringUtils

class ShippingLabelPackagesAdapter(
    val siteParameters: SiteParameters,
    val onWeightEdited: (Int, Float) -> Unit,
    val onExpandedChanged: (Int, Boolean) -> Unit,
    val onPackageSpinnerClicked: (Int) -> Unit,
    val onMoveItemClicked: (ShippingLabelPackage.Item, ShippingLabelPackage) -> Unit
) : RecyclerView.Adapter<ShippingLabelPackageViewHolder>() {
    var uiModels: List<ShippingLabelPackageUiModel> = emptyList()
        set(value) {
            val diff = DiffUtil.calculateDiff(ShippingLabelPackageDiffCallback(field, value))
            field = value
            diff.dispatchUpdatesTo(this)
        }

    private val weightUnit
        get() = siteParameters.weightUnit.orEmpty()

    private val dimensionUnit
        get() = siteParameters.dimensionUnit.orEmpty()

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShippingLabelPackageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ShippingLabelPackageViewHolder(
            ShippingLabelPackageDetailsListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun getItemCount() = uiModels.size

    override fun getItemId(position: Int): Long = uiModels[position].data.packageId.hashCode().toLong()

    override fun onBindViewHolder(holder: ShippingLabelPackageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onBindViewHolder(holder: ShippingLabelPackageViewHolder, position: Int, payloads: MutableList<Any>) {
        // When the difference is only the expansion state, then skip updating to keep animations smoother
        if (payloads.size == 1 &&
            payloads.contains(ShippingLabelPackageDiffCallback.EXPANSION_STATE_PAYLOAD) &&
            holder.isExpanded == uiModels[position].isExpanded
        ) return
        super.onBindViewHolder(holder, position, payloads)
    }

    @Suppress("MagicNumber")
    inner class ShippingLabelPackageViewHolder(
        val binding: ShippingLabelPackageDetailsListItemBinding
    ) : ViewHolder(binding.root) {
        val isExpanded
            get() = binding.expandIcon.rotation == 180f

        init {
            with(binding.itemsList) {
                layoutManager =
                    LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
                adapter = PackageProductsAdapter(weightUnit)
            }
            binding.weightEditText.hint = binding.root.context.getString(
                R.string.shipping_label_package_details_weight_hint,
                weightUnit
            )
            binding.weightEditText.setOnTextChangedListener {
                val weight = it?.toString()?.trim('.')?.ifEmpty { null }?.toFloat() ?: Float.NaN
                // Return early if the weight wasn't changed
                if (weight == uiModels[adapterPosition].data.weight) return@setOnTextChangedListener

                onWeightEdited(adapterPosition, weight)

                if (weight <= 0.0) {
                    val context = binding.root.context
                    binding.weightEditText.error =
                        context.getString(R.string.shipping_label_package_details_weight_error)
                } else {
                    binding.weightEditText.error = null
                }
            }

            binding.selectedPackageSpinner.setClickListener {
                onPackageSpinnerClicked(adapterPosition)
            }

            binding.titleLayout.setOnClickListener {
                if (isExpanded) {
                    binding.expandIcon.animate().rotation(0f).start()
                    binding.detailsLayout.collapse()
                    onExpandedChanged(adapterPosition, false)
                } else {
                    binding.expandIcon.animate().rotation(180f).start()
                    binding.detailsLayout.expand()
                    onExpandedChanged(adapterPosition, true)
                }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val context = binding.root.context
            val uiModel = uiModels[position]
            val shippingLabelPackage = uiModel.data
            binding.packageName.text = shippingLabelPackage.getTitle(context)

            binding.packageItemsCount.text = "- ${StringUtils.getQuantityString(
                context = context,
                quantity = shippingLabelPackage.itemsCount,
                default = R.string.shipping_label_package_details_items_count_many,
                one = R.string.shipping_label_package_details_items_count_one,
            )}"

            binding.errorView.isVisible = !uiModel.isValid
            with(binding.itemsList.adapter as PackageProductsAdapter) {
                items = shippingLabelPackage.adaptItemsForUi()
                moveItemClickListener = { item -> onMoveItemClicked(item, shippingLabelPackage) }
            }
            if (shippingLabelPackage.selectedPackage?.isIndividual == true) {
                binding.selectedPackageSpinner.isVisible = false
                binding.individualPackageLayout.isVisible = true
                binding.individualPackageDimensions.isVisible = true
                binding.individualPackageDimensions.text = with(shippingLabelPackage.selectedPackage.dimensions) {
                    "${length.formatToString()} $dimensionUnit x" +
                        " ${width.formatToString()} $dimensionUnit x" +
                        " ${height.formatToString()} $dimensionUnit"
                }
                if (!shippingLabelPackage.selectedPackage.dimensions.isValid) {
                    binding.individualPackageDimensions.setTextColor(context.getColorCompat(R.color.color_error))
                    binding.individualPackageError.isVisible = true
                } else {
                    binding.individualPackageDimensions
                        .setTextColor(context.getColorCompat(R.color.color_on_surface_medium))
                    binding.individualPackageError.isVisible = false
                }
            } else {
                binding.selectedPackageSpinner.isVisible = true
                binding.individualPackageLayout.isVisible = false
                binding.selectedPackageSpinner.setText(shippingLabelPackage.selectedPackage?.title ?: "")
            }
            if (!shippingLabelPackage.weight.isNaN()) {
                binding.weightEditText.setTextIfDifferent(shippingLabelPackage.weight.toString())
            }
            if (uiModel.isExpanded) {
                binding.expandIcon.rotation = 180f
                binding.detailsLayout.isVisible = true
            } else {
                binding.expandIcon.rotation = 0f
                binding.detailsLayout.isVisible = false
            }
        }

        private fun ShippingLabelPackage.adaptItemsForUi(): List<ShippingLabelPackage.Item> {
            return items.map { item ->
                List(item.quantity) { item }
            }.flatten()
        }
    }

    private class ShippingLabelPackageDiffCallback(
        private val oldList: List<ShippingLabelPackageUiModel>,
        private val newList: List<ShippingLabelPackageUiModel>
    ) : DiffUtil.Callback() {
        companion object {
            const val EXPANSION_STATE_PAYLOAD = "expansion_state"
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].data.items == newList[newItemPosition].data.items
        }

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return if (oldItem.data == newItem.data && oldItem.isExpanded != newItem.isExpanded) {
                EXPANSION_STATE_PAYLOAD
            } else {
                null
            }
        }
    }
}

class PackageProductsAdapter(
    private val weightUnit: String
) : RecyclerView.Adapter<PackageProductViewHolder>() {
    var items: List<ShippingLabelPackage.Item> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    lateinit var moveItemClickListener: (ShippingLabelPackage.Item) -> Unit

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PackageProductViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PackageProductViewHolder(
            ShippingLabelPackageProductListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun getItemCount() = items.count()

    override fun onBindViewHolder(holder: PackageProductViewHolder, position: Int) = holder.bind(items[position])

    inner class PackageProductViewHolder(
        val binding: ShippingLabelPackageProductListItemBinding
    ) : ViewHolder(binding.root) {
        fun bind(item: ShippingLabelPackage.Item) {
            binding.productName.text = item.name
            val attributes = item.attributesDescription
                .takeIf { it.isNotEmpty() }
                ?.let { "$it \u2981 " }
                ?: StringUtils.EMPTY
            val details = "$attributes${item.weight} $weightUnit"
            if (details.isEmpty()) {
                binding.productDetails.isVisible = false
            } else {
                binding.productDetails.isVisible = true
                binding.productDetails.text = details
            }
            binding.moveButton.setOnClickListener {
                moveItemClickListener(item)
            }
        }
    }
}
