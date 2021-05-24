package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingCustomsLineListItemBinding
import com.woocommerce.android.databinding.ShippingCustomsListItemBinding
import com.woocommerce.android.extensions.collapse
import com.woocommerce.android.extensions.expand
import com.woocommerce.android.extensions.setClickableText
import com.woocommerce.android.model.ContentsType
import com.woocommerce.android.model.CustomsLine
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.RestrictionType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsAdapter.PackageCustomsViewHolder
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingCustomsLineAdapter.CustomsLineViewHolder
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.widgets.WooClickableSpan

class ShippingCustomsAdapter(
    private val onReturnToSenderChanged: (Int, Boolean) -> Unit,
    private val onContentsTypeChanged: (Int, ContentsType) -> Unit,
    private val onRestrictionTypeChanged: (Int, RestrictionType) -> Unit,
    private val onItnChanged: (Int, String) -> Unit
) : RecyclerView.Adapter<PackageCustomsViewHolder>() {
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

    inner class PackageCustomsViewHolder(val binding: ShippingCustomsListItemBinding) : ViewHolder(binding.root) {
        private val linesAdapter: ShippingCustomsLineAdapter by lazy { ShippingCustomsLineAdapter() }
        private val context
            get() = binding.root.context

        init {
            binding.itemsList.apply {
                itemAnimator = null
                layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
                adapter = linesAdapter
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
                onReturnToSenderChanged(adapterPosition, isChecked)
            }
            binding.contentsTypeSpinner.setup(
                values = ContentsType.values(),
                onSelected = { onContentsTypeChanged(adapterPosition, it) },
                mapper = { context.getString(it.title) }
            )
            binding.restrictionTypeSpinner.setup(
                values = RestrictionType.values(),
                onSelected = { onRestrictionTypeChanged(adapterPosition, it) },
                mapper = { context.getString(it.title) }
            )
            binding.itnEditText.setOnTextChangedListener {
                it?.let { onItnChanged(adapterPosition, it.toString()) }
            }
        }

        @SuppressLint("SetTextI18n")
        fun bind(customsPackage: CustomsPackage) {
            binding.packageId.text = context.getString(
                R.string.orderdetail_shipping_label_item_header,
                adapterPosition + 1
            )
            binding.packageName.text = "- ${customsPackage.box.title}"
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
        private val context
            get() = binding.root.context

        init {
            binding.expandIcon.setOnClickListener {
                if (binding.expandIcon.rotation == 0f) {
                    binding.expandIcon.rotation = 180f
                    // binding.expandIcon.animate().rotation(180f).start()
                    binding.detailsLayout.expand()
                } else {
                    binding.expandIcon.rotation = 0f
                    // binding.expandIcon.animate().rotation(0f).start()
                    binding.detailsLayout.collapse()
                }
            }
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
        }

        fun bind(customsPackage: CustomsLine) {
            binding.lineTitle.text = context.getString(R.string.shipping_label_customs_line_item, adapterPosition + 1)
            binding.itemDescriptionEditText.setText(customsPackage.itemDescription)
            binding.hsTariffNumberEditText.setText(customsPackage.hsTariffNumber)
            binding.weightEditText.setText(customsPackage.weight.toString())
            binding.valueEditText.setText(customsPackage.value.toPlainString())
            binding.countrySpinner.setText(customsPackage.originCountry)
        }
    }
}
