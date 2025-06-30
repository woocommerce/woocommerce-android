package com.woocommerce.android.util

import android.content.Context
import com.woocommerce.android.model.UiString
import com.woocommerce.android.util.UiHelpers.getTextOfUiString
import javax.inject.Inject

class UiStringParser @Inject constructor(
    private val context: Context
) {
    fun asString(uiString: UiString): String = getTextOfUiString(context, uiString)
}
