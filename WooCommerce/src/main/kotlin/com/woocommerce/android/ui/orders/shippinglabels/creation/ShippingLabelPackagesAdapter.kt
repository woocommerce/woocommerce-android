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
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.util.StringUtils

class ShippingLabelPackagesAdapter(
    val parameters: SiteParameters,
    val onWeightEdited: (Int, Double) -> Unit,
    val onPackageSpinnerClicked: (Int) -> Unit
) : RecyclerView.Adapter<ShippingLabelPackageViewHolder>() {
    var shippingLabelPackages: List<ShippingLabelPackage> = emptyList()
        set(value) {
            val diff = DiffUtil.calculateDiff(ShippingLabelPackageDiffCallback(field, value))
            field = value
            diff.dispatchUpdatesTo(this)
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShippingLabelPackageViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return ShippingLabelPackageViewHolder(
            ShippingLabelPackageDetailsListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun getItemCount() = shippingLabelPackages.count()

    override fun onBindViewHolder(holder: ShippingLabelPackageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun onBindViewHolder(holder: ShippingLabelPackageViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.size == 1 && payloads[0] == ChangePayload.Weight) {
            // If the only change is weight, avoid updating the view, as it already has the last changes
            return
        } else {
            onBindViewHolder(holder, position)
        }
    }

    inner class ShippingLabelPackageViewHolder(
        val binding: ShippingLabelPackageDetailsListItemBinding
    ) : ViewHolder(binding.root) {
        init {
            with(binding.itemsList) {
                layoutManager =
                    LinearLayoutManager(binding.root.context, LinearLayoutManager.VERTICAL, false)
                val canMoveItems = shippingLabelPackages.count() > 1 ||
                    shippingLabelPackages.firstOrNull()?.items?.count() ?: 0 > 1
                adapter = PackageProductsAdapter(canMoveItems = canMoveItems)
            }
            binding.weightEditText.hint = binding.root.context.getString(
                R.string.shipping_label_package_details_weight_hint,
                parameters.weightUnit
            )
            binding.weightEditText.setOnTextChangedListener {
                onWeightEdited(
                    adapterPosition,
                    it?.toString()?.trim('.')?.ifEmpty { null }?.toDouble() ?: Double.NaN
                )
            }
            binding.selectedPackageSpinner.setOnClickListener {
                onPackageSpinnerClicked(adapterPosition)
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
            binding.packageItemsCount.text = " - ${context.resources.getQuantityString(
                R.plurals.shipping_label_package_details_items_count,
                shippingLabelPackage.items.size,
                shippingLabelPackage.items.size
            )}"
            (binding.itemsList.adapter as PackageProductsAdapter).items = shippingLabelPackage.items
            binding.selectedPackageSpinner.setText(shippingLabelPackage.selectedPackage.title)
            if (!shippingLabelPackage.weight.isNaN()) {
                binding.weightEditText.setText(shippingLabelPackage.weight.toString())
            }
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

        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
            return if (oldList[oldItemPosition].items == newList[newItemPosition].items &&
                oldList[oldItemPosition].selectedPackage == newList[newItemPosition].selectedPackage) {
                ChangePayload.Weight
            } else null
        }
    }

    // TODO We will the ExpansionState to animate collapsing expanding later
    enum class ChangePayload {
        Weight, ExpansionState
    }
}

class PackageProductsAdapter(val canMoveItems: Boolean) : RecyclerView.Adapter<PackageProductViewHolder>() {
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
        fun bind(item: ShippingLabelPackage.Item) {
            binding.moveButton.isVisible = canMoveItems
            binding.productName.text = item.name
            val attributes = item.attributesList.takeIf { it.isNotEmpty() }?.let { "$it \u2981 " } ?: StringUtils.EMPTY
            val details = "$attributes${item.weight}"
            if (details.isEmpty()) {
                binding.productDetails.isVisible = false
            } else {
                binding.productDetails.isVisible = true
                binding.productDetails.text = details
            }
        }
    }
}
