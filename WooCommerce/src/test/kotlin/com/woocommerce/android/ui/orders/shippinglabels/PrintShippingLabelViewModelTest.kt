package com.woocommerce.android.ui.orders.shippinglabels

import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.media.FileUtils
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.*
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.shippinglabels.PrintShippingLabelViewModel.PrintShippingLabelViewState
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize.LABEL
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize.LETTER
import com.woocommerce.android.util.Base64Decoder
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.API_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.util.*
import kotlin.collections.ArrayList
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class PrintShippingLabelViewModelTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_SHIPPING_LABEL_ID = 1L
    }

    private val repository: ShippingLabelRepository = mock()
    private val networkStatus: NetworkStatus = mock()
    private val fileUtils: FileUtils = mock()
    private val base64Decoder: Base64Decoder = mock()

    private var savedState = PrintShippingLabelFragmentArgs(shippingLabelIds = longArrayOf(REMOTE_SHIPPING_LABEL_ID))
        .initSavedStateHandle()

    private val printShippingLabelViewState = PrintShippingLabelViewState()
    private lateinit var viewModel: PrintShippingLabelViewModel

    private fun initViewModel() {
        viewModel = PrintShippingLabelViewModel(
            savedState,
            coroutinesTestRule.testDispatchers,
            repository,
            networkStatus,
            fileUtils,
            base64Decoder
        )
    }

    @Test
    fun `Displays print shipping label view correctly`() {
        initViewModel()
        var shippingLabelData: PrintShippingLabelViewState? = null
        viewModel.viewStateData.observeForever { _, new -> shippingLabelData = new }

        assertThat(shippingLabelData).isEqualTo(printShippingLabelViewState)
    }

    @Test
    fun `Displays label format options correctly`() {
        initViewModel()
        var viewLabelFormatOptions: ViewShippingLabelFormatOptions? = null
        viewModel.event.observeForever {
            if (it is ViewShippingLabelFormatOptions) viewLabelFormatOptions = it
        }

        viewModel.onViewLabelFormatOptionsClicked()
        assertNotNull(viewLabelFormatOptions)
    }

    @Test
    fun `Displays label info view correctly`() {
        initViewModel()
        var viewPrintShippingLabelInfo: ViewPrintShippingLabelInfo? = null
        viewModel.event.observeForever {
            if (it is ViewPrintShippingLabelInfo) viewPrintShippingLabelInfo = it
        }

        viewModel.onPrintShippingLabelInfoSelected()
        assertNotNull(viewPrintShippingLabelInfo)
    }

    @Test
    fun `Displays paper size options view correctly`() {
        initViewModel()
        var viewShippingLabelPaperSizes: ViewShippingLabelPaperSizes? = null
        viewModel.event.observeForever {
            if (it is ViewShippingLabelPaperSizes) viewShippingLabelPaperSizes = it
        }

        val shippingLabelPaperSizeList = ArrayList<ShippingLabelPaperSize>()
        viewModel.viewStateData.observeForever { old, new ->
            new.paperSize.takeIfNotEqualTo(old?.paperSize) {
                shippingLabelPaperSizeList.add(it)
            }
        }

        viewModel.onPaperSizeOptionsSelected()
        assertNotNull(viewShippingLabelPaperSizes)

        val paperSize = LETTER
        viewModel.onPaperSizeSelected(paperSize)

        assertThat(shippingLabelPaperSizeList).containsExactly(LABEL, LETTER)
    }

    @Test
    fun `Do not print shipping label when not connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        initViewModel()
        doReturn(false).whenever(networkStatus).isConnected()

        var snackbar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackbar = it
        }

        viewModel.onPrintShippingLabelClicked()
        assertThat(snackbar).isEqualTo(ShowSnackbar(string.offline_error))
    }

    @Test
    fun `Print shipping label when api connected`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val testString = "testString"
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(WooResult(testString)).whenever(repository).printShippingLabels(any(), any())

        initViewModel()

        val isProgressDialogShown = ArrayList<Boolean>()
        val previewShippingLabelStringList = ArrayList<String>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                isProgressDialogShown.add(it)
            }
            new.previewShippingLabel?.takeIfNotEqualTo(old?.previewShippingLabel) {
                previewShippingLabelStringList.add(it)
            }
        }

        viewModel.onPrintShippingLabelClicked()
        assertThat(isProgressDialogShown).containsExactly(true, false)
        assertThat(previewShippingLabelStringList).containsExactly(testString)
    }

    @Test
    fun `Print shipping label results in an error`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        doReturn(true).whenever(networkStatus).isConnected()
        doReturn(
            WooResult<Boolean>(WooError(API_ERROR, NETWORK_ERROR, ""))
        ).whenever(repository).printShippingLabels(any(), any())

        initViewModel()

        val isProgressDialogShown = ArrayList<Boolean>()
        val previewShippingLabelStringList = ArrayList<String>()
        viewModel.viewStateData.observeForever { old, new ->
            new.isProgressDialogShown?.takeIfNotEqualTo(old?.isProgressDialogShown) {
                isProgressDialogShown.add(it)
            }
            new.previewShippingLabel?.takeIfNotEqualTo(old?.previewShippingLabel) {
                previewShippingLabelStringList.add(it)
            }
        }

        var snackBar: ShowSnackbar? = null
        viewModel.event.observeForever {
            if (it is ShowSnackbar) snackBar = it
        }

        viewModel.onPrintShippingLabelClicked()
        assertThat(isProgressDialogShown).containsExactly(true, false)
        assertThat(previewShippingLabelStringList).isEmpty()
        assertThat(snackBar).isEqualTo(ShowSnackbar(string.shipping_label_preview_error))
    }

    @Test
    fun `consider an anonymized label as expired`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val label = OrderTestUtils.generateShippingLabel(shippingLabelId = REMOTE_SHIPPING_LABEL_ID)
            .copy(status = "ANONYMIZED")
        doReturn(label)
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        initViewModel()

        var isLabelExpired: Boolean? = null
        viewModel.viewStateData.observeForever { old, new ->
            isLabelExpired = new.isLabelExpired
        }

        assertThat(isLabelExpired).isTrue()
    }

    @Test
    fun `disable printing if label expiry date passed`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val label = OrderTestUtils.generateShippingLabel(shippingLabelId = REMOTE_SHIPPING_LABEL_ID)
            .copy(expiryDate = Date(System.currentTimeMillis() - 60_000))
        doReturn(label)
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        initViewModel()

        var isLabelExpired: Boolean? = null
        viewModel.viewStateData.observeForever { old, new ->
            isLabelExpired = new.isLabelExpired
        }

        assertThat(isLabelExpired).isTrue()
    }

    @Test
    fun `enable printing if label hasn't expired yet`() = coroutinesTestRule.testDispatcher.runBlockingTest {
        val label = OrderTestUtils.generateShippingLabel(shippingLabelId = REMOTE_SHIPPING_LABEL_ID)
            .copy(expiryDate = Date(System.currentTimeMillis() + 60_000))
        doReturn(label)
            .whenever(repository).getShippingLabelByOrderIdAndLabelId(any(), any())

        initViewModel()

        var isLabelExpired: Boolean? = null
        viewModel.viewStateData.observeForever { old, new ->
            isLabelExpired = new.isLabelExpired
        }

        assertThat(isLabelExpired).isFalse()
    }

    @Test
    fun `when printing a single label, then show the correct screen title`() {
        initViewModel()

        assertThat(viewModel.screenTitle).isEqualTo(R.string.shipping_label_print_screen_title)
    }

    @Test
    fun `when printing multiple labels, then show the correct screen title`() {
        savedState = PrintShippingLabelFragmentArgs(shippingLabelIds = longArrayOf(REMOTE_SHIPPING_LABEL_ID, 0L))
            .initSavedStateHandle()

        initViewModel()

        assertThat(viewModel.screenTitle).isEqualTo(R.string.shipping_label_print_multiple_screen_title)
    }
}
