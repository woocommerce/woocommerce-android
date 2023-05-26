package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import com.google.mlkit.vision.barcode.common.Barcode as GoogleBarcode

@ExperimentalCoroutinesApi
class GoogleBarcodeFormatMapperTest : BaseUnitTest() {
    private lateinit var barcodeFormatMapper: GoogleBarcodeFormatMapper

    @Before
    fun setup() {
        barcodeFormatMapper = GoogleBarcodeFormatMapper()
    }

    @Test
    fun `when barcode format is AZTEC, then return AZTEC type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_AZTEC)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatAztec)
    }

    @Test
    fun `when barcode format is CODABAR, then return CODABAR type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_CODABAR)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatCodaBar)
    }

    @Test
    fun `when barcode format is CODE_128, then return CODE_128 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_CODE_128)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode128)
    }

    @Test
    fun `when barcode format is CODE_39, then return CODE_39 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_CODE_39)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode39)
    }

    @Test
    fun `when barcode format is CODE_93, then return CODE_93 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_CODE_93)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatCode93)
    }

    @Test
    fun `when barcode format is CODE_DATA_MATRIX, then return CODE_DATA_MATRIX type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_DATA_MATRIX)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatDataMatrix)
    }

    @Test
    fun `when barcode format is CODE_EAN_13, then return CODE_EAN_13 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_EAN_13)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN13)
    }

    @Test
    fun `when barcode format is CODE_EAN_8, then return CODE_EAN_8 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_EAN_8)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatEAN8)
    }

    @Test
    fun `when barcode format is ITF, then return ITF type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_ITF)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatITF)
    }

    @Test
    fun `when barcode format is PDF417, then return PDF417 type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_PDF417)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatPDF417)
    }

    @Test
    fun `when barcode format is QR_CODE, then return QR_CODE type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_QR_CODE)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatQRCode)
    }

    @Test
    fun `when barcode format is UPC_A, then return UPC_A type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_UPC_A)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCA)
    }

    @Test
    fun `when barcode format is UPC_E, then return UPC_E type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_UPC_E)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatUPCE)
    }

    @Test
    fun `when barcode format is UNKNOWN, then return UNKNOWN type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(GoogleBarcode.FORMAT_UNKNOWN)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatUnknown)
    }

    @Test
    fun `when barcode format is Invalid, then return UNKNOWN type`() {
        assertThat(
            barcodeFormatMapper.mapBarcodeFormat(-1)
        ).isEqualTo(GoogleBarcodeFormatMapper.BarcodeFormat.FormatUnknown)
    }
}
