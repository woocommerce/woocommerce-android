package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.ui.woopos.common.barcode.WooPosBarcodeFormat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test

class WooPosSearchByIdentifierCheckDigitRemoverTest {

    private lateinit var sut: WooPosSearchByIdentifierCheckDigitRemover

    @Before
    fun setup() {
        sut = WooPosSearchByIdentifierCheckDigitRemover()
    }

    @Test
    fun `given EAN13 format with check digit, when removing check digit, should return code without check digit`() {
        // GIVEN
        val codeWithCheckDigit = "1234567890123"
        val format = WooPosBarcodeFormat.FormatEAN13

        // WHEN
        val result = sut(codeWithCheckDigit, format)

        // THEN
        assertThat(result).isEqualTo("123456789012")
    }

    @Test
    fun `given UPCA format with check digit, when removing check digit, should return code without check digit`() {
        // GIVEN
        val codeWithCheckDigit = "123456789012"
        val format = WooPosBarcodeFormat.FormatUPCA

        // WHEN
        val result = sut(codeWithCheckDigit, format)

        // THEN
        assertThat(result).isEqualTo("12345678901")
    }

    @Test
    fun `given unknown format, when removing check digit, should return original code`() {
        // GIVEN
        val originalCode = "1234567890"
        val format = WooPosBarcodeFormat.FormatUnknown

        // WHEN
        val result = sut(originalCode, format)

        // THEN
        assertThat(result).isEqualTo(originalCode)
    }

    @Test
    fun `given QR code format, when removing check digit, should return original code`() {
        // GIVEN
        val originalCode = "some-qr-data"
        val format = WooPosBarcodeFormat.FormatQRCode

        // WHEN
        val result = sut(originalCode, format)

        // THEN
        assertThat(result).isEqualTo(originalCode)
    }
}