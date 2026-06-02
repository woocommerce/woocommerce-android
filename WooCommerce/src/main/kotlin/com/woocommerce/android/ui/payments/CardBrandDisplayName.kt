package com.woocommerce.android.ui.payments

import java.util.Locale

fun String?.toCardBrandDisplayName(): String =
    when (this?.lowercase(Locale.ROOT)) {
        "cartes_bancaires" -> "Cartes Bancaires"
        "eftpos", "eftpos_au" -> "eftpos"
        else -> orEmpty().replaceFirstChar { it.uppercase() }
    }
