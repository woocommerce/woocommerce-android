package com.woocommerce.android.extensions

import android.widget.RadioButton
import android.widget.RadioGroup

fun RadioGroup.getCheckValue(): String {
    return (findViewById<RadioButton>(checkedRadioButtonId)).text.toString()
}
