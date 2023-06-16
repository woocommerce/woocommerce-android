package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Test

@ExperimentalCoroutinesApi
class EAN13CheckDigitRemoverTest : BaseUnitTest() {
    private val checkDigitRemover = EAN13CheckDigitRemover()

    @Test
    fun `given EAN-13 format barcode SKU with check digit, then return SKU with check digit removed`() {
        val sku = "1234567890123"
        Assertions.assertThat(checkDigitRemover.getSKUWithoutCheckDigit(sku)).isEqualTo(
            "123456789012"
        )
    }

    @Test
    fun `given alpha numeric EAN-13 format barcode SKU with check digit, then return SKU with check digit removed`() {
        val sku = "1a345Z7890123"
        Assertions.assertThat(checkDigitRemover.getSKUWithoutCheckDigit(sku)).isEqualTo(
            "1a345Z789012"
        )
    }
}
