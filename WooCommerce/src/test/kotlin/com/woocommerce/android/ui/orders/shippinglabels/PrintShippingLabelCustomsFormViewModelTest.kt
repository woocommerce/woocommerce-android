package com.woocommerce.android.ui.orders.shippinglabels

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelCustomsFormViewModel.PrintCustomsForm
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class PrintShippingLabelCustomsFormViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrintShippingLabelCustomsFormViewModel
    private val fileDownloader: FileDownloader = mock()
    private val fileUtils: FileUtils = mock()
    private val storageDirectory: File = File(".")
    private val url = "test_url"

    @Before
    fun setup() {
        whenever(fileUtils.createTempTimeStampedFile(any(), any(), any())).thenReturn(File("./test"))
        viewModel = PrintShippingLabelCustomsFormViewModel(
            savedStateHandle = PrintShippingLabelCustomsFormFragmentArgs(url).initSavedStateHandle(),
            fileDownloader = fileDownloader,
            fileUtils = fileUtils
        )
        viewModel.storageDirectory = storageDirectory
    }

    @Test
    fun `start download when print button is clicked`() = testBlocking {
        viewModel.onPrintButtonClicked()

        verify(fileDownloader).downloadFile(any(), any())
    }

    @Test
    fun `open pdf reader when download is complete`() = testBlocking {
        whenever(fileDownloader.downloadFile(any(), any())).thenReturn(true)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value).isInstanceOf(PrintCustomsForm::class.java)
    }
}
