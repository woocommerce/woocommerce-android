package com.woocommerce.android.ui.orders.creation

import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner
import com.google.mlkit.vision.common.InputImage
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.barcodescanner.InputImageProvider
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class CodeScannerTest : BaseUnitTest() {

    private val scanner: BarcodeScanner = mock()
    private val errorMapper: GoogleCodeScannerErrorMapper = mock()
    private val barcodeFormatMapper: GoogleBarcodeFormatMapper = mock()
    private val imageProxy: ImageProxy = mock()
    private val inputImageProvider: InputImageProvider = mock()

    private lateinit var codeScanner: CodeScanner

    @Before
    fun setup() {
        codeScanner = GoogleMLKitCodeScanner(scanner, errorMapper, barcodeFormatMapper, inputImageProvider)
    }

    @Test
    fun `when scanning code succeeds, then success is emitted`() {
        testBlocking {
            val barcodeRawValue = "12345"
            val mockBarcodeList = mock<Task<List<Barcode>>>()
            val mockBarcode = mock<Barcode>() {
                on {
                    rawValue
                }.thenReturn(barcodeRawValue)
            }
            val inputImage = mock<InputImage>()
            whenever(inputImageProvider.provideImage(imageProxy)).thenReturn(inputImage)
            whenever(scanner.process(inputImage)).thenAnswer {
                mockBarcodeList
            }
            whenever(barcodeFormatMapper.mapBarcodeFormat(any())).thenReturn(BarcodeFormat.FormatUPCA)
            whenever(mockBarcodeList.addOnFailureListener(any())).thenReturn(mockBarcodeList)
            whenever(mockBarcodeList.addOnCompleteListener(any())).thenReturn(mockBarcodeList)
            doNothing().`when`(imageProxy).close()
            whenever(mockBarcodeList.addOnSuccessListener(any())).thenAnswer {
                @Suppress("UNCHECKED_CAST")
                (it.arguments[0] as OnSuccessListener<List<Barcode>>).onSuccess(
                    mock {
                        on {
                            firstOrNull()
                        }.thenReturn(mockBarcode)
                    }
                )
                mock<Task<List<Barcode>>>()
            }

            val result = codeScanner.startScan(imageProxy).first()

            assertThat(result).isExactlyInstanceOf(CodeScannerStatus.Success::class.java)
        }
    }

    @Test
    fun `when scanning code succeeds, then proper barcode value is emitted`() {
        testBlocking {
            val barcodeRawValue = "12345"
            val mockBarcodeList = mock<Task<List<Barcode>>>()
            val mockBarcode = mock<Barcode>() {
                on {
                    rawValue
                }.thenReturn(barcodeRawValue)
            }
            val inputImage = mock<InputImage>()
            whenever(inputImageProvider.provideImage(imageProxy)).thenReturn(inputImage)
            whenever(scanner.process(inputImage)).thenAnswer {
                mockBarcodeList
            }
            whenever(barcodeFormatMapper.mapBarcodeFormat(any())).thenReturn(BarcodeFormat.FormatUPCA)
            whenever(mockBarcodeList.addOnFailureListener(any())).thenReturn(mockBarcodeList)
            whenever(mockBarcodeList.addOnCompleteListener(any())).thenReturn(mockBarcodeList)
            whenever(mockBarcodeList.addOnSuccessListener(any())).thenAnswer {
                @Suppress("UNCHECKED_CAST")
                (it.arguments[0] as OnSuccessListener<List<Barcode>>).onSuccess(
                    mock {
                        on {
                            firstOrNull()
                        }.thenReturn(mockBarcode)
                    }
                )
                mock<Task<List<Barcode>>>()
            }

            val result = codeScanner.startScan(imageProxy).first()

            assertThat((result as CodeScannerStatus.Success).code).isEqualTo(barcodeRawValue)
        }
    }
//
//    @Test
//    fun `when scanning code succeeds, then flow is terminated`() {
//        testBlocking {
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(mockBarcode.addOnSuccessListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnSuccessListener<Barcode>).onSuccess(mock())
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().toList()
//
//            assertThat(result.size).isEqualTo(1)
//        }
//    }
//
//    @Test
//    fun `when scanning code fails, then failure is emitted`() {
//        testBlocking {
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(errorMapper.mapGoogleMLKitScanningErrors(any())).thenReturn(CodeScanningErrorType.NotFound)
//            whenever(mockBarcode.addOnSuccessListener(any())).thenReturn(mockBarcode)
//            whenever(mockBarcode.addOnFailureListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnFailureListener).onFailure(mock())
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().first()
//
//            assertThat(result).isExactlyInstanceOf(CodeScannerStatus.Failure::class.java)
//        }
//    }
//
//    @Test
//    fun `when scanning code fails, then proper failure message is emitted`() {
//        testBlocking {
//            val errorMessage = "Invalid Barcode"
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(errorMapper.mapGoogleMLKitScanningErrors(any())).thenReturn(CodeScanningErrorType.NotFound)
//            whenever(mockBarcode.addOnSuccessListener(any())).thenReturn(mockBarcode)
//            whenever(mockBarcode.addOnFailureListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnFailureListener).onFailure(
//                    mock {
//                        on {
//                            message
//                        }.thenReturn(errorMessage)
//                    }
//                )
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().first()
//
//            assertThat((result as CodeScannerStatus.Failure).error).isEqualTo(errorMessage)
//        }
//    }
//
//    @Test
//    fun `when scanning code succeeds but does not contain raw value, then failure is emitted`() {
//        testBlocking {
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(mockBarcode.addOnSuccessListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnSuccessListener<Barcode>).onSuccess(
//                    mock {
//                        on {
//                            rawValue
//                        }.thenReturn(null)
//                    }
//                )
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().first()
//
//            assertThat(result).isExactlyInstanceOf(CodeScannerStatus.Failure::class.java)
//        }
//    }
//
//    @Test
//    fun `when scanning code succeeds but does not contain raw value, then proper failure message is emitted`() {
//        testBlocking {
//            val errorMessage = "Failed to find a valid raw value!"
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(mockBarcode.addOnSuccessListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnSuccessListener<Barcode>).onSuccess(
//                    mock {
//                        on {
//                            rawValue
//                        }.thenReturn(null)
//                    }
//                )
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().first()
//
//            assertThat((result as CodeScannerStatus.Failure).error).isEqualTo(errorMessage)
//        }
//    }
//
//    @Test
//    fun `when scanning code fails, then flow is terminated`() {
//        testBlocking {
//            val mockBarcode = mock<Task<Barcode>>()
//            whenever(scanner.startScan()).thenAnswer {
//                mockBarcode
//            }
//            whenever(errorMapper.mapGoogleMLKitScanningErrors(any())).thenReturn(CodeScanningErrorType.NotFound)
//            whenever(mockBarcode.addOnSuccessListener(any())).thenReturn(mockBarcode)
//            whenever(mockBarcode.addOnFailureListener(any())).thenAnswer {
//                @Suppress("UNCHECKED_CAST")
//                (it.arguments[0] as OnFailureListener).onFailure(mock())
//                mock<Task<Barcode>>()
//            }
//
//            val result = codeScanner.startScan().toList()
//
//            assertThat(result.size).isEqualTo(1)
//        }
//    }
}
