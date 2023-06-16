package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
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
}
