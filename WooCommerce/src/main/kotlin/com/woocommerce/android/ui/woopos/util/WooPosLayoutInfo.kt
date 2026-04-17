package com.woocommerce.android.ui.woopos.util

import android.content.Context
import com.woocommerce.android.ui.woopos.util.ext.isWooPosPhoneLayout
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class WooPosLayoutInfo @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isPhoneLayout(): Boolean = context.isWooPosPhoneLayout()
}
