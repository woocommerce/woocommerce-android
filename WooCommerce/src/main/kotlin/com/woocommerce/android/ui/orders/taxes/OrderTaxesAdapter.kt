package com.woocommerce.android.ui.orders.taxes

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.woocommerce.android.R
import com.woocommerce.android.databinding.OrderTaxItemBinding
import com.woocommerce.android.model.Order
import com.woocommerce.android.util.CurrencyFormatter

class OrderTaxesAdapter(
    private val currencyFormatter: CurrencyFormatter,
    private val currencyCode: String
) : RecyclerView.Adapter<OrderTaxesAdapter.ViewHolder>() {
    private var taxes: List<Order.TaxLine> = emptyList()

    init {
        setHasStableIds(true)
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateTaxes(newTaxes: List<Order.TaxLine>) {
        taxes = newTaxes
        notifyDataSetChanged()
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
        holder.bind(taxes[position])
    }

    override fun getItemId(position: Int): Long = taxes[position].id

    override fun getItemCount(): Int = taxes.size

    class ViewHolder(
        private val viewBinding: OrderTaxItemBinding,
        private val currencyFormatter: CurrencyFormatter,
        private val currencyCode: String
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bind(tax: Order.TaxLine) {
            val context = viewBinding.root.context
            viewBinding.taxLabel.text =
                String.format(context.getString(R.string.tax_name_with_tax_percent), tax.label, tax.ratePercent)
            viewBinding.taxValue.text = currencyFormatter.formatCurrency(tax.taxTotal, currencyCode)
        }
    }
}
