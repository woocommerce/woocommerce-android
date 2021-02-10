package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.woocommerce.android.R
import com.woocommerce.android.databinding.ShippingLabelPaymentMethodListItemBinding
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelPaymentViewModel.PaymentMethodUiModel
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelPaymentMethodsAdapter.PaymentMethodViewHolder
import java.text.SimpleDateFormat
import java.util.Locale

class ShippingLabelPaymentMethodsAdapter : RecyclerView.Adapter<PaymentMethodViewHolder>() {
    private val dateFormat by lazy {
        SimpleDateFormat("MM/yy", Locale.getDefault())
    }
    var items: List<PaymentMethodUiModel> = emptyList()
        set(value) {
            field = value
            notifyDataSetChanged()
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentMethodViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return PaymentMethodViewHolder(
            ShippingLabelPaymentMethodListItemBinding.inflate(layoutInflater, parent, false)
        )
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: PaymentMethodViewHolder, position: Int) {
        holder.bind(items[position])
    }

    inner class PaymentMethodViewHolder(val binding: ShippingLabelPaymentMethodListItemBinding) : ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(item: PaymentMethodUiModel) {
            val context = binding.root.context
            binding.radioButton.isChecked = item.isSelected
            val cardType = context.getPaymentMethodTranslation(item.paymentMethod.cardType)
            binding.cardTypeNumber.text = context.getString(
                R.string.shipping_label_payments_type_digits, cardType, item.paymentMethod.cardDigits
            )
            binding.cardholderName.text = item.paymentMethod.name
            binding.expirationDate.text = context.getString(
                R.string.shipping_label_payments_expiration_date,
                dateFormat.format(item.paymentMethod.expirationDate)
            )
        }

        private fun Context.getPaymentMethodTranslation(paymentMethod: String): String {
            return getString(
                when (paymentMethod) {
                    "amex" -> R.string.payment_method_american_express
                    "discover" -> R.string.payment_method_discover
                    "mastercard" -> R.string.payment_method_mastercard
                    "visa" -> R.string.payment_method_visa
                    "paypal" -> R.string.payment_method_paypal
                    else -> throw IllegalArgumentException()
                }
            )
        }
    }
}

