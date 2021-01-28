package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
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

class ShippingLabelPackagesAdapter(val parameters: SiteParameters) : RecyclerView.Adapter<ShippingLabelPackageViewHolder>() {
    var shippingLabelPackages: List<ShippingLabelPackage> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
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
        }

        @SuppressLint("SetTextI18n")
        fun bind(position: Int) {
            val context = binding.root.context
            val packageDetails = shippingLabelPackages[position]
            binding.packageName.text = context.getString(
                R.string.shipping_label_package_details_title_template,
                position + 1
            )
            binding.packageItemsCount.text = " - ${context.getString(
                R.string.shipping_label_package_details_items_count,
                packageDetails.items.count()
            )}"
            (binding.itemsList.adapter as PackageProductsAdapter).items = packageDetails.items
            binding.selectedPackageSpinner.setText(packageDetails.selectedPackage.title)
            if (packageDetails.weight != -1) {
                binding.weightEditText.setText(packageDetails.weight.toString())
            }
        }
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

    inner class PackageProductViewHolder(val binding: ShippingLabelPackageProductListItemBinding) : ViewHolder(binding.root) {
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
