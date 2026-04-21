package com.woocommerce.android.ui.woopos.util.format

import android.content.Context
import android.text.format.DateFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class Is24HourFormat @Inject constructor(
    @ApplicationContext private val context: Context
) {
    operator fun invoke(): Boolean = DateFormat.is24HourFormat(context)
}
