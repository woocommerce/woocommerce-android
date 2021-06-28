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
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.PackageProductsAdapter.PackageProductViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelPackagesAdapter.ShippingLabelPackageViewHolder
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.StringUtils

class ShippingLabelPackagesAdapter(
    val weightUnit: String,
    val onWeightEdited: (Int, Float) -> Unit,
    val onPackageSpinnerClicked: (Int) -> Unit
) : RecyclerView.Adapter<ShippingLabelPackageViewHolder>() {
    var shippingLabelPackages: List<ShippingLabelPackage> = emptyList()
        set(value) {
            val diff = DiffUtil.calculateDiff(ShippingLabelPackageDiffCallback(field, value))
            field = value
            diff.dispatchUpdatesTo(this)
        }

    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShippingLabelPackageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ShippingLabelPackageViewHolder(
            ShippingLabelPackageDetailsListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun getItemCount() = shippingLabelPackages.count()

    override fun getItemId(position: Int): Long = shippingLabelPackages[position].packageId.hashCode().toLong()

    override fun onBindViewHolder(holder: ShippingLabelPackageViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ShippingLabelPackageViewHolder(
        val binding: ShippingLabelPackageDetailsListItemBinding
    ) : ViewHolder(binding.root) {
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
                if (weight == shippingLabelPackages[adapterPosition].weight) return@setOnTextChangedListener

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

            if (!FeatureFlag.SHIPPING_LABELS_M4.isEnabled()) {
                binding.expandIcon.isVisible = false
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val context = binding.root.context
            val shippingLabelPackage = shippingLabelPackages[position]
            binding.packageName.text = context.getString(
                R.string.shipping_label_package_details_title_template,
                position + 1
            )
            binding.packageItemsCount.text = "- ${context.resources.getQuantityString(
                R.plurals.shipping_label_package_details_items_count,
                shippingLabelPackage.items.size,
                shippingLabelPackage.items.size
            )}"
            (binding.itemsList.adapter as PackageProductsAdapter).items = shippingLabelPackage.adaptItemsForUi()
            binding.selectedPackageSpinner.setText(shippingLabelPackage.selectedPackage?.title ?: "")
            if (!shippingLabelPackage.weight.isNaN()) {
                binding.weightEditText.setTextIfDifferent(shippingLabelPackage.weight.toString())
            }
        }

        private fun ShippingLabelPackage.adaptItemsForUi(): List<ShippingLabelPackage.Item> {
            return items.map { item ->
                List(item.quantity) { item }
            }.flatten()
        }
    }

    private class ShippingLabelPackageDiffCallback(
        private val oldList: List<ShippingLabelPackage>,
        private val newList: List<ShippingLabelPackage>
    ) : DiffUtil.Callback() {
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].items == newList[newItemPosition].items
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
    }
}

class PackageProductsAdapter(private val weightUnit: String) : RecyclerView.Adapter<PackageProductViewHolder>() {
    var items: List<ShippingLabelPackage.Item> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

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
        init {
            if (!FeatureFlag.SHIPPING_LABELS_M4.isEnabled()) {
                binding.moveButton.isVisible = false
            }
        }

        fun bind(item: ShippingLabelPackage.Item) {
            binding.productName.text = item.name
            val attributes = item.attributesList.takeIf { it.isNotEmpty() }?.let { "$it \u2981 " } ?: StringUtils.EMPTY
            val details = "$attributes${item.weight} $weightUnit"
            if (details.isEmpty()) {
                binding.productDetails.isVisible = false
            } else {
                binding.productDetails.isVisible = true
                binding.productDetails.text = details
            }
        }
    }
}
