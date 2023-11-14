package com.woocommerce.android.widgets

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.view.View
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R
import com.woocommerce.android.extensions.filterNotNull
import com.woocommerce.android.extensions.serializable
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
    private lateinit var currencyEditTextLayout: WCMaterialOutlinedCurrencyEditTextView
    private lateinit var messageText: TextView

    private var currentValue: BigDecimal = BigDecimal.ZERO
    private var confirmed: Boolean = false
    private var maxValue: BigDecimal = BigDecimal(Double.MAX_VALUE)
    private var minValue: BigDecimal = BigDecimal(Double.MIN_VALUE)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val view = View.inflate(activity, R.layout.currency_amount_dialog, null)

        headerText = view.findViewById(R.id.currencyAmount_header)
        messageText = view.findViewById(R.id.currencyAmount_message)
        currencyEditTextLayout = view.findViewById(R.id.currencyAmount_refundAmountInputLayout)

        val args = arguments
        if (args != null) {
            headerText.text = args.getString(TITLE_KEY, "")
            currentValue = args.serializable(CURRENT_VALUE_KEY) ?: BigDecimal.ZERO
            maxValue = args.serializable(MAX_VALUE_KEY) ?: BigDecimal(Double.MAX_VALUE)
            minValue = args.serializable(MIN_VALUE_KEY) ?: BigDecimal(Double.MIN_VALUE)
            messageText.text = args.getString(MESSAGE_KEY, "")
        }

        currencyEditTextLayout.setValue(currentValue)

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

        currencyEditTextLayout.value.filterNotNull().observe(this) {
            currentValue = if (it > maxValue) maxValue else it
            isAmountValid(it)
        }

        Handler(Looper.getMainLooper()).postDelayed(
            {
                currencyEditTextLayout.requestFocus()
                ActivityUtils.showKeyboard(currencyEditTextLayout)
            },
            200
        )

        return builder.create()
    }

    private fun isAmountValid(amount: BigDecimal): Boolean {
        return when {
            amount > maxValue -> {
                currencyEditTextLayout.error = getString(R.string.order_refunds_refund_high_error)
                false
            }
            amount < minValue -> {
                currencyEditTextLayout.error = getString(R.string.order_refunds_refund_zero_error)
                false
            }
            else -> {
                currencyEditTextLayout.error = null
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

    @Suppress("DEPRECATION")
    open fun returnResult(enteredAmount: BigDecimal) {
        val target = targetFragment
        val resultIntent = Intent()
        arguments?.let { resultIntent.replaceExtras(it) }
        resultIntent.putExtra(CURRENT_VALUE_KEY, enteredAmount)
        target?.onActivityResult(targetRequestCode, Activity.RESULT_OK, resultIntent)
    }
}
