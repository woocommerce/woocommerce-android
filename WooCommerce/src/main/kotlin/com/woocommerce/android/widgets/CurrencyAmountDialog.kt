package com.woocommerce.android.widgets

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Observer
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.woocommerce.android.R
import com.woocommerce.android.util.CurrencyFormatter
import org.wordpress.android.util.ActivityUtils
import java.math.BigDecimal

open class CurrencyAmountDialog : DialogFragment(), DialogInterface.OnClickListener {
    companion object {
        const val TITLE_KEY = "title"
        const val CURRENT_VALUE_KEY = "currentValue"
        const val MIN_VALUE_KEY = "minValue"
        const val MAX_VALUE_KEY = "maxValue"
        const val MESSAGE_KEY = "message"
    }

    private lateinit var headerText: TextView
    private lateinit var inputLayout: TextInputLayout
    private lateinit var amountText: CurrencyEditText
    private lateinit var messageText: TextView

    private var currentValue: BigDecimal = BigDecimal.ZERO
    private var confirmed: Boolean = false
    private var maxValue: BigDecimal = BigDecimal(Double.MAX_VALUE)
    private var minValue: BigDecimal = BigDecimal(Double.MIN_VALUE)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val view = View.inflate(activity, R.layout.currency_amount_dialog, null)

        headerText = view.findViewById(R.id.currencyAmount_header)
        amountText = view.findViewById(R.id.currencyAmount_refundAmount)
        messageText = view.findViewById(R.id.currencyAmount_message)
        inputLayout = view.findViewById(R.id.currencyAmount_refundAmountInputLayout)

        val args = arguments
        if (args != null) {
            headerText.text = args.getString(TITLE_KEY, "")
            currentValue = args.getSerializable(CURRENT_VALUE_KEY) as? BigDecimal ?: BigDecimal.ZERO
            maxValue = args.getSerializable(MAX_VALUE_KEY) as? BigDecimal ?: BigDecimal(Double.MAX_VALUE)
            minValue = args.getSerializable(MIN_VALUE_KEY) as? BigDecimal ?: BigDecimal(Double.MIN_VALUE)
            messageText.text = args.getString(MESSAGE_KEY, "")
        }

        amountText.setValue(currentValue)

        // hide empty text views
        if (TextUtils.isEmpty(headerText.text)) {
            headerText.visibility = View.GONE
        }

        if (TextUtils.isEmpty(messageText.text)) {
            messageText.visibility = View.GONE
        }

        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(R.string.cancel, this)
        builder.setView(view)

        amountText.value.observe(this, Observer {
            currentValue = if (it > maxValue) maxValue else it
            isAmountValid(it)
        })

        Handler().postDelayed({
            amountText.requestFocus()
            ActivityUtils.showKeyboard(amountText)
        }, 200)

        return builder.create()
    }

    fun initializeCurrencyEditText(currency: String, decimals: Int, currencyFormatter: CurrencyFormatter) {
        amountText.initView(currency, decimals, currencyFormatter)
    }

    private fun isAmountValid(amount: BigDecimal): Boolean {
        return when {
            amount > maxValue -> {
                inputLayout.error = getString(R.string.order_refunds_refund_high_error)
                false
            }
            amount < minValue -> {
                inputLayout.error = getString(R.string.order_refunds_refund_zero_error)
                false
            }
            else -> {
                inputLayout.error = null
                true
            }
        }
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        confirmed = which == DialogInterface.BUTTON_POSITIVE
        dismiss()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        arguments?.putSerializable(CURRENT_VALUE_KEY, currentValue)

        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (confirmed) {
            returnResult(currentValue)
        }

        super.onDismiss(dialog)
    }

    open fun returnResult(enteredAmount: BigDecimal) {
        val target = targetFragment
        val resultIntent = Intent()
        arguments?.let { resultIntent.replaceExtras(it) }
        resultIntent.putExtra(CURRENT_VALUE_KEY, enteredAmount)
        target?.onActivityResult(targetRequestCode, Activity.RESULT_OK, resultIntent)
    }
}
