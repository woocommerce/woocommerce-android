package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelCustomsFormViewModel.PrintCustomsForm
import com.woocommerce.android.util.FileDownloader
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import java.io.File

class PrintShippingLabelCustomsFormViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: PrintShippingLabelCustomsFormViewModel
    private val fileDownloader: FileDownloader = mock()
    private val fileUtils: FileUtils = mock()
    private val storageDirectory: File = File(".")
    private var urls = mutableListOf("test_url")

    @Before
    fun setup() {
        whenever(fileUtils.createTempTimeStampedFile(any(), any(), any())).thenReturn(File("./test"))
        initViewModel()
    }

    private fun initViewModel() {
        viewModel = PrintShippingLabelCustomsFormViewModel(
            savedStateHandle = PrintShippingLabelCustomsFormFragmentArgs(urls.toTypedArray(), true)
                .initSavedStateHandle(),
            fileDownloader = fileDownloader,
            fileUtils = fileUtils
        )
        viewModel.storageDirectory = storageDirectory
    }

    @Test
    fun `when print button is clicked, then start download`() = testBlocking {
        viewModel.onPrintButtonClicked()

        verify(fileDownloader).downloadFile(any(), any())
    }

    @Test
    fun `when download is complete, then print file`() = testBlocking {
        whenever(fileDownloader.downloadFile(any(), any())).thenReturn(true)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value).isInstanceOf(PrintCustomsForm::class.java)
    }

    @Test
    fun `when dialog is dismissed, then cancel download`() = testBlocking {
        whenever(fileDownloader.downloadFile(any(), any())).doSuspendableAnswer {
            try {
                delay(1000L)
                true
            } catch (e: CancellationException) {
                false
            }
        }

        viewModel.onPrintButtonClicked()
        viewModel.onDownloadCanceled()

        val viewState = viewModel.viewStateData.liveData.value!!
        assertThat(viewState.isProgressDialogShown).isFalse
        assertThat(viewModel.event.value).isNull()
    }

    @Test
    fun `when file creation fails, then show an error`() = testBlocking {
        whenever(fileUtils.createTempTimeStampedFile(any(), any(), any())).thenReturn(null)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
    }

    @Test
    fun `when the file download fails, then show an error`() = testBlocking {
        whenever(fileDownloader.downloadFile(any(), any())).thenReturn(false)

        viewModel.onPrintButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ShowSnackbar(R.string.shipping_label_print_customs_form_download_failed))
    }

    @Test
    fun `when save for later is clicked, then navigate back`() = testBlocking {
        viewModel.onSaveForLaterClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(ExitWithResult(Unit))
    }

    @Test
    fun `when back button is clicked, then navigate back`() = testBlocking {
        viewModel.onBackButtonClicked()

        assertThat(viewModel.event.value)
            .isEqualTo(Exit)
    }

    @Test
    fun `when there are multiple invoices, then show a list`() = testBlocking {
        urls.add("second_url")

        initViewModel()

        assertThat(viewModel.viewStateData.liveData.value?.showInvoicesAsAList).isTrue
        assertThat(viewModel.viewStateData.liveData.value?.commercialInvoices?.size).isEqualTo(2)
    }

    @Test
    fun `given there are multiple invoices, when print is clicked, then start download`() = testBlocking {
        val url = "second_url"
        urls.add(url)
        whenever(fileDownloader.downloadFile(any(), any())).thenReturn(true)

        initViewModel()
        viewModel.onInvoicePrintButtonClicked(url)

        verify(fileDownloader).downloadFile(any(), any())
        assertThat(viewModel.event.value).isInstanceOf(PrintCustomsForm::class.java)
    }
}
