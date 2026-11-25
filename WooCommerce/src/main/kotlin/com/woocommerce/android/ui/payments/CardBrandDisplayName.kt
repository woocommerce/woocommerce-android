package com.woocommerce.android.ui.payments

import java.util.Locale

fun String?.toCardBrandDisplayName(): String =
    when (this?.lowercase(Locale.ROOT)) {
        null, "unknown" -> ""
        "amex" -> "American Express"
        "jcb" -> "JCB"
        "diners" -> "Diners Club"
        "unionpay" -> "UnionPay"
        "cartes_bancaires" -> "Cartes Bancaires"
        "eftpos", "eftpos_au" -> "eftpos"
        "girocard" -> "girocard"
        else -> replaceFirstChar { it.uppercase() }
    }

fun maskedCardDescription(brand: String?, last4: String?): String =
    listOf(brand.toCardBrandDisplayName(), "**** ${last4.orEmpty()}")
        .filter { it.isNotEmpty() }
        .joinToString(" ")
