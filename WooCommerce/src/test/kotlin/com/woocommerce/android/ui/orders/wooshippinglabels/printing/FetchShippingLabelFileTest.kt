package com.woocommerce.android.ui.orders.wooshippinglabels.printing

import android.content.Context
import android.os.Environment
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.FetchShippingLabelFile
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.printing.ShippingLabelPrintingResponse
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.io.File

@ExperimentalCoroutinesApi
class FetchShippingLabelFileTest : BaseUnitTest() {

    private lateinit var fetchShippingLabelFile: FetchShippingLabelFile
    private val appContext: Context = mock()
    private val selectedSite: SelectedSite = mock()
    private val fileUtils: FileUtils = mock()
    private val base64Decoder: Base64Decoder = mock()
    private val labelRepository: WooShippingLabelRepository = mock()
    private val storageDir: File = mock()
    private val siteModel: SiteModel = mock()
    private val file: File = mock()

    @Before
    fun setup() {
        whenever(appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)).thenReturn(storageDir)
        whenever(selectedSite.getOrNull()).thenReturn(siteModel)
        fetchShippingLabelFile = FetchShippingLabelFile(
            appContext = appContext,
            selectedSite = selectedSite,
            dispatchers = coroutinesTestRule.testDispatchers,
            fileUtils = fileUtils,
            base64Decoder = base64Decoder,
            labelRepository = labelRepository
        )
    }

    @Test
    fun `invoke calls labelRepository and creates file`() = testBlocking {
        val labelIds = listOf(1L, 2L, 3L)
        val paperSize = "A4"
        val labelData = "base64EncodedContent"
        val response = mock<ShippingLabelPrintingResponse> {
            on { b64Content } doReturn labelData
        }

        whenever(
            labelRepository.fetchShippingLabelPrinting(siteModel, labelIds, paperSize)
        ).thenReturn(WooResult(response))

        whenever(
            fileUtils.createTempTimeStampedFile(
                storageDir,
                FetchShippingLabelFile.PDF_PREFIX,
                FetchShippingLabelFile.PDF_EXTENSION
            )
        ).thenReturn(file)
        whenever(base64Decoder.decode(labelData, 0)).thenReturn(ByteArray(0))
        whenever(
            fileUtils.writeContentToFile(eq(file), any())
        ).thenReturn(file)

        val result = fetchShippingLabelFile(labelIds, paperSize)

        verify(labelRepository).fetchShippingLabelPrinting(siteModel, labelIds, paperSize)
        verify(
            fileUtils
        ).createTempTimeStampedFile(storageDir, FetchShippingLabelFile.PDF_PREFIX, FetchShippingLabelFile.PDF_EXTENSION)
        verify(base64Decoder).decode(labelData, 0)
        verify(fileUtils).writeContentToFile(file, ByteArray(0))
        assertThat(result).isEqualTo(file)
    }

    @Test
    fun `invoke returns null when site is null`() = testBlocking {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        val result = fetchShippingLabelFile(listOf(1L, 2L, 3L), "A4")

        assertThat(result).isNull()
        verify(labelRepository, never()).fetchShippingLabelPrinting(any(), any(), any())
    }

    @Test
    fun `invoke returns null when response is error`() = testBlocking {
        val labelIds = listOf(1L, 2L, 3L)
        val paperSize = "A4"
        val response = mock<WooResult<ShippingLabelPrintingResponse>> {
            on { isError } doReturn true
        }

        whenever(labelRepository.fetchShippingLabelPrinting(siteModel, labelIds, paperSize)).thenReturn(response)

        val result = fetchShippingLabelFile(labelIds, paperSize)

        assertThat(result).isNull()
        verify(labelRepository).fetchShippingLabelPrinting(siteModel, labelIds, paperSize)
        verify(fileUtils, never()).createTempTimeStampedFile(any(), any(), any())
    }

    @Test
    fun `invoke returns null when b64Content is empty`() = testBlocking {
        val labelIds = listOf(1L, 2L, 3L)
        val paperSize = "A4"
        val response = mock<ShippingLabelPrintingResponse> {
            on { b64Content } doReturn ""
        }

        whenever(
            labelRepository.fetchShippingLabelPrinting(siteModel, labelIds, paperSize)
        ).thenReturn(WooResult(response))

        val result = fetchShippingLabelFile(labelIds, paperSize)

        assertThat(result).isNull()
        verify(labelRepository).fetchShippingLabelPrinting(siteModel, labelIds, paperSize)
        verify(fileUtils, never()).createTempTimeStampedFile(any(), any(), any())
    }
}
