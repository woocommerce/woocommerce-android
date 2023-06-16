package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatIllegalStateException
import org.junit.Before
import org.junit.Test

@ExperimentalCoroutinesApi
class CheckDigitRemoverFactoryTest : BaseUnitTest() {
    private lateinit var checkDigitRemoverFactory: CheckDigitRemoverFactory

    @Before
    fun setup() {
        checkDigitRemoverFactory = CheckDigitRemoverFactory()
    }

    @Test
    fun `given UPC-A barcode format, when factory is called, then return UPCA check digit remover` () {
        assertThat(
            checkDigitRemoverFactory.getCheckDigitRemoverFor(
                BarcodeFormat.FormatUPCA
            )
        ).isInstanceOf(UPCCheckDigitRemover::class.java)
    }

    @Test
    fun `given UPC-E barcode format, when factory is called, then return UPCA check digit remover` () {
        assertThat(
            checkDigitRemoverFactory.getCheckDigitRemoverFor(
                BarcodeFormat.FormatUPCE
            )
        ).isInstanceOf(UPCCheckDigitRemover::class.java)
    }

    @Test
    fun `given EAN-13 barcode format, when factory is called, then return UPCA check digit remover` () {
        assertThat(
            checkDigitRemoverFactory.getCheckDigitRemoverFor(
                BarcodeFormat.FormatEAN13
            )
        ).isInstanceOf(EAN13CheckDigitRemover::class.java)
    }

    @Test
    fun `given EAN-8 barcode format, when factory is called, then return UPCA check digit remover` () {
        assertThat(
            checkDigitRemoverFactory.getCheckDigitRemoverFor(
                BarcodeFormat.FormatEAN8
            )
        ).isInstanceOf(EAN8CheckDigitRemover::class.java)
    }

    @Test
    fun `given non-supported barcode format, when factory is called, then throw illegal state exception` () {
        assertThatIllegalStateException().isThrownBy {
            checkDigitRemoverFactory.getCheckDigitRemoverFor(
                BarcodeFormat.FormatQRCode
            )
        }
    }
}
