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
        val sku = "12345678901"
        Assertions.assertThat(checkDigitRemover.getSKUWithoutCheckDigit(sku)).isEqualTo(
            "1234567890"
        )
    }
}
