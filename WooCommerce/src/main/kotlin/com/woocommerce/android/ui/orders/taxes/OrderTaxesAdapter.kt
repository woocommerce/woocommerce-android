package com.woocommerce.android.ui.orders.taxes

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderTaxItemBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CurrencyFormatter

class OrderTaxesAdapter(
    private val currencyFormatter: CurrencyFormatter,
    private val currencyCode: String
) : ListAdapter<Order.TaxLine, OrderTaxesAdapter.ViewHolder>(TaxLineDiffCallBack) {
    init {
        setHasStableIds(true)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            OrderTaxItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            ),
            currencyFormatter,
            currencyCode
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun getItemId(position: Int): Long = getItem(position).id

    class ViewHolder(
        private val viewBinding: OrderTaxItemBinding,
        private val currencyFormatter: CurrencyFormatter,
        private val currencyCode: String
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(tax: Order.TaxLine) {
            val context = viewBinding.root.context
            viewBinding.taxLabel.text =
                context.getString(R.string.tax_name_with_tax_percent, tax.label, tax.ratePercent.toString())
            viewBinding.taxValue.text = currencyFormatter.formatCurrency(tax.taxTotal, currencyCode)
        }
    }

    object TaxLineDiffCallBack : DiffUtil.ItemCallback<Order.TaxLine>() {
        override fun areItemsTheSame(
            oldItem: Order.TaxLine,
            newItem: Order.TaxLine
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Order.TaxLine,
            newItem: Order.TaxLine
        ): Boolean = oldItem == newItem
    }
}
