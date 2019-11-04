package com.woocommerce.android.widgets

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.CompoundButton
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.TextView
import androidx.fragment.app.DialogFragment

import com.woocommerce.android.R

class NumberPickerDialog : DialogFragment(),
        DialogInterface.OnClickListener,
        CompoundButton.OnCheckedChangeListener {
    companion object {
        const val TITLE_KEY = "dialog-title"
        const val HEADER_TEXT_KEY = "header-text"
        const val MIN_VALUE_KEY = "min-value"
        const val MAX_VALUE_KEY = "max-value"
        const val CUR_VALUE_KEY = "cur-value"

        private const val DEFAULT_MIN_VALUE = 0
        private const val DEFAULT_MAX_VALUE = 99
    }

    private lateinit var headerText: TextView
    private lateinit var numberPicker: NumberPicker
    private lateinit var format: NumberPicker.Formatter
    private var minValue: Int = 0
    private var maxValue: Int = 0
    private var confirmed: Boolean = false

    private val resultIntent: Intent?
        get() = if (confirmed) {
            Intent().putExtra(CUR_VALUE_KEY, numberPicker.value)
        } else null

    init {
        minValue = DEFAULT_MIN_VALUE
        maxValue = DEFAULT_MAX_VALUE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(
                ContextThemeWrapper(activity, R.style.Woo_Dialog)
        )
        val view = View.inflate(activity, R.layout.number_picker_dialog, null)
        headerText = view.findViewById(R.id.number_picker_header)
        numberPicker = view.findViewById(R.id.number_picker)
        var value = minValue

        val args = arguments
        if (args != null) {
            headerText.text = args.getString(HEADER_TEXT_KEY, "")
            minValue = args.getInt(MIN_VALUE_KEY, DEFAULT_MIN_VALUE)
            maxValue = args.getInt(MAX_VALUE_KEY, DEFAULT_MAX_VALUE)
            value = args.getInt(CUR_VALUE_KEY, minValue)

            builder.setTitle(args.getString(TITLE_KEY, ""))
        }

        // Fix for https://issuetracker.google.com/issues/36952035
        val editText = numberPicker.getChildAt(0)
        if (editText is EditText) {
            editText.filters = arrayOfNulls(0)
        }

        numberPicker.setFormatter(format)
        numberPicker.minValue = minValue
        numberPicker.maxValue = maxValue
        numberPicker.value = value

        // hide empty text views
        if (TextUtils.isEmpty(headerText.text)) {
            headerText.visibility = View.GONE
        }

        builder.setPositiveButton(android.R.string.ok, this)
        builder.setNegativeButton(R.string.cancel, this)
        builder.setView(view)

        return builder.create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        confirmed = which == DialogInterface.BUTTON_POSITIVE
        dismiss()
    }

    override fun onCheckedChanged(buttonView: CompoundButton, isChecked: Boolean) {
        numberPicker.isEnabled = isChecked
        headerText.isEnabled = isChecked
    }

    override fun onDismiss(dialog: DialogInterface) {
        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        val target = targetFragment
        target?.onActivityResult(targetRequestCode, Activity.RESULT_OK, resultIntent)

        super.onDismiss(dialog)
    }

    fun setNumberFormat(format: NumberPicker.Formatter) {
        this.format = format
    }
}
