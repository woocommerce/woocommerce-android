package com.woocommerce.android.ui.orders.creation

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CodeScannerTest : BaseUnitTest() {

    private val scanner: GmsBarcodeScanner = mock()
    private lateinit var codeScanner: CodeScanner

    @Before
    fun setup() {
        codeScanner = GoogleCodeScanner(scanner)
    }

    @Test
    fun `when scanning code succeeds, then success is emitted`() {
        testBlocking {
            val mockBarcode = mock<Task<Barcode>>()
            whenever(scanner.startScan()).thenAnswer {
                mockBarcode
            }
            whenever(mockBarcode.addOnSuccessListener(any())).thenAnswer {
                @Suppress("UNCHECKED_CAST")
                (it.arguments[0] as OnSuccessListener<Barcode>).onSuccess(mock())
                mock<Task<Barcode>>()
            }

            val result = codeScanner.startScan().first()

            Assertions.assertThat(result).isExactlyInstanceOf(CodeScannerStatus.Success::class.java)
        }
    }
}
