package com.woocommerce.android.ui.orders.shippinglabels

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelCustomsFormViewModel.PrintCustomsForm
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
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
            savedStateHandle = PrintShippingLabelCustomsFormFragmentArgs(url, true).initSavedStateHandle(),
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

    @Test
    fun `show error when the file creation fails`() = testBlocking {
        whenever(fileUtils.createTempTimeStampedFile(any(), any(), any())).thenReturn(null)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
    }

    @Test
    fun `show error when the file download fails`() = testBlocking {
        whenever(fileDownloader.downloadFile(any(), any())).thenReturn(false)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
    }

    @Test
    fun `navigates back when save for later is clicked`() = testBlocking {
        viewModel.onSaveForLaterClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ExitWithResult(Unit))
    }

    @Test
    fun `navigates back when back button is clicked`() = testBlocking {
        viewModel.onBackButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(Exit)
    }
}
