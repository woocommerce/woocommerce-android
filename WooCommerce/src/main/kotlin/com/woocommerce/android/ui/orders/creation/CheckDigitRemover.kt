package com.woocommerce.android.ui.orders.creation

import javax.inject.Inject

interface CheckDigitRemover {
    fun getSKUWithoutCheckDigit(sku: String): String
}

class UPCCheckDigitRemover @Inject constructor() : CheckDigitRemover {
    override fun getSKUWithoutCheckDigit(sku: String): String {
        return sku.substring(0, sku.length - 1)
    }
}
