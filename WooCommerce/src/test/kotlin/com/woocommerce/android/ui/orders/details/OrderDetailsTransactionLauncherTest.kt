package com.woocommerce.android.ui.orders.details

import androidx.lifecycle.Lifecycle
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Ignore
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@Ignore("Caused by: java.lang.ClassNotFoundException: io.sentry.ITransaction")
@OptIn(ExperimentalCoroutinesApi::class)
class OrderDetailsTransactionLauncherTest : BaseUnitTest() {
    val transactionId = TransactionId("testTransactionId")
    val performanceTransactionRepository: PerformanceTransactionRepository = mock {
        on { startTransaction(any(), any()) } doReturn transactionId
    }

    private val sut = OrderDetailsTransactionLauncher(
        performanceTransactionRepository,
        coroutinesTestRule.testDispatchers
    )

    @Test
    fun `should start transaction on create`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)

        verify(performanceTransactionRepository).startTransaction("OrderDetails", TransactionOperation.UI_LOAD)
    }

    @Test
    fun `should abort transaction if on destroy`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)
        sut.onStateChanged(mock(), Lifecycle.Event.ON_DESTROY)

        verify(performanceTransactionRepository).finishTransaction(transactionId, TransactionStatus.ABORTED)
    }

    @Test
    fun `should not finish transaction if not all conditions are met`() {
        sut.onOrderFetched()
        sut.onNotesFetched()

        verifyNoInteractions(performanceTransactionRepository)
    }

    @Test
    fun `should successfully finish transaction if all conditions are met`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)

        sut.onOrderFetched()
        sut.onShippingLabelFetchingCompleted()
        sut.onNotesFetched()
        sut.onRefundsFetched()
        sut.onShipmentTrackingFetchingCompleted()
        sut.onPackageCreationEligibleFetched()
        sut.onSubscriptionsFetched()
        sut.onGiftCardsFetched()

        verify(performanceTransactionRepository).finishTransaction(transactionId, TransactionStatus.SUCCESSFUL)
    }

    @Test
    fun `should not allow to successfully finish transaction after abort`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)
        sut.onStateChanged(mock(), Lifecycle.Event.ON_DESTROY)

        sut.onOrderFetched()
        sut.onShippingLabelFetchingCompleted()
        sut.onNotesFetched()
        sut.onRefundsFetched()
        sut.onShipmentTrackingFetchingCompleted()
        sut.onPackageCreationEligibleFetched()

        verify(performanceTransactionRepository, never()).finishTransaction(transactionId, TransactionStatus.SUCCESSFUL)
    }
}
