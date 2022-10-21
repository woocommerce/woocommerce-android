package com.woocommerce.android.cardreader.payments

class StatementDescriptor(rawValue: String?) {

    val value: String? = rawValue?.take(MAX_LENGTH)?.split("<", ">", "'", "\"")?.joinToString(REPLACEMENT_CHAR)

    companion object {
        const val MAX_LENGTH = 22
        const val REPLACEMENT_CHAR = "-"
    }
}
