package com.woocommerce.android.ui.mystore

import androidx.lifecycle.Lifecycle
import com.automattic.android.tracks.crashlogging.performance.PerformanceTransactionRepository
import com.automattic.android.tracks.crashlogging.performance.TransactionId
import com.automattic.android.tracks.crashlogging.performance.TransactionOperation
import com.automattic.android.tracks.crashlogging.performance.TransactionStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions

@OptIn(ExperimentalCoroutinesApi::class)
class MyStoreTransactionLauncherTest : BaseUnitTest() {
    val transactionId = TransactionId("testTransactionId")
    val performanceTransactionRepository: PerformanceTransactionRepository = mock {
        on { startTransaction(any(), any()) } doReturn transactionId
    }

    private val sut = MyStoreTransactionLauncher(
        performanceTransactionRepository,
        coroutinesTestRule.testDispatchers
    )

    @Test
    fun `should start transaction on create`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)

        verify(performanceTransactionRepository).startTransaction("MyStore", TransactionOperation.UI_LOAD)
    }

    @Test
    fun `should abort transaction if on destroy`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)
        sut.onStateChanged(mock(), Lifecycle.Event.ON_STOP)

        verify(performanceTransactionRepository).finishTransaction(transactionId, TransactionStatus.ABORTED)
    }

    @Test
    fun `should successfully finish transaction if list fetch condition is met`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)

        sut.onStoreStatisticsFetched()
        sut.onTopPerformersFetched()

        verify(performanceTransactionRepository).finishTransaction(transactionId, TransactionStatus.SUCCESSFUL)
    }

    @Test
    fun `should not finish transaction if not all conditions are met`() {
        sut.onStoreStatisticsFetched()

        verifyNoInteractions(performanceTransactionRepository)
    }

    @Test
    fun `should not allow to successfully finish transaction after abort`() {
        sut.onStateChanged(mock(), Lifecycle.Event.ON_CREATE)
        sut.onStateChanged(mock(), Lifecycle.Event.ON_STOP)

        sut.onStoreStatisticsFetched()
        sut.onTopPerformersFetched()

        verify(performanceTransactionRepository, never()).finishTransaction(transactionId, TransactionStatus.SUCCESSFUL)
    }
}
