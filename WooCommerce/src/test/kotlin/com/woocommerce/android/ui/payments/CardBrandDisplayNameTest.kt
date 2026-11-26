package com.woocommerce.android.ui.payments

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CardBrandDisplayNameTest {
    @Test
    fun `given a Stripe brand, when getting its display name, then it is the name the card network uses`() {
        val names = listOf(
            "visa", "amex", "mastercard", "discover", "jcb", "diners", "interac", "unionpay", "eftpos_au",
            "cartes_bancaires", "girocard",
        ).map { it.toCardBrandDisplayName() }

        assertThat(names).containsExactly(
            "Visa", "American Express", "Mastercard", "Discover", "JCB", "Diners Club", "Interac", "UnionPay",
            "eftpos", "Cartes Bancaires", "girocard",
        )
    }

    @Test
    fun `given an unknown or missing brand, when getting its display name, then it is empty`() {
        assertThat("unknown".toCardBrandDisplayName()).isEmpty()
        assertThat((null as String?).toCardBrandDisplayName()).isEmpty()
    }

    @Test
    fun `given a brand the app does not know, when getting its display name, then it is capitalised`() {
        assertThat("link".toCardBrandDisplayName()).isEqualTo("Link")
    }

    @Test
    fun `given a brand and last 4, when describing the card, then the brand comes before the masked number`() {
        assertThat(maskedCardDescription("amex", "1234")).isEqualTo("American Express **** 1234")
    }

    @Test
    fun `given an unknown brand, when describing the card, then only the masked number is shown`() {
        assertThat(maskedCardDescription("unknown", "1234")).isEqualTo("**** 1234")
    }
}
