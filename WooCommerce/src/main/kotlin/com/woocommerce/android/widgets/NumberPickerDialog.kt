package com.woocommerce.android.widgets

import android.app.Activity
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.NumberPicker.Formatter
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.woocommerce.android.R

open class NumberPickerDialog : DialogFragment(), DialogInterface.OnClickListener {
    companion object {
        const val TITLE_KEY = "title"
        const val MIN_VALUE_KEY = "minValue"
        const val MAX_VALUE_KEY = "maxValue"
        const val CUR_VALUE_KEY = "currentValue"

        private const val DEFAULT_MIN_VALUE = 0
        private const val DEFAULT_MAX_VALUE = 99
    }

    private lateinit var headerText: TextView
    private lateinit var numberPicker: NumberPicker

    private var format: Formatter = Formatter { it.toString() }
    private var minValue: Int = 0
    private var maxValue: Int = 0
    private var confirmed: Boolean = false

    init {
        minValue = DEFAULT_MIN_VALUE
        maxValue = DEFAULT_MAX_VALUE
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = MaterialAlertDialogBuilder(requireActivity())
        val view = View.inflate(activity, R.layout.number_picker_dialog, null)
        headerText = view.findViewById(R.id.number_picker_header)
        numberPicker = view.findViewById(R.id.number_picker)
        var value = minValue

        val args = arguments
        if (args != null) {
            headerText.text = args.getString(TITLE_KEY, "")
            minValue = args.getInt(MIN_VALUE_KEY, DEFAULT_MIN_VALUE)
            maxValue = args.getInt(MAX_VALUE_KEY, DEFAULT_MAX_VALUE)
            value = args.getInt(CUR_VALUE_KEY, minValue)
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

    override fun onSaveInstanceState(outState: Bundle) {
        arguments?.putInt(CUR_VALUE_KEY, numberPicker.value)

        super.onSaveInstanceState(outState)
    }

    override fun onDismiss(dialog: DialogInterface) {
        if (confirmed) {
            returnResult(numberPicker.value)
        }

        super.onDismiss(dialog)
    }

    open fun returnResult(selectedValue: Int) {
        val target = targetFragment
        val resultIntent = Intent()
        arguments?.let { resultIntent.replaceExtras(it) }
        resultIntent.putExtra(CUR_VALUE_KEY, numberPicker.value)
        target?.onActivityResult(targetRequestCode, Activity.RESULT_OK, resultIntent)
    }

    fun setNumberFormat(format: Formatter) {
        this.format = format
    }
}
