package com.woocommerce.android.ui.orders.creation

import javax.inject.Inject

interface CheckDigitRemover {
    fun getSKUWithoutCheckDigit(sku: String): String
}

class UPCCheckDigitRemover @Inject constructor() : CheckDigitRemover {
    override fun getSKUWithoutCheckDigit(sku: String): String {
        return sku.dropLast(1)
    }
}

class EAN13CheckDigitRemover @Inject constructor() : CheckDigitRemover {
    override fun getSKUWithoutCheckDigit(sku: String): String {
        return sku.dropLast(1)
    }
}

class EAN8CheckDigitRemover @Inject constructor() : CheckDigitRemover {
    override fun getSKUWithoutCheckDigit(sku: String): String {
        return sku.dropLast(1)
    }
}
