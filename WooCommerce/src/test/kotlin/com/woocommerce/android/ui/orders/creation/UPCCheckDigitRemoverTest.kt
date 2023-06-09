package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

@ExperimentalCoroutinesApi
class UPCCheckDigitRemoverTest : BaseUnitTest() {
    private val checkDigitRemover = UPCCheckDigitRemover()

    @Test
    fun `given UPC format barcode SKU with check digit, then return SKU with check digit removed`() {
        val sku = "12345678901"
        assertThat(checkDigitRemover.getSKUWithoutCheckDigit(sku)).isEqualTo(
            "1234567890"
        )
    }

    @Test
    fun `given alpha numeric UPC format barcode SKU with check digit, then return SKU with check digit removed`() {
        val sku = "1a345Z78901"
        assertThat(checkDigitRemover.getSKUWithoutCheckDigit(sku)).isEqualTo(
            "1a345Z7890"
        )
    }
}
