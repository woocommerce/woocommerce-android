package com.woocommerce.android.ui.woopos.common.barcode

interface WooPosCheckDigitRemover {
    fun getCodeWithoutCheckDigit(code: String): String
}

class WooPosUPCCheckDigitRemover : WooPosCheckDigitRemover {
    override fun getCodeWithoutCheckDigit(code: String): String {
        return code.dropLast(1)
    }
}

class WooPosEAN13CheckDigitRemover : WooPosCheckDigitRemover {
    override fun getCodeWithoutCheckDigit(code: String): String {
        return code.dropLast(1)
    }
}

class WooPosEAN8CheckDigitRemover : WooPosCheckDigitRemover {
    override fun getCodeWithoutCheckDigit(code: String): String {
        return code.dropLast(1)
    }
}