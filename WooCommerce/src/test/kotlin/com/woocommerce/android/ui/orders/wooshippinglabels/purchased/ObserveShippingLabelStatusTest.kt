import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PURCHASED
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PURCHASE_IN_PROGRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.UNKNOWN
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import com.woocommerce.android.ui.orders.wooshippinglabels.purchased.ObserveShippingLabelStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.advanceUntilIdle
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import java.math.BigDecimal
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
class ObserveShippingLabelStatusTest : BaseUnitTest() {
    private lateinit var observeShippingLabelStatus: ObserveShippingLabelStatus
    private val selectedSite: SelectedSite = mock()
    private val labelRepository: WooShippingLabelRepository = mock()

    private val mockSite = SiteModel().apply { id = 123 }
    private val mockOrderId = 456L
    private val mockLabelId = 789L

    private val purchaseLabelModel = ShippingLabelModel(
        labelId = 0,
        tracking = "",
        refundableAmount = BigDecimal.ZERO,
        status = UNKNOWN,
        created = null,
        carrierId = "",
        serviceName = "",
        commercialInvoiceUrl = "",
        isCommercialInvoiceSubmittedElectronically = false,
        packageName = "",
        isLetter = false,
        productNames = emptyList(),
        productIds = emptyList(),
        shipmentId = "0",
        receiptItemId = 0L,
        createdDate = null,
        mainReceiptId = 0L,
        rate = BigDecimal.ZERO,
        currency = "",
        expiryDate = 0L
    )

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(mockSite)
        observeShippingLabelStatus = ObserveShippingLabelStatus(selectedSite, labelRepository)
    }

    @Test
    fun `When status response is unknown, then observation stops`() = testBlocking {
        whenever(labelRepository.fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId))
            .thenReturn(WooResult(purchaseLabelModel))

        val result = observeShippingLabelStatus(mockOrderId, mockLabelId).toList()
        advanceUntilIdle()

        verify(labelRepository).fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId)
        assertEquals(listOf(PURCHASE_IN_PROGRESS, UNKNOWN), result.map { it.status })
    }

    @Test
    fun `When status response is PurchaseInProgress, continue trying until it changes`() = testBlocking {
        var statusCallCount = 0
        whenever(labelRepository.fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId))
            .then {
                if (statusCallCount++ == 0) {
                    WooResult(purchaseLabelModel.copy(status = PURCHASE_IN_PROGRESS))
                } else {
                    WooResult(purchaseLabelModel.copy(status = PURCHASED))
                }
            }

        val result = observeShippingLabelStatus(mockOrderId, mockLabelId).toList()
        advanceUntilIdle()

        verify(labelRepository, times(2)).fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId)
        assertEquals(listOf(PURCHASE_IN_PROGRESS, PURCHASE_IN_PROGRESS, PURCHASED), result.map { it.status })
    }

    @Test
    fun `When status response is error, then fallback to Unknown status`() = testBlocking {
        val error = WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN)
        whenever(labelRepository.fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId))
            .thenReturn(WooResult(error))

        val result = observeShippingLabelStatus(mockOrderId, mockLabelId).toList()
        advanceUntilIdle()

        verify(labelRepository, times(1)).fetchShippingLabelStatus(mockSite, mockOrderId, mockLabelId)
        assertEquals(listOf(PURCHASE_IN_PROGRESS, UNKNOWN), result.map { it.status })
    }
}
